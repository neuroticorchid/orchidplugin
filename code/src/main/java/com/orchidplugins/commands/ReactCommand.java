package com.orchidplugins.commands;

import com.orchidplugins.managers.GiveawayManager;
import com.orchidplugins.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class ReactCommand implements CommandExecutor {

    private final GiveawayManager giveawayManager;

    public ReactCommand(GiveawayManager giveawayManager) {
        this.giveawayManager = giveawayManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (giveawayManager.isOpen() || giveawayManager.isRolling()) {
            Msg.send(sender, "<red>[Giveaway] A giveaway is already running! Run /endreact to roll the winner.");
            return true;
        }
        String phrase = args.length > 0 ? String.join(" ", args) : null;
        if (!giveawayManager.start(phrase)) {
            Msg.send(sender, "<red>[Giveaway] Could not start a giveaway right now.");
        }
        return true;
    }
}