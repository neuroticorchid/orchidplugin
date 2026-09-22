package com.orchidplugins.commands;

import com.orchidplugins.managers.ConfigManager;
import com.orchidplugins.managers.TpaManager;
import com.orchidplugins.tpa.FriendManager;
import com.orchidplugins.tpa.TpaMsg;
import com.orchidplugins.tpa.TpaUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class FriendCommand implements CommandExecutor, TabCompleter {

    private final ConfigManager config;
    private final TpaManager tpaManager;
    private final FriendManager friendManager;

    public FriendCommand(ConfigManager config, TpaManager tpaManager, FriendManager friendManager) {
        this.config = config;
        this.tpaManager = tpaManager;
        this.friendManager = friendManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length == 0) {
            TpaMsg.sendFriend(config, player, "<red>Usage: /friend <add|remove|list> [player]");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "add" -> add(player, args);
            case "remove" -> remove(player, args);
            case "list" -> list(player);
            default -> TpaMsg.sendFriend(config, player, "<red>Usage: /friend <add|remove|list> [player]");
        }
        return true;
    }

    private void add(Player player, String[] args) {
        if (args.length != 2) {
            TpaMsg.sendFriend(config, player, "<red>Usage: /friend add <player>");
            return;
        }
        UUID targetId = TpaUtil.resolve(args[1]);
        if (targetId == null) {
            TpaMsg.sendFriend(config, player, "<red>Player <yellow>" + args[1] + " <red>not found.");
            return;
        }
        if (targetId.equals(player.getUniqueId())) {
            TpaMsg.sendFriend(config, player, "<red>You cannot add yourself as a friend.");
            return;
        }
        if (tpaManager.isFriend(player.getUniqueId(), targetId)) {
            TpaMsg.sendFriend(config, player, "<yellow>" + TpaUtil.nameOf(targetId)
                    + " <red>is already your friend.");
            return;
        }
        if (friendManager.hasPending(targetId)) {
            TpaMsg.sendFriend(config, player, "<red>A friend request to <yellow>" + TpaUtil.nameOf(targetId)
                    + " <red>is already pending.");
            return;
        }
        Player target = Bukkit.getPlayer(targetId);
        if (target == null) {
            TpaMsg.sendFriend(config, player,
                    "<yellow>" + TpaUtil.nameOf(targetId) + " <red>must be online to accept a friend request.");
            return;
        }
        friendManager.send(targetId, player.getUniqueId());
        TpaMsg.sendFriend(config, player, "<green>Friend request sent to <yellow>" + target.getName()
                + "<green>. They have " + config.tpaRequestTtlSeconds() + " seconds to accept.");
        target.sendMessage(TpaMsg.friendRequestMessage(config, player.getName()));
    }

    private void remove(Player player, String[] args) {
        if (args.length != 2) {
            TpaMsg.sendFriend(config, player, "<red>Usage: /friend remove <player>");
            return;
        }
        UUID targetId = TpaUtil.resolve(args[1]);
        if (targetId == null) {
            TpaMsg.sendFriend(config, player, "<red>Player <yellow>" + args[1] + " <red>not found.");
            return;
        }
        if (!tpaManager.isFriend(player.getUniqueId(), targetId)) {
            TpaMsg.sendFriend(config, player, "<yellow>" + TpaUtil.nameOf(targetId)
                    + " <red>is not your friend.");
            return;
        }
        tpaManager.removeFriendship(player.getUniqueId(), targetId);
        Player target = Bukkit.getPlayer(targetId);
        if (target != null) {
            TpaMsg.sendFriend(config, target, "<yellow>" + player.getName() + " <red>removed you as a friend.");
        }
        TpaMsg.sendFriend(config, player, "<green>Removed <yellow>" + TpaUtil.nameOf(targetId)
                + " <green>from your friends.");
    }

    private void list(Player player) {
        List<UUID> friends = tpaManager.getFriends(player.getUniqueId());
        if (friends.isEmpty()) {
            TpaMsg.sendFriend(config, player, "<gray>You have no friends. Use <yellow>/friend add <player>"
                    + " <gray>to add one!");
            return;
        }
        StringBuilder sb = new StringBuilder("<green>Friends <gray>(").append(friends.size()).append(")<green>:");
        for (UUID uuid : friends) {
            sb.append("\n").append("<yellow>» <white>").append(TpaUtil.nameOf(uuid));
        }
        TpaMsg.sendFriend(config, player, sb.toString());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("add", "remove", "list").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            if (!(sender instanceof Player player)) {
                return List.of();
            }
            return tpaManager.getFriends(player.getUniqueId()).stream()
                    .map(TpaUtil::nameOf)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }
        if (args.length == 2) {
            return sender.getServer().getOnlinePlayers().stream()
                    .filter(p -> p.getName().toLowerCase().startsWith(args[1].toLowerCase())
                            && !p.getUniqueId().equals(sender instanceof Player p2 ? p2.getUniqueId() : null))
                    .map(Player::getName)
                    .toList();
        }
        return List.of();
    }
}