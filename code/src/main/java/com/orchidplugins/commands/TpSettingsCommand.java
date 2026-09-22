package com.orchidplugins.commands;

import com.orchidplugins.managers.ConfigManager;
import com.orchidplugins.tpa.TpaMsg;
import com.orchidplugins.tpa.TpaUtil;
import com.orchidplugins.tpa.gui.ChestSettingsGui;
import com.orchidplugins.tpa.gui.SettingsGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class TpSettingsCommand implements CommandExecutor, TabCompleter {

    private final ConfigManager config;
    private final SettingsGui dialogGui;
    private final ChestSettingsGui chestGui;

    public TpSettingsCommand(ConfigManager config, SettingsGui dialogGui, ChestSettingsGui chestGui) {
        this.config = config;
        this.dialogGui = dialogGui;
        this.chestGui = chestGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length > 1) {
            TpaMsg.send(config, player, "<red>Usage: /tpsettings [player]");
            return true;
        }
        UUID target = null;
        String targetName = null;
        if (args.length == 1) {
            target = TpaUtil.resolve(args[0]);
            if (target == null) {
                TpaMsg.send(config, player, "<red>Player <yellow>" + args[0] + " <red>not found.");
                return true;
            }
            targetName = TpaUtil.nameOf(target);
        }
        if (ChestSettingsGui.supportsDialog(player)) {
            if (target == null) {
                dialogGui.open(player);
            } else {
                dialogGui.openWithPlayer(player, target, targetName);
            }
        } else {
            if (target == null) {
                chestGui.open(player);
            } else {
                chestGui.openWithPlayer(player, target);
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return sender.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(prefix))
                    .toList();
        }
        return List.of();
    }
}