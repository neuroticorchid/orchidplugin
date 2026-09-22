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

public class TpaHereCommand implements CommandExecutor, TabCompleter {

    private final ConfigManager config;
    private final RequestManager requestManager;
    private final TpaManager tpaManager;
    private final TeleportService teleportService;

    public TpaHereCommand(ConfigManager config, RequestManager requestManager, TpaManager tpaManager,
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
            TpaMsg.send(config, player, "<red>Usage: /tpahere <player>");
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
        if (!tpaManager.accepts(target.getUniqueId(), player.getUniqueId(), Request.Type.TPAHERE)) {
            TpaMsg.send(config, player, "<red>" + target.getName() + " <red>is not accepting TPAHERE requests.");
            return true;
        }
        if (requestManager.find(target.getUniqueId(), player.getUniqueId(), Request.Type.TPAHERE).isPresent()) {
            TpaMsg.send(config, player, "<red>You already have a pending TPAHERE request with <yellow>"
                    + target.getName() + "<red>.");
            return true;
        }

        Request request = new Request(player.getUniqueId(), target.getUniqueId(), Request.Type.TPAHERE,
                config.tpaRequestTtlSeconds() * 1000L);
        requestManager.add(request);

        if (tpaManager.get(target.getUniqueId()).isAutoTpaHere()) {
            requestManager.cancel(request);
            TpaMsg.send(config, target, "<green><italic>Auto-accept</italic> accepted <yellow>"
                    + player.getName() + "<green>'s <yellow>TPAHERE <green>request.");
            teleportService.begin(target, player.getUniqueId(), player.getName(), Request.Type.TPAHERE);
            return true;
        }

        TpaMsg.send(config, player, "<green>TPAHERE request sent to <yellow>" + target.getName()
                + "<green>. <gray>They have " + config.tpaRequestTtlSeconds() + " seconds to accept.");
        TpaMsg.send(config, player, "<gray>» <red>TPTrap is NOT allowed<reset> and gets reported if they die.");
        target.sendMessage(TpaMsg.requestMessage(config, player.getName(), Request.Type.TPAHERE));
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