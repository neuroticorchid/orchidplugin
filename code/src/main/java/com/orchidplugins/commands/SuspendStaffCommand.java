package com.orchidplugins.commands;

import com.orchidplugins.OrchidPlugins;
import com.orchidplugins.managers.SuspensionManager;
import com.orchidplugins.util.Msg;
import com.orchidplugins.util.TimeParser;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class SuspendStaffCommand implements CommandExecutor {

    private final OrchidPlugins plugin;

    public SuspendStaffCommand(OrchidPlugins plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (command.getName().equalsIgnoreCase("suspendstaff")) {
                Msg.send(sender, "<red>Usage: /suspendstaff <player> [duration] [reason]");
                Msg.send(sender, "<gray>Example: /suspendstaff Steve 1d Abusing /aa | <gray>Durations: 30m 1h 7d 0/permanent");
            } else {
                Msg.send(sender, "<red>Usage: /unsuspendstaff <player>");
            }
            return true;
        }
        SuspensionManager suspensionManager = plugin.getSuspensionManager();
        if (command.getName().equalsIgnoreCase("unsuspendstaff")) {
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                Msg.send(sender, "<red>Player '<yellow>" + args[0] + "<red>' is not online.");
                return true;
            }
            if (!suspensionManager.unsuspend(target)) {
                Msg.send(sender, "<red>Player '<yellow>" + target.getName() + "<red>' is not suspended.");
            }
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            Msg.send(sender, "<red>Player '<yellow>" + args[0] + "<red>' is not online.");
            return true;
        }
        if (suspensionManager.isSuspended(target.getUniqueId())) {
            Msg.send(sender, "<red>Player '<yellow>" + target.getName() + "<red>' is already suspended.");
            return true;
        }
        TimeParser.Parsed parsed = TimeParser.extractDuration(args);
        long expiresAtEpochMs = parsed.seconds() == 0 ? 0 : (System.currentTimeMillis() + parsed.seconds() * 1000L);
        suspensionManager.suspend(target, sender.getName(), parsed.reason(), expiresAtEpochMs);
        return true;
    }
}