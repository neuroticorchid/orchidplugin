package com.orchidplugins.managers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.orchidplugins.util.Msg;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UpdateManager {

    private static final Pattern VERSION_PATTERN =
            Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(?:-(\\w+))?");

    private final Plugin plugin;
    private final ConfigManager config;
    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public UpdateManager(Plugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    /* ── version helpers ─────────────────────────────────────── */

    public String pluginVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    public String channel() {
        String override = config.getRaw().getString("update.channel", "auto");
        if ("neuro".equalsIgnoreCase(override) || "stable".equalsIgnoreCase(override)) {
            return override.toLowerCase(Locale.ROOT);
        }
        Matcher m = VERSION_PATTERN.matcher(pluginVersion());
        return (m.find() && m.group(4) != null) ? m.group(4).toLowerCase(Locale.ROOT) : "neuro";
    }

    public int[] baseVersion() {
        return parseBase(pluginVersion());
    }

    private int[] parseBase(String ver) {
        Matcher m = VERSION_PATTERN.matcher(ver);
        if (m.find()) {
            return new int[]{
                    Integer.parseInt(m.group(1)),
                    Integer.parseInt(m.group(2)),
                    Integer.parseInt(m.group(3))
            };
        }
        return new int[]{0, 0, 0};
    }

    private int compareVersions(int[] a, int[] b) {
        for (int i = 0; i < 3; i++) {
            if (a[i] != b[i]) return Integer.compare(a[i], b[i]);
        }
        return 0;
    }

    /* ── GitHub API ──────────────────────────────────────────── */

    private String repoSlug() {
        return config.getRaw().getString("update.repo", "neuroticorchid/orchidplugin");
    }

    /**
     * Fetches the latest release tag matching the given channel.
     * Returns the tag name without a leading "v", or null if nothing found.
     */
    private String fetchLatestTag(String channel) throws Exception {
        String slug = repoSlug();
        HttpRequest req = HttpRequest.newBuilder(
                        URI.create("https://api.github.com/repos/" + slug + "/releases?per_page=50"))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "OrchidPlugins-Updater")
                .GET().build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 404) {
            throw new IOException("Repo not found: " + slug + " (is it public?)");
        }
        if (resp.statusCode() != 200) {
            throw new IOException("GitHub API HTTP " + resp.statusCode());
        }

        JsonArray releases = JsonParser.parseString(resp.body()).getAsJsonArray();
        int[] bestVer = null;
        String bestTag = null;

        for (JsonElement el : releases) {
            JsonObject rel = el.getAsJsonObject();
            String tag = rel.get("tag_name").getAsString().replaceFirst("^v", "");
            Matcher m = VERSION_PATTERN.matcher(tag);
            if (!m.find()) continue;

            String relChannel = m.group(4);
            if (relChannel == null || !relChannel.equalsIgnoreCase(channel)) continue;

            int[] ver = {
                    Integer.parseInt(m.group(1)),
                    Integer.parseInt(m.group(2)),
                    Integer.parseInt(m.group(3))
            };
            if (bestVer == null || compareVersions(ver, bestVer) > 0) {
                bestVer = ver;
                bestTag = tag;
            }
        }
        return bestTag;
    }

    /**
     * Returns true if `remoteBase` (e.g. [1,0,5]) is newer than the running version.
     */
    private boolean isNewer(int[] remoteBase) {
        return compareVersions(remoteBase, baseVersion()) > 0;
    }

    /* ── commands ────────────────────────────────────────────── */

    /** Called from /orchidplugins update — sends feedback to the sender. */
    public void check(CommandSender sender) {
        runAsync(() -> doCheck(sender, false));
    }

    /** Called from /orchidplugins update download — sends feedback + downloads. */
    public void download(CommandSender sender) {
        runAsync(() -> doCheck(sender, true));
    }

    /** Startup check — logs to console, optionally auto-downloads. */
    public void startupCheck() {
        if (!config.getRaw().getBoolean("update.check-on-startup", true)) return;
        runAsync(() -> doCheck(null, config.getRaw().getBoolean("update.auto-download", false)));
    }

    private void doCheck(CommandSender sender, boolean doDownload) {
        try {
            String channel = channel();
            String latestTag = fetchLatestTag(channel);
            if (latestTag == null) {
                respond(sender, "<red>No " + channel + " releases found on GitHub.");
                return;
            }
            int[] remoteVer = parseBase(latestTag);
            String remoteDisplay = latestTag;

            if (!isNewer(remoteVer)) {
                respond(sender, "<green>You are up to date! <gray>(OrchidPlugins <yellow>"
                        + pluginVersion() + "<gray>)");
                return;
            }

            respond(sender, "<yellow>Update available: <aqua>v" + remoteDisplay
                    + " <gray>(you're on <white>" + pluginVersion() + "<gray>).");

            if (doDownload) {
                performDownload(sender, remoteDisplay);
            } else {
                respond(sender, "<green>Run <yellow>/orchidplugins update download<green> to grab it.");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Update check failed: " + e.getMessage());
            respond(sender, "<red>Update check failed: <gray>" + e.getMessage());
        }
    }

    private void performDownload(CommandSender sender, String tag) {
        try {
            String assetName = "OrchidPlugins-" + tag + ".jar";
            String downloadUrl = findAssetUrl(tag, assetName);
            if (downloadUrl == null) {
                respond(sender, "<red>Jar asset '" + assetName + "' not found in release v" + tag + ".");
                return;
            }

            Path pluginsDir = plugin.getDataFolder().getParentFile().toPath();
            Path target = pluginsDir.resolve(assetName);

            if (Files.exists(target)) {
                respond(sender, "<yellow>" + assetName + " already exists in plugins/.");
                return;
            }

            respond(sender, "<gray>Downloading <white>" + assetName + "<gray>...");

            HttpRequest req = HttpRequest.newBuilder(URI.create(downloadUrl))
                    .header("User-Agent", "OrchidPlugins-Updater")
                    .header("Accept", "application/octet-stream")
                    .GET().build();
            HttpResponse<InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());

            if (resp.statusCode() != 200) {
                respond(sender, "<red>Download failed — HTTP " + resp.statusCode());
                return;
            }

            Path temp = pluginsDir.resolve(assetName + ".tmp");
            try (InputStream in = resp.body()) {
                Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);

            respond(sender, "<green>Downloaded <white>" + assetName
                    + "<green> to plugins/. <yellow>Restart the server to activate the update.");
        } catch (Exception e) {
            plugin.getLogger().warning("Update download failed: " + e.getMessage());
            respond(sender, "<red>Download failed: <gray>" + e.getMessage());
        }
    }

    private String findAssetUrl(String tag, String assetName) throws Exception {
        String slug = repoSlug();
        // Use the assets endpoint directly for the release.
        HttpRequest req = HttpRequest.newBuilder(
                        URI.create("https://api.github.com/repos/" + slug + "/releases/tags/v" + tag))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "OrchidPlugins-Updater")
                .GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) return null;

        JsonObject release = JsonParser.parseString(resp.body()).getAsJsonObject();
        JsonArray assets = release.getAsJsonArray("assets");
        if (assets == null) return null;

        for (JsonElement el : assets) {
            JsonObject asset = el.getAsJsonObject();
            if (assetName.equals(asset.get("name").getAsString())) {
                return asset.get("browser_download_url").getAsString();
            }
        }
        return null;
    }

    /* ── utils ───────────────────────────────────────────────── */

    private void runAsync(Runnable task) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
    }

    private void respond(CommandSender sender, String message) {
        if (sender == null) {
            String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                    .plainText().serialize(Msg.parse(message));
            plugin.getLogger().info(plain);
            return;
        }
        Msg.send(sender, message);
    }
}