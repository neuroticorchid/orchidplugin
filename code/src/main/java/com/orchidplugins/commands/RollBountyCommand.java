package com.orchidplugins.commands;

import com.orchidplugins.managers.BountyManager;
import com.orchidplugins.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class RollBountyCommand implements CommandExecutor {

    private final BountyManager bountyManager;

    public RollBountyCommand(com.orchidplugins.OrchidPlugins plugin, BountyManager bountyManager) {
        this.bountyManager = bountyManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only in-game players can roll a bounty.");
            return true;
        }
        bountyManager.roll(player);
        return true;
    }
}