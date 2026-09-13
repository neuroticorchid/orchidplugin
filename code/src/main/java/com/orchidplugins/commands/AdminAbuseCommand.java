package com.orchidplugins.commands;

import com.orchidplugins.db.SqliteDatabase;
import com.orchidplugins.managers.AttributeManager;
import com.orchidplugins.managers.ConfigManager;
import com.orchidplugins.managers.WebhookManager;
import com.orchidplugins.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class AdminAbuseCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "bigcharacter", "char", "reach", "speed", "jump", "health", "hp", "reset", "r");

    private static final List<String> CANONICAL = Arrays.asList(
            "bigcharacter", "reach", "speed", "jump", "health", "reset");

    private static final List<String> FLAGS = Arrays.asList("-s", "silent", "-b", "broadcast");

    private final AttributeManager attributeManager;
    private final ConfigManager config;
    private final org.bukkit.plugin.Plugin plugin;
    private final SqliteDatabase database;
    private final WebhookManager webhook;

    public AdminAbuseCommand(AttributeManager attributeManager, ConfigManager config,
                             org.bukkit.plugin.Plugin plugin, SqliteDatabase database, WebhookManager webhook) {
        this.attributeManager = attributeManager;
        this.config = config;
        this.plugin = plugin;
        this.database = database;
        this.webhook = webhook;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            Msg.send(sender, "<red>Usage: /adminabuse <bigcharacter|reach|speed|jump|health|reset> [player] [value] [-b|-s]");
            Msg.send(sender, "<gray>Aliases: /aa. Sub aliases: char, hp, r.");
            Msg.send(sender, "<gray>-b = broadcast publicly | -s = keep private (default: "
                    + (config.adminAbuseBroadcastByDefault() ? "public" : "private") + ")");
            return true;
        }

        boolean forcedPublic = false;
        boolean forcedSilent = false;
        List<String> clean = new ArrayList<>();
        for (String arg : args) {
            String lower = arg.toLowerCase(Locale.ROOT);
            if (lower.equals("-s") || lower.equals("silent")) {
                forcedSilent = true;
            } else if (lower.equals("-b") || lower.equals("broadcast")) {
                forcedPublic = true;
            } else {
                clean.add(arg);
            }
        }
        boolean broadcast = forcedPublic ? true : (forcedSilent ? false : config.adminAbuseBroadcastByDefault());

        if (clean.isEmpty()) {
            Msg.send(sender, "<red>Usage: /adminabuse <bigcharacter|reach|speed|jump|health|reset> [player] [value] [-b|-s]");
            return true;
        }

        String sub = clean.get(0).toLowerCase(Locale.ROOT);
        Player target;
        String valueArg = null;

        if (clean.size() >= 2) {
            Player match = Bukkit.getPlayerExact(clean.get(1));
            if (match != null) {
                target = match;
                if (clean.size() >= 3) {
                    valueArg = clean.get(2);
                }
            } else {
                if (sender instanceof Player p) {
                    target = p;
                    valueArg = clean.get(1);
                } else {
                    Msg.send(sender, "<red>Unknown player '<yellow>" + clean.get(1) + "<red>' - specify an online player.");
                    return true;
                }
            }
        } else {
            if (!(sender instanceof Player p)) {
                Msg.send(sender, "<red>Console must specify a target player.");
                return true;
            }
            target = p;
        }

        switch (sub) {
            case "bigcharacter":
            case "char": {
                double scale = parseDouble(valueArg, 2.0);
                attributeManager.setScale(target, scale);
                resolveAbuse(sender, target, broadcast, "bigcharacter", scale + "x scale");
                return true;
            }
            case "reach": {
                double blocks = parseDouble(valueArg, 10.0);
                attributeManager.setReach(target, blocks);
                resolveAbuse(sender, target, broadcast, "reach", blocks + " blocks");
                return true;
            }
            case "speed": {
                double multiplier = parseDouble(valueArg, 2.0);
                attributeManager.setSpeedMultiplier(target, multiplier);
                resolveAbuse(sender, target, broadcast, "speed", multiplier + "x speed");
                return true;
            }
            case "jump": {
                double amount = parseDouble(valueArg, 1.0);
                attributeManager.setJumpBoost(target, amount);
                resolveAbuse(sender, target, broadcast, "jump", "+" + amount + " jump strength");
                return true;
            }
            case "health":
            case "hp": {
                double hearts = parseDouble(valueArg, 100.0);
                attributeManager.setHealth(target, hearts);
                resolveAbuse(sender, target, broadcast, "health", hearts + " hearts");
                return true;
            }
            case "reset":
            case "r": {
                attributeManager.reset(target);
                resolveAbuse(sender, target, broadcast, "reset", "all attribute modifications cleared");
                return true;
            }
            default:
                Msg.send(sender, "<red>Unknown subcommand '<yellow>" + sub + "<red>'. "
                        + "Valid: bigcharacter, reach, speed, jump, health, reset.");
                return true;
        }
    }

    private void resolveAbuse(CommandSender sender, Player target, boolean broadcast, String what, String detail) {
        String message = config.prefix("abuse") + "<yellow>" + target.getName() + " <white>now has <gold>"
                + (what.equals("reset") ? "<white>" + detail + "!"
                : what + " <white>(" + detail + ")!");
        if (broadcast) {
            Msg.broadcast(message);
        } else {
            Msg.send(sender, message);
            plugin.getLogger().info(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(Msg.parse(message)));
        }
        database.logAbuse(sender.getName(), target.getName(), what, detail);
        webhook.sendAbuse(sender.getName(), target.getName(), what, detail == null ? "" : detail, broadcast, null);
    }

    private double parseDouble(String value, double fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return match(args[0], CANONICAL);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            names.add("-b");
            names.add("-s");
            return match(args[1], names);
        }
        if (args.length == 3) {
            switch (sub) {
                case "bigcharacter":
                case "char":
                    return match(args[2], Arrays.asList("2", "3", "5"));
                case "reach":
                    return match(args[2], Arrays.asList("10", "20", "50"));
                case "speed":
                    return match(args[2], Arrays.asList("2", "3", "5"));
                case "jump":
                    return match(args[2], Arrays.asList("0.5", "1", "2"));
                case "health":
                case "hp":
                    return match(args[2], Arrays.asList("50", "100", "200"));
                default:
                    return Collections.emptyList();
            }
        }
        if (args.length == 4) {
            return match(args[3], Arrays.asList("-b", "-s"));
        }
        return Collections.emptyList();
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