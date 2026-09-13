package com.orchidplugins.commands;

import com.orchidplugins.managers.ConfigManager;
import com.orchidplugins.managers.ModerationManager;
import com.orchidplugins.managers.WebhookManager;
import com.orchidplugins.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class UnbanCommand implements CommandExecutor {

    private final ModerationManager moderationManager;
    private final ConfigManager config;
    private final WebhookManager webhook;

    public UnbanCommand(ModerationManager moderationManager, ConfigManager config, WebhookManager webhook) {
        this.moderationManager = moderationManager;
        this.config = config;
        this.webhook = webhook;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            Msg.send(sender, "<red>Usage: /unban <name>");
            return true;
        }
        String name = args[0];
        if (!moderationManager.isBanned(name)) {
            Msg.send(sender, "<red>Player '<yellow>" + name + "<red>' is not banned.");
            return true;
        }
        moderationManager.unban(name);
        config.announce(sender, config.isUnbanBroadcast(), "unban",
                "<yellow>" + name + " <green>has been unbanned.");
        webhook.sendUnban(name, sender.getName(), null);
        return true;
    }
}