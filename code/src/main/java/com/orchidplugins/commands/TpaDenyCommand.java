package com.orchidplugins.commands;

import com.orchidplugins.managers.ConfigManager;
import com.orchidplugins.tpa.Request;
import com.orchidplugins.tpa.RequestManager;
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
import java.util.Optional;

public class TpaDenyCommand implements CommandExecutor, TabCompleter {

    private final ConfigManager config;
    private final RequestManager requestManager;

    public TpaDenyCommand(ConfigManager config, RequestManager requestManager) {
        this.config = config;
        this.requestManager = requestManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Optional<Request> request;
        if (args.length == 0) {
            request = requestManager.first(player.getUniqueId());
        } else {
            Player requester = TpaUtil.getOnlinePlayer(args[0]);
            if (requester == null) {
                TpaMsg.send(config, player, "<red>Player <yellow>" + args[0] + " <red>is not online.");
                return true;
            }
            request = requestManager.find(player.getUniqueId(), requester.getUniqueId());
        }

        if (request.isEmpty()) {
            TpaMsg.send(config, player, "<red>You have no pending TP requests"
                    + (args.length > 0 ? " from <yellow>" + args[0] + "<red>." : "."));
            return true;
        }
        Request denied = request.get();
        requestManager.cancel(denied);

        TpaMsg.send(config, player, "<red>You denied the request.");
        Player requester = Bukkit.getPlayer(denied.getRequester());
        if (requester != null) {
            TpaMsg.send(config, requester, "<yellow>" + player.getName() + " <red>denied your TP request.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || args.length > 1) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (Request request : requestManager.forTarget(player.getUniqueId())) {
            String name = TpaUtil.nameOf(request.getRequester());
            if (name.toLowerCase().startsWith(args[0].toLowerCase()) && !names.contains(name)) {
                names.add(name);
            }
        }
        return names;
    }
}