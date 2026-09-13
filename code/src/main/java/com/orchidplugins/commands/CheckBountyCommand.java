package com.orchidplugins.commands;

import com.orchidplugins.managers.BountyManager;
import com.orchidplugins.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class CheckBountyCommand implements CommandExecutor {

    private final BountyManager bountyManager;

    public CheckBountyCommand(BountyManager bountyManager) {
        this.bountyManager = bountyManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        Player target;
        if (args.length > 0) {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                Msg.send(sender, "<red>Player '<yellow>" + args[0] + "<red>' is not online.");
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            Msg.send(sender, "<red>Usage: /checkbounty [player]");
            return true;
        }
        long bounty = (long) bountyManager.getBounty(target);
        if (bounty > 0) {
            Msg.send(sender, "<dark_red>[BOUNTY] <yellow>" + target.getName() + " <gray>has a <green>$"
                    + bounty + " <gray>bounty on their head!");
        } else {
            Msg.send(sender, "<gray>[BOUNTY] <yellow>" + target.getName() + " <gray>has no active bounty.");
        }
        if (sender instanceof Player && sender.getName().equals(target.getName())) {
            Msg.send(sender, "<gray>[BOUNTY] Your balance: <green>$" + (long) bountyManager.getBalance(target));
        }
        return true;
    }
}