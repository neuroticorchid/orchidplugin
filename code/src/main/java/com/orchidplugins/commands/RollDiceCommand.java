package com.orchidplugins.commands;

import com.orchidplugins.managers.AnimationManager;
import com.orchidplugins.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class RollDiceCommand implements CommandExecutor {

    private final AnimationManager animationManager;
    private boolean rolling = false;

    public RollDiceCommand(com.orchidplugins.OrchidPlugins plugin) {
        this.animationManager = new AnimationManager(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (rolling) {
            Msg.send(sender, "<red>Please wait for the current roll to finish!");
            return true;
        }

        List<Player> candidates = new ArrayList<>(sender.getServer().getOnlinePlayers());
        if (candidates.isEmpty()) {
            Msg.send(sender, "<gray>[Dice] No online players available to select!");
            return true;
        }

        rolling = true;
        if (sender instanceof org.bukkit.command.ConsoleCommandSender) {
            sender.getServer().getLogger().info("[Dice] Rolling animation started...");
        }

        animationManager.roll(candidates, "ROLLING...", "<aqua><bold>", selected -> {
            rolling = false;
            Msg.broadcast("<gold><bold>[Dice] <yellow>The dice landed on <aqua><bold>"
                    + selected.getName() + "<yellow>!");
            Msg.titleAll("<green><bold>SELECTED!", "<yellow>" + selected.getName()
                    + " <white>was chosen!", 4, 60, 10);
            sender.getServer().getLogger().info("[Dice] ROLL COMPLETE! Selected player: "
                    + selected.getName());
        });
        return true;
    }
}