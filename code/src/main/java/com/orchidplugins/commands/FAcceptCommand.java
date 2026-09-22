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
import java.util.Optional;
import java.util.UUID;

public class FAcceptCommand implements CommandExecutor, TabCompleter {

    private final ConfigManager config;
    private final TpaManager tpaManager;
    private final FriendManager friendManager;

    public FAcceptCommand(ConfigManager config, TpaManager tpaManager, FriendManager friendManager) {
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
        Optional<UUID> pending = friendManager.requesterOf(player.getUniqueId());
        if (pending.isEmpty()) {
            TpaMsg.sendFriend(config, player, "<red>You have no pending friend requests.");
            return true;
        }
        UUID requesterId = pending.get();
        if (args.length > 0) {
            UUID named = TpaUtil.resolve(args[0]);
            if (named == null || !named.equals(requesterId)) {
                TpaMsg.sendFriend(config, player, "<red>You have no pending friend request from <yellow>"
                        + args[0] + "<red>.");
                return true;
            }
        }
        friendManager.cancel(player.getUniqueId());
        tpaManager.addFriendship(player.getUniqueId(), requesterId);

        TpaMsg.sendFriend(config, player, "<green>You are now friends with <yellow>" + TpaUtil.nameOf(requesterId)
                + "<green>!");
        Player requester = Bukkit.getPlayer(requesterId);
        if (requester != null) {
            TpaMsg.sendFriend(config, requester, "<yellow>" + player.getName()
                    + " <green>accepted your friend request!");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || args.length != 1) {
            return List.of();
        }
        return friendManager.requesterOf(player.getUniqueId())
                .map(id -> List.of(TpaUtil.nameOf(id)))
                .orElseGet(List::of);
    }
}