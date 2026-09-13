package com.orchidplugins.commands;

import com.orchidplugins.managers.ConfigManager;
import com.orchidplugins.managers.ModerationManager;
import com.orchidplugins.managers.WebhookManager;
import com.orchidplugins.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class UnwarnCommand implements CommandExecutor {

    private final ModerationManager moderationManager;
    private final ConfigManager config;
    private final WebhookManager webhook;

    public UnwarnCommand(ModerationManager moderationManager, ConfigManager config, WebhookManager webhook) {
        this.moderationManager = moderationManager;
        this.config = config;
        this.webhook = webhook;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            Msg.send(sender, "<red>Usage: /unwarn <player> [count] [reason]");
            Msg.send(sender, "<gray>Removes the <count> most recent warnings (default 1).");
            return true;
        }
        String name = args[0];
        Player online = Bukkit.getPlayerExact(name);
        UUID uuid;
        if (online != null) {
            uuid = online.getUniqueId();
        } else {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
            uuid = offline.getUniqueId();
        }
        int count = 1;
        if (args.length >= 2) {
            try {
                count = Math.max(1, Integer.parseInt(args[1]));
            } catch (NumberFormatException e) {
                Msg.send(sender, "<red>Invalid count '<yellow>" + args[1] + "<red>' - use a positive number.");
                return true;
            }
        }
        String reason = args.length > 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : "";
        int removed = moderationManager.removeWarnings(uuid, count);
        if (removed == 0) {
            Msg.send(sender, "<red>Player '<yellow>" + name + "<red>' has no warnings.");
            return true;
        }
        String plural = removed == 1 ? "" : "s";
        if (online != null) {
            Msg.send(online, "<green>[UNWARN] <yellow>A staff member removed <white>" + removed + " <yellow>warning" + plural + "."
                    + (reason.isEmpty() ? "" : "\n<gray>Reason: <white>" + reason));
        } else {
            moderationManager.queueOfflineMessage(uuid,
                    "<green>[UNWARN] <yellow>A staff member removed <white>" + removed + " <yellow>warning" + plural + "."
                            + (reason.isEmpty() ? "" : "\n<gray>Reason: <white>" + reason));
        }
        config.announce(sender, config.isUnwarnBroadcast(), "unwarn",
                "<yellow>" + name + " <green>had <white>" + removed + " <green>warning" + plural + " removed."
                        + (reason.isEmpty() ? "" : (" | Reason: <white>" + reason)));
        webhook.sendUnwarn(name, sender.getName(), String.valueOf(removed) + " warning" + plural,
                reason.isEmpty() ? "None" : reason, null);
        return true;
    }
}