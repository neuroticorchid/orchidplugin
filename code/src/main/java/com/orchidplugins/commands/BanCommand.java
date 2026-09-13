package com.orchidplugins.commands;

import com.orchidplugins.managers.ConfigManager;
import com.orchidplugins.managers.ModerationManager;
import com.orchidplugins.managers.WebhookManager;
import com.orchidplugins.util.Msg;
import com.orchidplugins.util.TimeParser;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public final class BanCommand implements CommandExecutor {

    private final ModerationManager moderationManager;
    private final ConfigManager config;
    private final WebhookManager webhook;

    public BanCommand(ModerationManager moderationManager, ConfigManager config, WebhookManager webhook) {
        this.moderationManager = moderationManager;
        this.config = config;
        this.webhook = webhook;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            Msg.send(sender, "<red>Usage: /ban <player> [duration] [reason]");
            Msg.send(sender, "<gray>Example: /ban Steve 7d Cheating | <gray>Durations: 45s 30m 1h 7d 2w 0/permanent");
            return true;
        }
        String name = args[0];
        TimeParser.Parsed parsed = TimeParser.extractDuration(args);
        Date expires = parsed.seconds() == 0 ? null : new Date(System.currentTimeMillis() + parsed.seconds() * 1000L);
        moderationManager.banByName(name, parsed.reason(), sender.getName(), expires);
        String durationText = parsed.seconds() == 0 ? "" : (" for <yellow>" + TimeParser.format(parsed.seconds()));
        config.announce(sender, config.isBanBroadcast(), "ban",
                "<yellow>" + name + " <red>was banned" + durationText + "."
                        + (parsed.reason().isEmpty() ? "" : (" | Reason: <white>" + parsed.reason())));
        webhook.sendBan(name, sender.getName(),
                parsed.reason().isEmpty() ? "None" : parsed.reason(),
                parsed.seconds() == 0 ? "permanent" : TimeParser.format(parsed.seconds()), null);
        return true;
    }
}