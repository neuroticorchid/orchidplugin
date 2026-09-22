package com.orchidplugins.commands;

import com.orchidplugins.managers.ConfigManager;
import com.orchidplugins.managers.TpaManager;
import com.orchidplugins.tpa.Request;
import com.orchidplugins.tpa.TpaMsg;
import com.orchidplugins.tpa.TpaUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class TpToggleCommand implements CommandExecutor, TabCompleter {

    private static final String[] TYPES = {"all", "tpa", "tpahere"};

    private final ConfigManager config;
    private final TpaManager tpaManager;

    public TpToggleCommand(ConfigManager config, TpaManager tpaManager) {
        this.config = config;
        this.tpaManager = tpaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2 || args.length > 3) {
            TpaMsg.send(config, sender, "<red>Usage: /tptoggle <all|player> <on|off> [all|tpa|tpahere]");
            return true;
        }
        String who = args[0];
        boolean on = parseOnOff(args[1]);
        Request.Type typeOrAll = parseType(args.length > 2 ? args[2] : "all");

        UUID playerId = null;
        if (!who.equalsIgnoreCase("all")) {
            playerId = TpaUtil.resolve(who);
            if (playerId == null) {
                TpaMsg.send(config, sender, "<red>Player <yellow>" + who + " <red>not found.");
                return true;
            }
        }

        if (playerId == null) {
            if (typeOrAll == null) {
                tpaManager.setGlobal(Request.Type.TPA, on);
                tpaManager.setGlobal(Request.Type.TPAHERE, on);
                msg(sender, "TPA and TPAHERE", who, on, null);
            } else {
                tpaManager.setGlobal(typeOrAll, on);
                msg(sender, typeName(typeOrAll), who, on, null);
            }
        } else {
            TpaManager.PlayerSettings settings = tpaManager.get(playerId);
            if (typeOrAll == null || typeOrAll == Request.Type.TPA) {
                settings.setTpa(on);
            }
            if (typeOrAll == null || typeOrAll == Request.Type.TPAHERE) {
                settings.setTpaHere(on);
            }
            msg(sender, typeOrAll == null ? "TPA and TPAHERE" : typeName(typeOrAll), who, on, playerId);
        }
        return true;
    }

    private void msg(CommandSender sender, String typeLabel, String who, boolean on, UUID playerId) {
        String name = playerId == null ? who : TpaUtil.nameOf(playerId);
        TpaMsg.send(config, sender, "<green>" + typeLabel + " is now <bold>" + (on ? "<green>ON" : "<red>OFF")
                + "</bold> <green>for <yellow>" + name + "<green>.");
        if (playerId != null && !(sender instanceof Player p && p.getUniqueId().equals(playerId))) {
            Player target = org.bukkit.Bukkit.getPlayer(playerId);
            if (target != null) {
                TpaMsg.send(config, target, "<yellow>" + sender.getName()
                        + " <green>changed your " + typeLabel + " toggle to <bold>"
                        + (on ? "<green>ON" : "<red>OFF") + "</bold><green>.");
            }
        }
    }

    private static boolean parseOnOff(String s) {
        return s.equalsIgnoreCase("on");
    }

    private static String typeName(Request.Type type) {
        return type == Request.Type.TPA ? "TPA" : "TPAHERE";
    }

    private static Request.Type parseType(String s) {
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "tpa" -> Request.Type.TPA;
            case "tpahere" -> Request.Type.TPAHERE;
            default -> null;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> out = new java.util.ArrayList<>();
            if ("all".startsWith(args[0].toLowerCase())) {
                out.add("all");
            }
            for (Player online : sender.getServer().getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    out.add(online.getName());
                }
            }
            return out;
        }
        if (args.length == 2) {
            return List.of("on", "off");
        }
        if (args.length == 3) {
            return java.util.Arrays.stream(TYPES)
                    .filter(t -> t.toLowerCase().startsWith(args[2].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}