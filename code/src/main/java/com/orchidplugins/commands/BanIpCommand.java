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

import java.util.Date;

public final class BanIpCommand implements CommandExecutor {

    private final ModerationManager moderationManager;
    private final ConfigManager config;
    private final WebhookManager webhook;

    public BanIpCommand(ModerationManager moderationManager, ConfigManager config, WebhookManager webhook) {
        this.moderationManager = moderationManager;
        this.config = config;
        this.webhook = webhook;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            Msg.send(sender, "<red>Usage: /banip <player> [duration] [reason]");
            Msg.send(sender, "<gray>Example: /banip Steve 1d Evading | <gray>Durations: 45s 30m 1h 7d 2w 0/permanent");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            Msg.send(sender, "<red>Player '<yellow>" + args[0] + "<red>' is not online.");
            return true;
        }
        TimeParser.Parsed parsed = TimeParser.extractDuration(args);
        Date expires = parsed.seconds() == 0 ? null : new Date(System.currentTimeMillis() + parsed.seconds() * 1000L);
        moderationManager.banByIp(target, parsed.reason(), sender.getName(), expires);
        String durationText = parsed.seconds() == 0 ? "" : (" for <yellow>" + TimeParser.format(parsed.seconds()));
        config.announce(sender, config.isBanIpBroadcast(), "ipban",
                "<yellow>" + target.getName() + " <red>was IP banned" + durationText + "."
                        + (parsed.reason().isEmpty() ? "" : (" | Reason: <white>" + parsed.reason())));
        webhook.sendBanIp(target.getName(), sender.getName(),
                parsed.reason().isEmpty() ? "None" : parsed.reason(),
                parsed.seconds() == 0 ? "permanent" : TimeParser.format(parsed.seconds()), null);
        return true;
    }
}