package com.orchidplugins.commands;

import com.orchidplugins.util.Msg;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class AnnounceCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            Msg.send(sender, "<red>Usage: /sannounce <message>");
            return true;
        }
        String message = String.join(" ", args);
        Msg.titleAll("<gold>\uD83D\uDCE2 ANNOUNCEMENT", "<yellow>" + message, 4, 100, 10);
        for (Player p : sender.getServer().getOnlinePlayers()) {
            p.sendActionBar(Msg.parse("<gold>\u26a1 <yellow>" + message));
        }
        for (Player p : sender.getServer().getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
        }
        return true;
    }
}