package com.orchidplugins.commands;

import com.orchidplugins.managers.PollManager;
import com.orchidplugins.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class EndPollCommand implements CommandExecutor {

    private final PollManager pollManager;

    public EndPollCommand(PollManager pollManager) {
        this.pollManager = pollManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!pollManager.isOpen()) {
            Msg.send(sender, "<gray>[Poll] No active poll running! Start one with /poll.");
            return true;
        }
        sender.getServer().getLogger().info("[Poll] Poll ended early by " + sender.getName());
        pollManager.end();
        return true;
    }
}