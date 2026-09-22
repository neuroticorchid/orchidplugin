package com.orchidplugins.commands;

import com.orchidplugins.managers.ConfigManager;
import com.orchidplugins.managers.SmpManager;
import com.orchidplugins.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class SmpCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList("start", "gather", "setborder");

    private final SmpManager smpManager;
    private final ConfigManager config;

    public SmpCommand(SmpManager smpManager, ConfigManager config) {
        this.smpManager = smpManager;
        this.config = config;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "gather":
                if (!smpManager.gather()) {
                    Msg.send(sender, "<red>\u274c A launch countdown is already running - "
                            + "use <yellow>/smp start <gray>to launch.");
                    return true;
                }
                Msg.send(sender, "<green>\u2714 Gather phase started - border shrunk, everyone at 0, 0.");
                return true;
            case "start":
                return handleStart(sender, args);
            case "setborder":
                return handleSetBorder(sender, args);
            default:
                sendUsage(sender);
                return true;
        }
    }

    private boolean handleStart(CommandSender sender, String[] args) {
        int seconds;
        if (args.length >= 2 && (args[1].equalsIgnoreCase("now") || args[1].equals("0"))) {
            seconds = 0;
        } else if (args.length >= 2) {
            try {
                seconds = Math.max(0, Integer.parseInt(args[1]));
            } catch (NumberFormatException e) {
                Msg.send(sender, "<red>Invalid seconds '<yellow>" + args[1] + "<red>' - use a number or <yellow>now<red>.");
                return true;
            }
        } else {
            seconds = config.smpStartSeconds();
        }
        if (!smpManager.start(seconds)) {
            Msg.send(sender, "<red>\u274c A launch countdown is already in progress!");
            return true;
        }
        Msg.send(sender, "<green>\u2714 Launch countdown started (" + Math.max(seconds, 0)
                + (seconds == 1 ? " second" : " seconds") + ").");
        return true;
    }

    private boolean handleSetBorder(CommandSender sender, String[] args) {
        int radius;
        double centerX;
        double centerZ;
        try {
            radius = args.length >= 2
                    ? Math.max(1, Integer.parseInt(args[1]))
                    : config.smpStartRadius();
            centerX = args.length >= 3 ? Double.parseDouble(args[2]) : config.smpCenterX();
            centerZ = args.length >= 4 ? Double.parseDouble(args[3]) : config.smpCenterZ();
        } catch (NumberFormatException e) {
            Msg.send(sender, "<red>Invalid number - usage: <yellow>/smp setborder [radius] [x] [z]");
            return true;
        }
        if (!smpManager.setBorder(radius, centerX, centerZ)) {
            Msg.send(sender, "<red>\u274c SMP world not found - check <yellow>smp.world <red>in config.yml.");
            return true;
        }
        Msg.send(sender, "<green>\u2714 Border set to <aqua>" + radius + " <green>around <gold>"
                + (int) centerX + ", " + (int) centerZ + "<green>.");
        return true;
    }

    private void sendUsage(CommandSender sender) {
        Msg.send(sender, "<gold><bold>SMP Launch</bold></gold>");
        Msg.send(sender, "<yellow>/smp gather <gray>- shrink the border to <aqua>" + config.smpStartRadius()
                + " <gray>at 0, 0 and move everyone there");
        Msg.send(sender, "<yellow>/smp start [seconds|now] <gray>- count down (default <aqua>"
                + config.smpStartSeconds() + "s<gray>), then expand the border to <aqua>"
                + config.smpEndRadius() + " <gray>+ GO");
        Msg.send(sender, "<yellow>/smp setborder [radius] [x] [z] <gray>- manually set the border (no teleport)");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return match(args[0], SUBCOMMANDS);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            return Arrays.asList("now", String.valueOf(config.smpStartSeconds()), "10", "30", "60");
        }
        return List.of();
    }

    private List<String> match(String input, List<String> options) {
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(input.toLowerCase(Locale.ROOT))) {
                result.add(option);
            }
        }
        return result;
    }
}