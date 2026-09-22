package com.orchidplugins.commands;

import com.orchidplugins.managers.ConfigManager;
import com.orchidplugins.managers.TpaManager;
import com.orchidplugins.tpa.TpaMsg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public class TpAutoCommand implements CommandExecutor, TabCompleter {

    private static final String[] TYPES = {"all", "tpa", "tpahere"};

    private final ConfigManager config;
    private final TpaManager tpaManager;

    public TpAutoCommand(ConfigManager config, TpaManager tpaManager) {
        this.config = config;
        this.tpaManager = tpaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length == 0) {
            showState(player);
            return true;
        }
        if (args.length > 2) {
            TpaMsg.send(config, player, "<red>Usage: /tpauto [all|tpa|tpahere] [on|off]");
            return true;
        }
        String typeArg = args[0].toLowerCase(Locale.ROOT);
        if (!typeArg.equals("all") && !typeArg.equals("tpa") && !typeArg.equals("tpahere")) {
            TpaMsg.send(config, player, "<red>Usage: /tpauto [all|tpa|tpahere] [on|off]");
            return true;
        }
        boolean on;
        if (args.length == 1) {
            on = !currentState(player, typeArg);
        } else {
            on = args[1].equalsIgnoreCase("on");
        }

        TpaManager.PlayerSettings settings = tpaManager.get(player.getUniqueId());
        boolean changedTpa = typeArg.equals("all") || typeArg.equals("tpa");
        boolean changedTpaHere = typeArg.equals("all") || typeArg.equals("tpahere");
        if (changedTpa) {
            settings.setAutoTpa(on);
        }
        if (changedTpaHere) {
            settings.setAutoTpaHere(on);
        }
        if (changedTpa && changedTpaHere) {
            TpaMsg.send(config, player, "<green>Auto-accept is now <bold>" + (on ? "<green>ON" : "<red>OFF")
                    + "</bold> <green>for <yellow>TPA and TPAHERE<green>.");
        } else {
            TpaMsg.send(config, player, "<green>Auto-accept is now <bold>" + (on ? "<green>ON" : "<red>OFF")
                    + "</bold> <green>for <yellow>" + (changedTpa ? "TPA" : "TPAHERE") + "<green>.");
        }
        return true;
    }

    private boolean currentState(Player player, String typeArg) {
        TpaManager.PlayerSettings settings = tpaManager.get(player.getUniqueId());
        if (typeArg.equals("all")) {
            return settings.isAutoTpa() && settings.isAutoTpaHere();
        }
        return typeArg.equals("tpa") ? settings.isAutoTpa() : settings.isAutoTpaHere();
    }

    private void showState(Player player) {
        TpaManager.PlayerSettings settings = tpaManager.get(player.getUniqueId());
        TpaMsg.send(config, player, "<green>Auto-accept TPA: <bold>" + (settings.isAutoTpa() ? "<green>ON" : "<red>OFF")
                + "</bold> <green>| TPAHERE: <bold>"
                + (settings.isAutoTpaHere() ? "<green>ON" : "<red>OFF") + "</bold><green>.");
        TpaMsg.send(config, player, "<gray>Usage: /tpauto [all|tpa|tpahere] [on|off]");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return java.util.Arrays.stream(TYPES)
                    .filter(t -> t.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2) {
            return List.of("on", "off");
        }
        return List.of();
    }
}