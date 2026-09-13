package com.orchidplugins.commands;

import com.orchidplugins.managers.DeathGameManager;
import com.orchidplugins.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class StopMovingCommand implements CommandExecutor {

    private final DeathGameManager deathGameManager;

    public StopMovingCommand(DeathGameManager deathGameManager) {
        this.deathGameManager = deathGameManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length > 0) {
            String first = args[0].toLowerCase(Locale.ROOT);
            if (first.equals("stop") || first.equals("cancel") || first.equals("end")) {
                deathGameManager.stop();
                return true;
            }
        }
        DeathGameManager.VoteMode mode = DeathGameManager.VoteMode.BOTH;
        for (String arg : args) {
            String a = arg.toLowerCase(Locale.ROOT);
            if (a.equals("-die") || a.equals("-d")) {
                if (mode != DeathGameManager.VoteMode.BOTH) {
                    Msg.send(sender, "<red>Use only one of -gay or -die!");
                    return true;
                }
                mode = DeathGameManager.VoteMode.DIE;
            } else if (a.equals("-gay") || a.equals("-gae")) {
                if (mode != DeathGameManager.VoteMode.BOTH) {
                    Msg.send(sender, "<red>Use only one of -gay or -die!");
                    return true;
                }
                mode = DeathGameManager.VoteMode.GAY;
            } else {
                Msg.send(sender, "<red>Unknown flag '<aqua>" + arg + "<red>'! Use <yellow>-gay <red>or <yellow>-die<red>.");
                return true;
            }
        }
        if (!deathGameManager.start(mode)) {
            Msg.send(sender, "<red>[Death Game] The game is already starting/running!");
        }
        return true;
    }
}