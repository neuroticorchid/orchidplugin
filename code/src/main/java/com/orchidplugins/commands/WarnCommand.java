package com.orchidplugins.commands;

import com.orchidplugins.managers.ConfigManager;
import com.orchidplugins.managers.ModerationManager;
import com.orchidplugins.managers.WebhookManager;
import com.orchidplugins.util.Msg;
import com.orchidplugins.util.TimeParser;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class WarnCommand implements CommandExecutor {

    private final ModerationManager moderationManager;
    private final ConfigManager config;
    private final WebhookManager webhook;

    public WarnCommand(ModerationManager moderationManager, ConfigManager config, WebhookManager webhook) {
        this.moderationManager = moderationManager;
        this.config = config;
        this.webhook = webhook;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            Msg.send(sender, "<red>Usage: /warn <player> [duration] [reason]");
            Msg.send(sender, "<gray>Example: /warn Steve 1h Spam | <gray>Durations: 45s 30m 1h 7d 2w 0/permanent");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            Msg.send(sender, "<red>Player '<yellow>" + args[0] + "<red>' is not online.");
            return true;
        }
        TimeParser.Parsed parsed = TimeParser.extractDuration(args);
        long expiresAtEpochMs = parsed.seconds() == 0 ? 0 : (System.currentTimeMillis() + parsed.seconds() * 1000L);
        moderationManager.addWarning(target, parsed.reason(), sender.getName(), expiresAtEpochMs);
        String durationText = parsed.seconds() == 0 ? "" : (" for <yellow>" + TimeParser.format(parsed.seconds()));
        config.announce(sender, config.isWarnBroadcast(), "warn",
                "<yellow>" + target.getName() + " <red>was warned by <yellow>" + sender.getName() + durationText + "."
                        + (parsed.reason().isEmpty() ? "" : (" | Reason: <white>" + parsed.reason())));
        webhook.sendWarn(target.getName(), sender.getName(),
                parsed.reason().isEmpty() ? "None" : parsed.reason(),
                parsed.seconds() == 0 ? "permanent" : TimeParser.format(parsed.seconds()), null);
        return true;
    }
}