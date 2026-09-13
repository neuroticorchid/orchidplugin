package com.orchidplugins.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.orchidplugins.util.Msg;
import com.orchidplugins.util.TimeParser;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SuspensionManager {

    private final Plugin plugin;
    private final ConfigManager config;
    private final WebhookManager webhook;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path dataFile;
    private final Map<UUID, Suspension> suspensions = new ConcurrentHashMap<>();

    public static final class Suspension {
        String primaryGroup;
        List<String> groupNodes = new ArrayList<>();
        String reason = "";
        String name = "";
        long expiresAtEpochMs = 0;
    }

    public SuspensionManager(Plugin plugin, ConfigManager config, WebhookManager webhook) {
        this.plugin = plugin;
        this.config = config;
        this.webhook = webhook;
        this.dataFile = plugin.getDataFolder().toPath().resolve("suspensions.json");
        load();
        scheduleAutoRestores();
    }

    public boolean isSuspended(UUID uuid) {
        return suspensions.containsKey(uuid);
    }

    public void suspend(Player target, String source, String reason, long expiresAtEpochMs) {
        if (plugin.getServer().getPluginManager().getPlugin("LuckPerms") == null) {
            config.announce(Bukkit.getConsoleSender(), config.isSuspendBroadcast(), "suspend",
                    "<yellow>" + target.getName() + " <red>requested suspension, but <bold>LuckPerms is not installed<red>!");
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                LuckPerms lp = LuckPermsProvider.get();
                User user = lp.getUserManager().loadUser(target.getUniqueId()).join();
                Suspension suspension = new Suspension();
                suspension.primaryGroup = user.getPrimaryGroup();
                suspension.reason = reason;
                suspension.name = target.getName();
                suspension.expiresAtEpochMs = expiresAtEpochMs;
                for (Node node : new ArrayList<>(user.getNodes())) {
                    String key = node.getKey();
                    if (key.startsWith("group.") && !key.equalsIgnoreCase("group.default")
                            && node.getValue()) {
                        suspension.groupNodes.add(key);
                        user.data().remove(node);
                    }
                }
                Group defaultGroup = lp.getGroupManager().getGroup("default");
                if (defaultGroup != null) {
                    user.data().add(Node.builder("group.default").value(true).build());
                }
                user.setPrimaryGroup("default");
                lp.getUserManager().saveUser(user);
                suspensions.put(target.getUniqueId(), suspension);
                save();
                scheduleRestore(target.getUniqueId());
                String durationText = expiresAtEpochMs > 0
                        ? " for <yellow>" + TimeParser.format(Math.max(0, (expiresAtEpochMs - System.currentTimeMillis()) / 1000L))
                        : "";
                String text = "<yellow>" + target.getName() + " <red>has been suspended from staff" + durationText + "."
                        + (reason.isEmpty() ? "" : (" | Reason: <white>" + reason));
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    config.announce(Bukkit.getConsoleSender(), config.isSuspendBroadcast(), "suspend", text);
                    Player online = plugin.getServer().getPlayer(target.getUniqueId());
                    if (online != null) {
                        Msg.send(online, "<red><bold>[SUSPEND] <yellow>You have been suspended from staff"
                                + (expiresAtEpochMs > 0
                                ? " for <white>" + TimeParser.format(Math.max(0, (expiresAtEpochMs - System.currentTimeMillis()) / 1000L))
                                : "")
                                + ". | Reason: <white>" + (reason.isEmpty() ? "None" : reason));
                    }
                    webhook.sendSuspend(target.getName(), source,
                            reason.isEmpty() ? "None" : reason,
                            expiresAtEpochMs > 0
                                    ? TimeParser.format(Math.max(0, (expiresAtEpochMs - System.currentTimeMillis()) / 1000L))
                                    : "permanent",
                            null);
                });
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to suspend " + target.getName() + ": " + e.getMessage());
            }
        });
    }

    public boolean unsuspend(Player target) {
        Suspension suspension = suspensions.get(target.getUniqueId());
        if (suspension == null) {
            return false;
        }
        restore(target.getUniqueId(), target);
        return true;
    }

    public void scheduleAutoRestores() {
        for (UUID uuid : new ArrayList<>(suspensions.keySet())) {
            scheduleRestore(uuid);
        }
    }

    private void scheduleRestore(UUID uuid) {
        Suspension suspension = suspensions.get(uuid);
        if (suspension == null || suspension.expiresAtEpochMs <= 0) {
            return;
        }
        long delayMs = suspension.expiresAtEpochMs - System.currentTimeMillis();
        long ticks = Math.max(1, Math.min(delayMs / 50L, Integer.MAX_VALUE));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> restore(uuid, plugin.getServer().getPlayer(uuid)), ticks);
    }

    private void restore(UUID uuid, Player hint) {
        if (plugin.getServer().getPluginManager().getPlugin("LuckPerms") == null) {
            if (hint != null) {
                Msg.send(hint, "<red>LuckPerms is not installed - cannot restore suspension.");
            }
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                LuckPerms lp = LuckPermsProvider.get();
                Suspension suspension = suspensions.get(uuid);
                if (suspension == null) {
                    return;
                }
                User user = lp.getUserManager().loadUser(uuid).join();
                for (String key : suspension.groupNodes) {
                    user.data().add(Node.builder(key).value(true).build());
                }
                user.data().remove(Node.builder("group.default").value(true).build());
                user.setPrimaryGroup(suspension.primaryGroup);
                lp.getUserManager().saveUser(user);
                suspensions.remove(uuid);
                save();
                String name = suspension.name;
                String text = (name == null || name.isEmpty() ? "A suspended player" : "<yellow>" + name)
                        + " <green>has been restored to staff.";
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    config.announce(Bukkit.getConsoleSender(), config.isUnsuspendBroadcast(), "suspend", text);
                    Player online = plugin.getServer().getPlayer(uuid);
                    if (online != null) {
                        Msg.send(online, "<green><bold>[SUSPEND] <yellow>You have been restored to staff.");
                    }
                    webhook.sendRestore(name == null || name.isEmpty() ? "Unknown" : name,
                            suspension.reason == null || suspension.reason.isEmpty() ? "None" : suspension.reason, null);
                });
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to restore suspension for " + uuid + ": " + e.getMessage());
            }
        });
    }

    public Map<UUID, Suspension> getSuspensions() {
        return suspensions;
    }

    private void save() {
        try {
            Files.createDirectories(dataFile.getParent());
            Path tmp = dataFile.resolveSibling(dataFile.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tmp)) {
                gson.toJson(suspensions, writer);
            }
            Files.move(tmp, dataFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save suspensions: " + e.getMessage());
        }
    }

    private void load() {
        if (!Files.exists(dataFile)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(dataFile)) {
            Type type = new TypeToken<Map<UUID, Suspension>>() {
            }.getType();
            Map<UUID, Suspension> data = gson.fromJson(reader, type);
            if (data != null) {
                for (Map.Entry<UUID, Suspension> e : data.entrySet()) {
                    if (e.getValue().reason == null) {
                        e.getValue().reason = "";
                    }
                    suspensions.put(e.getKey(), e.getValue());
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load suspensions: " + e.getMessage());
        }
    }
}