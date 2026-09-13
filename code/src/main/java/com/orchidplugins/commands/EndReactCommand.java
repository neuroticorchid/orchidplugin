package com.orchidplugins.commands;

import com.orchidplugins.managers.GiveawayManager;
import com.orchidplugins.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class EndReactCommand implements CommandExecutor {

    private final GiveawayManager giveawayManager;

    public EndReactCommand(GiveawayManager giveawayManager) {
        this.giveawayManager = giveawayManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!giveawayManager.isOpen() && !giveawayManager.isRolling()) {
            Msg.send(sender, "<gray>[Giveaway] No active giveaway running! Start one with /react <phrase>.");
            return true;
        }
        if (giveawayManager.isRolling()) {
            Msg.send(sender, "<gray>[Giveaway] Already rolling a winner!");
            return true;
        }
        sender.getServer().getLogger().info("[Giveaway] Rolling giveaway animation started...");
        giveawayManager.end();
        return true;
    }
}