package com.orchidplugins.commands;

import com.orchidplugins.managers.DeathGameManager;
import com.orchidplugins.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class DeathGameCommand implements CommandExecutor {

    private final DeathGameManager deathGameManager;

    public DeathGameCommand(DeathGameManager deathGameManager) {
        this.deathGameManager = deathGameManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            Msg.send(sender, "<red>Usage: /deathgame start [-die|-gay]|stop");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("start")) {
            DeathGameManager.VoteMode mode = parseMode(sender, args);
            if (mode == null) {
                return true;
            }
            if (!deathGameManager.start(mode)) {
                Msg.send(sender, "<red>[Death Game] The game is already starting/running!");
            }
            return true;
        }
        if (sub.equals("stop")) {
            deathGameManager.stop();
            return true;
        }
        Msg.send(sender, "<red>Usage: /deathgame start [-die|-gay]|stop");
        return true;
    }

    private DeathGameManager.VoteMode parseMode(CommandSender sender, String[] args) {
        DeathGameManager.VoteMode mode = DeathGameManager.VoteMode.BOTH;
        for (int i = 1; i < args.length; i++) {
            String arg = args[i].toLowerCase(Locale.ROOT);
            if (arg.equals("-die") || arg.equals("-d")) {
                if (mode != DeathGameManager.VoteMode.BOTH) {
                    Msg.send(sender, "<red>Use only one of -gay or -die!");
                    return null;
                }
                mode = DeathGameManager.VoteMode.DIE;
            } else if (arg.equals("-gay") || arg.equals("-gae")) {
                if (mode != DeathGameManager.VoteMode.BOTH) {
                    Msg.send(sender, "<red>Use only one of -gay or -die!");
                    return null;
                }
                mode = DeathGameManager.VoteMode.GAY;
            } else {
                Msg.send(sender, "<red>Unknown flag '<aqua>" + arg + "<red>'! Use <yellow>-gay <red>or <yellow>-die<red>.");
                return null;
            }
        }
        return mode;
    }
}