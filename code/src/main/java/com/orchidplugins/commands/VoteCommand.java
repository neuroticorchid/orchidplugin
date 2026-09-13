package com.orchidplugins.commands;

import com.orchidplugins.managers.DeathGameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class VoteCommand implements CommandExecutor {

    private final DeathGameManager deathGameManager;

    public VoteCommand(DeathGameManager deathGameManager) {
        this.deathGameManager = deathGameManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can vote.");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage("Usage: /vote die|gay");
            return true;
        }
        return deathGameManager.castVote(player, args[0]);
    }
}