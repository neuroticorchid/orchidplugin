package com.orchidplugins.managers;

import com.orchidplugins.util.Msg;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

public final class ConfigManager {

    private final Plugin plugin;
    private FileConfiguration config;

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    public FileConfiguration getRaw() {
        return config;
    }

    public String databaseFilename() {
        return config.getString("database.filename", "data.db");
    }

    /** Styled server name for poll branding, translated from legacy &-codes to MiniMessage. */
    public String serverName() {
        return com.orchidplugins.util.LegacyToMini.convert(
                config.getString("server-name", "&l&bCLOUDREND SMP")) + "<reset>";
    }

    public String serverNamePlain() {
        return config.getString("server-name", "&l&bCLOUDREND SMP")
                .replaceAll("(?i)&[0-9a-fk-or]|&#[0-9a-f]{6}", "").trim();
    }

    public int pollSeconds() {
        return Math.max(5, config.getInt("poll.seconds", 30));
    }

    public String restartDefaultTime() {
        return config.getString("restart.default-time", "60s");
    }

    public int deathgameCountdownSeconds() {
        return Math.max(1, config.getInt("deathgame.countdown-seconds", 5));
    }

    public int deathgameVoteSeconds() {
        return Math.max(1, config.getInt("deathgame.vote-seconds", 10));
    }

    public int deathgameGlowDistance() {
        return Math.max(4, config.getInt("deathgame.glow-distance", 24));
    }

    public int bountyMinAmount() {
        return Math.max(1, config.getInt("bounty.min-amount", 500));
    }

    public int bountyMaxAmount() {
        return Math.max(bountyMinAmount(), config.getInt("bounty.max-amount", 5000));
    }

    public boolean adminAbuseBroadcastByDefault() {
        return config.getBoolean("admin-abuse.broadcast-by-default", false);
    }

    public boolean moderationBroadcast(String action) {
        return config.getBoolean("moderation-broadcast." + action, true);
    }

    public boolean isWarnBroadcast() {
        return moderationBroadcast("warn");
    }

    public boolean isBanBroadcast() {
        return moderationBroadcast("ban");
    }

    public boolean isBanIpBroadcast() {
        return moderationBroadcast("banip");
    }

    public boolean isUnbanBroadcast() {
        return moderationBroadcast("unban");
    }

    public boolean isUnwarnBroadcast() {
        return moderationBroadcast("unwarn");
    }

    public boolean isSuspendBroadcast() {
        return moderationBroadcast("suspendstaff");
    }

    public boolean isUnsuspendBroadcast() {
        return moderationBroadcast("unsuspendstaff");
    }

    public String prefix(String key) {
        return config.getString("prefixes." + key, key);
    }

    /** Broadcasts prefix+text if the toggle is on, otherwise sends only to the executor and logs to console. */
    public void announce(CommandSender executor, boolean broadcast, String prefixKey, String text) {
        String message = prefix(prefixKey) + text;
        if (broadcast) {
            Msg.broadcast(message);
        } else {
            Msg.send(executor, message);
            plugin.getLogger().info(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(Msg.parse(message)));
        }
    }

    public Plugin getPlugin() {
        return plugin;
    }
}