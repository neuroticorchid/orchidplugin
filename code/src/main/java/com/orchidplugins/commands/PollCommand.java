package com.orchidplugins.commands;

import com.orchidplugins.managers.PollManager;
import com.orchidplugins.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PollCommand implements CommandExecutor {

    public static final int MAX_CHOICES = 10;
    public static final int MIN_CHOICES = 2;
    private static final Pattern BRACKET = Pattern.compile("\\[([^\\]]+)\\]");

    private final PollManager pollManager;
    private final com.orchidplugins.managers.ConfigManager config;

    public PollCommand(PollManager pollManager, com.orchidplugins.managers.ConfigManager config) {
        this.pollManager = pollManager;
        this.config = config;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        String joined = String.join(" ", args);
        String question;
        List<String> choices = new ArrayList<>();

        if (joined.contains("|")) {
            String[] parts = joined.split("\\|", -1);
            if (parts.length < MIN_CHOICES + 1) {
                Msg.send(sender, "<red>A poll needs a question and at least " + MIN_CHOICES
                        + " choices, separated by '|'.");
                sendExample(sender);
                return true;
            }
            question = parts[0].trim();
            for (int i = 1; i < parts.length; i++) {
                String c = parts[i].trim();
                if (c.isEmpty()) continue;
                if (hasDupe(sender, choices, c)) return true;
                choices.add(c);
            }
        } else {
            Matcher matcher = BRACKET.matcher(joined);
            boolean hasBracket = false;
            while (matcher.find()) {
                hasBracket = true;
                String c = matcher.group(1).trim();
                if (c.isEmpty()) continue;
                if (hasDupe(sender, choices, c)) return true;
                choices.add(c);
            }
            question = BRACKET.matcher(joined).replaceAll("").trim();
            if (choices.isEmpty() && !hasBracket) {
                choices.add("YES");
                choices.add("NO");
            }
        }

        if (question.isEmpty()) {
            Msg.send(sender, "<red>The poll question cannot be empty!");
            return true;
        }
        if (choices.size() < MIN_CHOICES) {
            Msg.send(sender, "<red>A poll needs at least " + MIN_CHOICES + " choices.");
            return true;
        }
        if (choices.size() > MAX_CHOICES) {
            Msg.send(sender, "<red>A poll can have at most " + MAX_CHOICES + " choices.");
            return true;
        }
        if (!pollManager.start(question, choices)) {
            Msg.send(sender, "<red>[Poll] A poll is already running! End it with /endpoll.");
            return true;
        }
        sender.getServer().getLogger().info("[Poll] POLL STARTED by " + sender.getName()
                + " | Q: " + question + " | Choices: " + String.join(", ", choices)
                + " | Duration: " + config.pollSeconds() + "s");
        return true;
    }

    private boolean hasDupe(CommandSender sender, List<String> choices, String candidate) {
        for (String c : choices) {
            if (c.equalsIgnoreCase(candidate)) {
                Msg.send(sender, "<red>Duplicate choice '<aqua>" + candidate + "<red>'.");
                return true;
            }
        }
        return false;
    }

    private void sendUsage(CommandSender sender) {
        Msg.send(sender, "<red>Usage: /poll <question> [| or [choices]...]");
        Msg.send(sender, "<gray>Default choices: YES / NO.");
        Msg.send(sender, "<yellow>/poll Should I reset? <gray>→ defaults to YES / NO");
        Msg.send(sender, "<yellow>/poll Best mob? [Creeper] [Zombie] <gray>→ bracket syntax");
        Msg.send(sender, "<yellow>/poll Best mob? | Creeper | Zombie <gray>→ pipe syntax");
    }

    private void sendExample(CommandSender sender) {
        Msg.send(sender, "<yellow>/poll Best mob? | Creeper | Zombie | Enderman");
    }
}