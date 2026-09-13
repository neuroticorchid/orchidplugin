package com.orchidplugins.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.orchidplugins.util.Msg;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ModerationManager {

    private final Plugin plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path dataFile;

    private final Map<UUID, List<String>> warnings = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastIps = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> pendingMessages = new ConcurrentHashMap<>();

    private final Object saveLock = new Object();
    private volatile boolean dirty = false;
    private volatile boolean saveRunning = false;

    public ModerationManager(Plugin plugin) {
        this.plugin = plugin;
        this.dataFile = plugin.getDataFolder().toPath().resolve("moderation-data.json");
        load();
    }

    public void addWarning(Player target, String reason, String issuer, long expiresAtEpochMs) {
        List<String> list = warnings.computeIfAbsent(target.getUniqueId(), k -> new ArrayList<>());
        list.add(System.currentTimeMillis() + "|" + expiresAtEpochMs + "|" + issuer + "|" + reason);
        String duration = expiresAtEpochMs > 0 ? " for <yellow>" + formatLeft(expiresAtEpochMs) + "<red>" : "";
        Msg.send(target, "<red><bold>[WARN]</bold> <yellow>You have been warned by <red>" + issuer
                + "<yellow>" + duration + "."
                + (reason.isEmpty() ? "" : ("\n<gray>Reason: <white>" + reason)));
        markDirty();
    }

    public int getWarningCount(UUID uuid) {
        purgeExpiredWarnings(uuid);
        return warnings.getOrDefault(uuid, List.of()).size();
    }

    public List<String> getWarnings(UUID uuid) {
        purgeExpiredWarnings(uuid);
        return warnings.getOrDefault(uuid, List.of());
    }

    public int removeWarnings(UUID uuid, int count) {
        List<String> list = warnings.get(uuid);
        if (list == null || list.isEmpty()) {
            return 0;
        }
        purgeExpiredWarnings(uuid);
        list = warnings.get(uuid);
        if (list == null || list.isEmpty()) {
            return 0;
        }
        int removed = Math.min(count, list.size());
        for (int i = 0; i < removed; i++) {
            list.remove(list.size() - 1);
        }
        if (list.isEmpty()) {
            warnings.remove(uuid);
        }
        markDirty();
        return removed;
    }

    public void removeAllWarnings(UUID uuid) {
        warnings.remove(uuid);
        markDirty();
    }

    public void purgeExpiredWarnings(UUID uuid) {
        List<String> list = warnings.get(uuid);
        if (list == null) {
            return;
        }
        boolean changed = false;
        long now = System.currentTimeMillis();
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            String[] parts = it.next().split("\\|", -1);
            if (parts.length >= 2) {
                try {
                    long expires = Long.parseLong(parts[1]);
                    if (expires > 0 && expires <= now) {
                        it.remove();
                        changed = true;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (changed) {
            if (list.isEmpty()) {
                warnings.remove(uuid);
            }
            markDirty();
        }
    }

    public void queueOfflineMessage(UUID uuid, String message) {
        pendingMessages.computeIfAbsent(uuid, k -> new ArrayList<>()).add(message);
        markDirty();
    }

    public List<String> drainOfflineMessages(UUID uuid) {
        List<String> messages = pendingMessages.remove(uuid);
        if (messages != null && !messages.isEmpty()) {
            markDirty();
        }
        return messages == null ? List.of() : messages;
    }

    public void recordIp(Player player) {
        String ip = player.getAddress() != null && player.getAddress().getAddress() != null
                ? player.getAddress().getAddress().getHostAddress()
                : null;
        if (ip == null) {
            return;
        }
        String existing = lastIps.putIfAbsent(player.getUniqueId(), ip);
        if (existing == null) {
            markDirty();
            return;
        }
        if (!existing.equals(ip)) {
            lastIps.put(player.getUniqueId(), ip);
            markDirty();
        }
    }

    public String getLastIp(UUID uuid) {
        return lastIps.get(uuid);
    }

    public void banByName(String name, String reason, String source, Date expires) {
        String msg = reason == null || reason.isEmpty() ? "Banned by " + source : (reason + "\n\n<gray>Banned by " + source);
        Bukkit.getBanList(BanList.Type.NAME).addBan(name, msg, expires, source);
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            online.kick(Msg.legacy("&cYou have been banned"
                    + (expires != null ? " for &e" + formatBanLeft(expires) : "")
                    + "&c! &7" + (reason == null || reason.isEmpty() ? "" : reason)));
        }
    }

    public void banByIp(Player target, String reason, String source, Date expires) {
        String ip = getLastIp(target.getUniqueId());
        if (ip == null && target.isOnline()) {
            ip = target.getAddress() != null && target.getAddress().getAddress() != null
                    ? target.getAddress().getAddress().getHostAddress()
                    : null;
        }
        if (ip != null) {
            String msg = reason == null || reason.isEmpty() ? "IP Banned by " + source : (reason + "\n\n<gray>IP Banned by " + source);
            Bukkit.getBanList(BanList.Type.IP).addBan(ip, msg, expires, source);
            if (target.isOnline()) {
                target.kick(Msg.legacy("&cYou have been banned"
                        + (expires != null ? " for &e" + formatBanLeft(expires) : "")
                        + "&c! &7" + (reason == null || reason.isEmpty() ? "" : reason)));
            }
        } else {
            banByName(target.getName(), reason, source, expires);
        }
    }

    public boolean isBanned(String name) {
        return Bukkit.getBanList(BanList.Type.NAME).isBanned(name);
    }

    public void unban(String name) {
        Bukkit.getBanList(BanList.Type.NAME).pardon(name);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String ip = null;
            for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                String playerName = player.getName();
                if (playerName != null && playerName.equalsIgnoreCase(name)) {
                    ip = lastIps.get(player.getUniqueId());
                    break;
                }
            }
            final String foundIp = ip;
            if (foundIp != null) {
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> Bukkit.getBanList(BanList.Type.IP).pardon(foundIp));
            }
        });
    }

    private String formatBanLeft(Date expires) {
        long left = (expires.getTime() - System.currentTimeMillis()) / 1000L;
        return com.orchidplugins.util.TimeParser.format(Math.max(0, left));
    }

    private String formatLeft(long expiresAtEpochMs) {
        long left = (expiresAtEpochMs - System.currentTimeMillis()) / 1000L;
        return com.orchidplugins.util.TimeParser.format(Math.max(0, left));
    }

    private void markDirty() {
        synchronized (saveLock) {
            dirty = true;
            if (!saveRunning) {
                saveRunning = true;
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::performSaveAsync);
            }
        }
    }

    private void performSaveAsync() {
        boolean isDirty;
        synchronized (saveLock) {
            isDirty = dirty;
            dirty = false;
        }
        if (!isDirty) {
            synchronized (saveLock) {
                saveRunning = false;
            }
            return;
        }
        writeSnapshot(new LinkedHashMap<>(warnings), new LinkedHashMap<>(lastIps), new LinkedHashMap<>(pendingMessages));
        synchronized (saveLock) {
            if (dirty) {
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::performSaveAsync);
            } else {
                saveRunning = false;
            }
        }
    }

    public void flushNow() {
        synchronized (saveLock) {
            dirty = false;
        }
        writeSnapshot(new LinkedHashMap<>(warnings), new LinkedHashMap<>(lastIps), new LinkedHashMap<>(pendingMessages));
    }

    private void writeSnapshot(Map<UUID, List<String>> warningsSnapshot,
                               Map<UUID, String> lastIpsSnapshot,
                               Map<UUID, List<String>> pendingMessagesSnapshot) {
        try {
            Files.createDirectories(dataFile.getParent());
            Path tmp = dataFile.resolveSibling(dataFile.getFileName() + ".tmp");
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("warnings", warningsSnapshot);
            data.put("lastIps", lastIpsSnapshot);
            data.put("pendingMessages", pendingMessagesSnapshot);
            try (Writer writer = Files.newBufferedWriter(tmp)) {
                gson.toJson(data, writer);
            }
            Files.move(tmp, dataFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save moderation data: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void load() {
        if (!Files.exists(dataFile)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(dataFile)) {
            Type type = new TypeToken<Map<String, Object>>() {
            }.getType();
            Map<String, Object> data = gson.fromJson(reader, type);
            if (data == null) {
                return;
            }
            Object w = data.get("warnings");
            if (w instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    try {
                        UUID uuid = UUID.fromString((String) e.getKey());
                        List<String> list = new ArrayList<>();
                        for (Object raw : (List<?>) e.getValue()) {
                            String entry = String.valueOf(raw);
                            String[] parts = entry.split("\\|", -1);
                            if (parts.length == 3) {
                                list.add(parts[0] + "|0|" + parts[1] + "|" + parts[2]);
                            } else {
                                list.add(entry);
                            }
                        }
                        warnings.put(uuid, list);
                    } catch (Exception ignored) {
                    }
                }
            }
            Object l = data.get("lastIps");
            if (l instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    try {
                        lastIps.put(UUID.fromString((String) e.getKey()), (String) e.getValue());
                    } catch (Exception ignored) {
                    }
                }
            }
            Object p = data.get("pendingMessages");
            if (p instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    try {
                        UUID uuid = UUID.fromString((String) e.getKey());
                        List<String> list = new ArrayList<>();
                        for (Object raw : (List<?>) e.getValue()) {
                            list.add(String.valueOf(raw));
                        }
                        pendingMessages.put(uuid, list);
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load moderation data: " + e.getMessage());
        }
    }
}