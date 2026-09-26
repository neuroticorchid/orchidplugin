package com.orchidplugins.commands;

import com.orchidplugins.managers.ConfigManager;
import com.orchidplugins.managers.TpaManager;
import com.orchidplugins.tpa.Request;
import com.orchidplugins.tpa.TpaMsg;
import com.orchidplugins.tpa.TpaUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TpToggleCommand implements CommandExecutor, TabCompleter {

    private static final String GLOBAL = "global";

    private final ConfigManager config;
    private final TpaManager tpaManager;

    public TpToggleCommand(ConfigManager config, TpaManager tpaManager) {
        this.config = config;
        this.tpaManager = tpaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 2) {
            TpaMsg.send(config, sender, "<red>Usage: /tptoggle <global|player> <on|off>");
            return true;
        }
        boolean on;
        if (args[1].equalsIgnoreCase("on")) {
            on = true;
        } else if (args[1].equalsIgnoreCase("off")) {
            on = false;
        } else {
            TpaMsg.send(config, sender, "<red>Usage: /tptoggle <global|player> <on|off>");
            return true;
        }

        if (args[0].equalsIgnoreCase(GLOBAL)) {
            tpaManager.setGlobal(Request.Type.TPA, on);
            tpaManager.setGlobal(Request.Type.TPAHERE, on);
            TpaMsg.send(config, sender, "<green>TPA and TPAHERE are now <bold>" + (on ? "<green>ON" : "<red>OFF")
                    + "</bold> <green>for <yellow>everyone<green>.");
            return true;
        }

        UUID playerId = TpaUtil.resolve(args[0]);
        if (playerId == null) {
            TpaMsg.send(config, sender, "<red>Player <yellow>" + args[0] + " <red>not found.");
            return true;
        }
        TpaManager.PlayerSettings settings = tpaManager.get(playerId);
        settings.setTpa(on);
        settings.setTpaHere(on);

        String name = TpaUtil.nameOf(playerId);
        TpaMsg.send(config, sender, "<green>TPA and TPAHERE are now <bold>" + (on ? "<green>ON" : "<red>OFF")
                + "</bold> <green>for <yellow>" + name + "<green>.");
        if (sender instanceof Player p && p.getUniqueId().equals(playerId)) {
            return true;
        }
        Player target = Bukkit.getPlayer(playerId);
        if (target != null) {
            TpaMsg.send(config, target, "<yellow>" + sender.getName()
                    + " <green>changed your TPA/TPAHERE toggle to <bold>"
                    + (on ? "<green>ON" : "<red>OFF") + "</bold><green>.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            if (GLOBAL.startsWith(args[0].toLowerCase())) {
                out.add(GLOBAL);
            }
            for (Player online : sender.getServer().getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(args[0].toLowerCase())
                        && !online.getUniqueId().equals(sender instanceof Player p ? p.getUniqueId() : null)) {
                    out.add(online.getName());
                }
            }
            return out;
        }
        if (args.length == 2) {
            return List.of("on", "off");
        }
        return List.of();
    }
}