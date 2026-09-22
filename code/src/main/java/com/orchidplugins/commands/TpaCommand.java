package com.orchidplugins.commands;

import com.orchidplugins.managers.ConfigManager;
import com.orchidplugins.managers.TpaManager;
import com.orchidplugins.tpa.Request;
import com.orchidplugins.tpa.RequestManager;
import com.orchidplugins.tpa.TeleportService;
import com.orchidplugins.tpa.TpaMsg;
import com.orchidplugins.tpa.TpaUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TpaCommand implements CommandExecutor, TabCompleter {

    private final ConfigManager config;
    private final RequestManager requestManager;
    private final TpaManager tpaManager;
    private final TeleportService teleportService;

    public TpaCommand(ConfigManager config, RequestManager requestManager, TpaManager tpaManager,
                      TeleportService teleportService) {
        this.config = config;
        this.requestManager = requestManager;
        this.tpaManager = tpaManager;
        this.teleportService = teleportService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length != 1) {
            TpaMsg.send(config, player, "<red>Usage: /tpa <player>");
            return true;
        }
        Player target = TpaUtil.getOnlinePlayer(args[0]);
        if (target == null) {
            TpaMsg.send(config, player, "<red>Player <yellow>" + args[0] + " <red>is not online.");
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            TpaMsg.send(config, player, "<red>You cannot send a TP request to yourself.");
            return true;
        }
        if (!tpaManager.accepts(target.getUniqueId(), player.getUniqueId(), Request.Type.TPA)) {
            TpaMsg.send(config, player, "<red>" + target.getName() + " <red>is not accepting TPA requests.");
            return true;
        }
        if (requestManager.find(target.getUniqueId(), player.getUniqueId(), Request.Type.TPA).isPresent()) {
            TpaMsg.send(config, player, "<red>You already have a pending TPA request with <yellow>"
                    + target.getName() + "<red>.");
            return true;
        }

        Request request = new Request(player.getUniqueId(), target.getUniqueId(), Request.Type.TPA,
                config.tpaRequestTtlSeconds() * 1000L);
        requestManager.add(request);

        if (tpaManager.get(target.getUniqueId()).isAutoTpa()) {
            requestManager.cancel(request);
            TpaMsg.send(config, target, "<green><italic>Auto-accept</italic> accepted <yellow>"
                    + player.getName() + "<green>'s <yellow>TPA <green>request.");
            teleportService.begin(player, target.getUniqueId(), target.getName(), Request.Type.TPA);
            return true;
        }

        TpaMsg.send(config, player, "<green>TP request sent to <yellow>" + target.getName()
                + "<green>. <gray>They have " + config.tpaRequestTtlSeconds() + " seconds to accept.");
        TpaMsg.send(config, player, "<gray>» <red>TPTrap is NOT allowed<reset> and gets reported if you die.");
        target.sendMessage(TpaMsg.requestMessage(config, player.getName(), Request.Type.TPA));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (Player online : sender.getServer().getOnlinePlayers()) {
            if (online.getName().toLowerCase().startsWith(args[0].toLowerCase())
                    && !online.getUniqueId().equals(sender instanceof Player p ? p.getUniqueId() : null)) {
                names.add(online.getName());
            }
        }
        return names;
    }
}