package com.orchidplugins.managers;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Offline, Ed25519-signed license validation.
 *
 * <p>Keys look like {@code base64url(payload) "." base64url(signature)} where the payload is
 * {@code k1:<license-id>:<expiry-epoch>} ({@code 0} expiry = never). Keys are produced by the
 * seller with {@code tools/keygen.sh} and verified against the embedded public key. The key is
 * stored in {@code plugins/OrchidPlugins/license.key}.
 */
public final class LicenseManager {

    public enum State { LICENSED, NOT_PROVIDED, INVALID, EXPIRED }

    private static final String TAG = "k1";

    private final Plugin plugin;
    private final File keyFile;
    private final PublicKey publicKey;
    private volatile State state;
    private volatile String licenseId;
    private volatile long expiresAt;

    public LicenseManager(Plugin plugin) {
        this.plugin = plugin;
        this.keyFile = new File(plugin.getDataFolder(), "license.key");
        this.publicKey = loadPublicKey();
        this.state = validate();
    }

    public boolean isLicensed() {
        return state == State.LICENSED;
    }

    public State getState() {
        return state;
    }

    public String getLicenseId() {
        return licenseId;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    /** Re-reads the key file and recomputes the license state (called on startup and periodically). */
    public void reload() {
        State before = state;
        this.state = validate();
        if (before != State.LICENSED && state == State.LICENSED) {
            plugin.getLogger().info("OrchidPlugins license key accepted.");
        } else if (before == State.LICENSED && state != State.LICENSED) {
            plugin.getLogger().warning("OrchidPlugins license is no longer valid - all commands are blocked ("
                    + describe(state) + ").");
        }
    }

    /** Validates a key string and, if valid, persists it and licenses the plugin. */
    public boolean activate(String key) {
        VerifyResult result = verify(key);
        if (!result.valid) return false;
        try {
            File dir = keyFile.getAbsoluteFile().getParentFile();
            if (dir != null) Files.createDirectories(dir.toPath());
            Files.writeString(keyFile.toPath(), key.trim() + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save license key: " + e.getMessage());
            return false;
        }
        licenseId = result.id;
        expiresAt = result.expiresAt;
        state = State.LICENSED;
        return true;
    }

    /** Message shown to anyone trying to use a feature while the plugin is unlicensed. */
    public String unlicensedMessage() {
        State s = state;
        return "<red>OrchidPlugins is not licensed - " + describe(s)
                + " A valid license key is required to use this command."
                + " <yellow>/orchidplugins license <key>";
    }

    private String describe(State s) {
        switch (s) {
            case NOT_PROVIDED: return "<gray>(no key set).";
            case INVALID:      return "<gray>(the key is invalid).";
            case EXPIRED:      return "<gray>(the key has expired).";
            default:           return "";
        }
    }

    /* ── validation ──────────────────────────────────────────── */

    private State validate() {
        String content;
        try {
            if (!keyFile.isFile()) return State.NOT_PROVIDED;
            content = Files.readString(keyFile.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not read " + keyFile + ": " + e.getMessage());
            return State.NOT_PROVIDED;
        }
        VerifyResult result = verify(content);
        if (!result.valid) {
            licenseId = null;
            expiresAt = 0;
            if ("expired".equals(result.error)) return State.EXPIRED;
            return result.missing ? State.NOT_PROVIDED : State.INVALID;
        }
        licenseId = result.id;
        expiresAt = result.expiresAt;
        return State.LICENSED;
    }

    private VerifyResult verify(String key) {
        String trimmed = key == null ? "" : key.trim();
        if (trimmed.isEmpty()) return VerifyResult.missing();

        String[] parts = trimmed.split("\\.", -1);
        if (parts.length != 2) return VerifyResult.invalid("malformed");
        if (parts[0].isEmpty() || parts[1].isEmpty()) return VerifyResult.invalid("malformed");

        byte[] payloadBytes;
        byte[] sigBytes;
        try {
            Base64.Decoder decoder = Base64.getUrlDecoder();
            payloadBytes = decoder.decode(parts[0]);
            sigBytes = decoder.decode(parts[1]);
        } catch (IllegalArgumentException e) {
            return VerifyResult.invalid("bad encoding");
        }

        try {
            Signature signature = Signature.getInstance("Ed25519");
            signature.initVerify(publicKey);
            signature.update(payloadBytes);
            if (!signature.verify(sigBytes)) return VerifyResult.invalid("signature");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Ed25519 unavailable", e);
        }

        String payload = new String(payloadBytes, StandardCharsets.UTF_8);
        String[] fields = payload.split(":", -1);
        if (fields.length < 3 || !TAG.equals(fields[0])) return VerifyResult.invalid("unsupported key version");

        long expires;
        try {
            expires = Long.parseLong(fields[2]);
        } catch (NumberFormatException e) {
            return VerifyResult.invalid("bad expiry");
        }
        String id = fields[1];
        if (expires != 0 && System.currentTimeMillis() / 1000L >= expires) {
            return VerifyResult.invalid("expired");
        }
        return VerifyResult.ok(id, expires);
    }

    private PublicKey loadPublicKey() {
        try (InputStream in = plugin.getResource("licensing/public.key")) {
            if (in == null) throw new IllegalStateException("licensing/public.key is missing from the jar");
            String b64 = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", "");
            byte[] der = Base64.getUrlDecoder().decode(b64);
            KeyFactory factory = KeyFactory.getInstance("Ed25519");
            return factory.generatePublic(new X509EncodedKeySpec(der));
        } catch (IOException | GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Failed to load the embedded license public key", e);
        }
    }

    private static final class VerifyResult {
        final boolean valid;
        final String error;
        final String id;
        final long expiresAt;
        final boolean missing;

        private VerifyResult(boolean valid, String error, String id, long expiresAt, boolean missing) {
            this.valid = valid;
            this.error = error;
            this.id = id;
            this.expiresAt = expiresAt;
            this.missing = missing;
        }

        static VerifyResult ok(String id, long expiresAt) {
            return new VerifyResult(true, null, id, expiresAt, false);
        }

        static VerifyResult invalid(String error) {
            return new VerifyResult(false, error, null, 0, false);
        }

        static VerifyResult missing() {
            return new VerifyResult(false, null, null, 0, true);
        }
    }
}