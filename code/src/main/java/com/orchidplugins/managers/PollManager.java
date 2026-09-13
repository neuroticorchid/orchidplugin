package com.orchidplugins.managers;

import com.orchidplugins.util.Msg;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Chat-answered poll. Players type a choice number or the exact choice text
 * in chat to cast (or switch) their vote. Ends automatically after the
 * configured duration or early via /endpoll.
 */
public final class PollManager {

    public enum Status {
        IDLE, OPEN
    }

    private final com.orchidplugins.OrchidPlugins plugin;
    private final ConfigManager config;

    private String question;
    private List<String> choices = new ArrayList<>();
    private final Map<UUID, Integer> votes = new LinkedHashMap<>();
    private int endTask = -1;
    private Status status = Status.IDLE;

    public PollManager(com.orchidplugins.OrchidPlugins plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    public boolean start(String question, List<String> choices) {
        if (status != Status.IDLE) {
            return false;
        }
        this.question = question.trim();
        this.choices = new ArrayList<>();
        for (String choice : choices) {
            String trimmed = choice.trim();
            if (!trimmed.isEmpty()) {
                this.choices.add(trimmed);
            }
        }
        votes.clear();
        status = Status.OPEN;

        String name = config.serverName();
        broadcast("<gray>" + name + "<gray>: <yellow><bold>POLL STARTED!");
        broadcast("<gray>" + name + "<gray>: <white>" + question);
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < this.choices.size(); i++) {
            line.append("<gold>").append(i + 1).append(") <aqua>").append(this.choices.get(i)).append("   ");
        }
        broadcast("<gray>" + name + "<gray>: " + line);
        broadcast("<gray>" + name + "<gray>: <gray>Type the <yellow>number <gray>or the <aqua>choice <gray>in chat to vote! You have <yellow>"
                + config.pollSeconds() + " <gray>seconds.");

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
        }
        Msg.titleAll("<gold><bold>POLL!", "<white>" + question, 4, 60, 10);

        endTask = plugin.getServer().getScheduler().runTaskLater(plugin, this::end,
                config.pollSeconds() * 20L).getTaskId();
        return true;
    }

    public boolean isOpen() {
        return status == Status.OPEN;
    }

    public List<String> getChoices() {
        return choices;
    }

    public String getQuestion() {
        return question;
    }

    /**
     * Consumes the chat message if it is a valid vote (number or exact choice text).
     * Returns true when the message should be cancelled (a vote was cast or switched).
     */
    public boolean castVote(Player player, String message) {
        if (status != Status.OPEN) {
            return false;
        }
        String text = message.trim();
        int index = resolveChoice(text);
        if (index < 0) {
            return false;
        }
        Integer previous = votes.get(player.getUniqueId());
        if (previous != null && previous == index) {
            Msg.send(player, config.serverName() + " <red>You already voted for that option!");
            return true;
        }
        votes.put(player.getUniqueId(), index);
        String old = previous == null ? "cast your vote for" : "switched your vote to";
        Msg.send(player, config.serverName() + " <green>You " + old + " <aqua>"
                + choices.get(index) + "<green>!");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1f);
        return true;
    }

    private int resolveChoice(String text) {
        for (int i = 0; i < choices.size(); i++) {
            if (Integer.toString(i + 1).equals(text) || choices.get(i).equalsIgnoreCase(text)) {
                return i;
            }
        }
        return -1;
    }

    public void end() {
        if (status != Status.OPEN) {
            return;
        }
        status = Status.IDLE;
        if (endTask != -1) {
            plugin.getServer().getScheduler().cancelTask(endTask);
            endTask = -1;
        }

        String name = config.serverName();
        broadcast("<gray>" + name + "<gray>: <yellow><bold>POLL ENDED! <white>"
                + question + " <gray>(" + votes.size() + " votes)");

        Map<Integer, Integer> counts = new HashMap<>();
        for (Integer choice : votes.values()) {
            counts.merge(choice, 1, Integer::sum);
        }

        int max = -1;
        List<String> winners = new ArrayList<>();
        for (int i = 0; i < choices.size(); i++) {
            int count = counts.getOrDefault(i, 0);
            broadcast("<gray>" + name + "<gray>: <gold>" + (i + 1) + ") <aqua>"
                    + choices.get(i) + " <gray>\u2014 <white>" + count + " <gray>votes");
            if (count > max) {
                max = count;
                winners.clear();
                winners.add(choices.get(i));
            } else if (count == max) {
                winners.add(choices.get(i));
            }
        }

        if (max > 0) {
            Msg.titleAll("<gold><bold>POLL RESULT!",
                    "<white>" + String.join("<gray>, <aqua>", winners), 4, 80, 10);
            broadcast("<gray>" + name + "<gray>: <green><bold>WINNER: <aqua>"
                    + String.join("<gray>, <aqua>", winners)
                    + " <gray>with <yellow>" + max + " <gray>votes!");
        } else {
            broadcast("<gray>" + name + "<gray>: <red>No one voted in this poll. Sad!");
        }
    }

    private void broadcast(String text) {
        Msg.broadcast(text);
    }
}