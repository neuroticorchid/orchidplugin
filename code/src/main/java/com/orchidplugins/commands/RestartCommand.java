package com.orchidplugins.commands;

import com.orchidplugins.managers.ConfigManager;
import com.orchidplugins.managers.RestartManager;
import com.orchidplugins.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class RestartCommand implements CommandExecutor {

    private final RestartManager restartManager;
    private final ConfigManager config;

    public RestartCommand(RestartManager restartManager, ConfigManager config) {
        this.restartManager = restartManager;
        this.config = config;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String input = args.length > 0 ? args[0] : config.restartDefaultTime();
        if (restartManager.isActive() && !input.equalsIgnoreCase("cancel")) {
            Msg.send(sender, "<red>\u274c A restart countdown is already in progress! Use /srestart cancel to stop it.");
            return true;
        }
        if (input.equalsIgnoreCase("cancel")) {
            restartManager.parseTime("cancel");
            return true;
        }
        restartManager.parseTime(input);
        return true;
    }
}