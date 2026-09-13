package com.orchidplugins.managers;

import com.orchidplugins.OrchidPlugins;
import com.orchidplugins.util.Msg;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class GiveawayManager {

    private static final String[] DEFAULT_PHRASES = {
            "neurosamaisnotgay", "vedaliscool", "cutevtuber", "evilneuro", "gymbag", "turtle"
    };

    public enum Status {
        IDLE, OPEN, ROLLING
    }

    private final Map<UUID, String> entries = new LinkedHashMap<>();
    private String targetPhrase;
    private Status status = Status.IDLE;

    public boolean start(String phrase) {
        if (status != Status.IDLE) {
            return false;
        }
        entries.clear();
        targetPhrase = phrase == null || phrase.isBlank()
                ? DEFAULT_PHRASES[(int) (Math.random() * DEFAULT_PHRASES.length)]
                : phrase;
        status = Status.OPEN;
        for (Player player : OrchidPlugins.getInstance().getServer().getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
        }
        Msg.titleAll("<light_purple><bold>GIVEAWAY STARTED!",
                "<yellow>Type <aqua>" + targetPhrase + " <yellow>in chat to enter!", 4, 80, 10);
        Msg.broadcast("<light_purple><bold>[GIVEAWAY] <yellow>A new giveaway has started!");
        Msg.broadcast("<light_purple><bold>[GIVEAWAY] <yellow>Type <aqua><bold>" + targetPhrase
                + " <yellow>in chat to enter!");
        return true;
    }

    public void cancel() {
        status = Status.IDLE;
        entries.clear();
        targetPhrase = null;
    }

    public boolean isOpen() {
        return status == Status.OPEN;
    }

    public String getTargetPhrase() {
        return targetPhrase;
    }

    public boolean submitEntry(Player player, String message) {
        if (status != Status.OPEN || !message.strip().equalsIgnoreCase(targetPhrase)) {
            return false;
        }
        if (entries.containsKey(player.getUniqueId())) {
            Msg.send(player, "<red>[Giveaway] You have already entered this giveaway!");
            return true;
        }
        entries.put(player.getUniqueId(), player.getName());
        Msg.send(player, "<green>[Giveaway] Your vote was placed in bet!");
        Msg.broadcast("<gray>[Giveaway] <aqua>" + player.getName() + "<gray>'s vote was placed in bet!");
        return true;
    }

    public boolean isRolling() {
        return status == Status.ROLLING;
    }

    public void end() {
        if (status == Status.IDLE) {
            return;
        }
        status = Status.ROLLING;

        if (entries.isEmpty()) {
            Msg.broadcast("<red><bold>[GIVEAWAY] <red>The giveaway ended, but no one entered!");
            cancel();
            return;
        }

        List<Player> candidates = new ArrayList<>();
        for (UUID uuid : entries.keySet()) {
            Player player = OrchidPlugins.getInstance().getServer().getPlayer(uuid);
            if (player != null) {
                candidates.add(player);
            }
        }

        if (candidates.isEmpty()) {
            Msg.broadcast("<red><bold>[GIVEAWAY] <red>No entries were online when the winner was rolled!");
            cancel();
            return;
        }

        new AnimationManager(OrchidPlugins.getInstance()).roll(candidates, "PICKING WINNER...",
                "<aqua><bold>", winner -> {
                    Msg.broadcast("<light_purple><bold>[GIVEAWAY] <yellow>Congratulations <aqua><bold>"
                            + winner.getName() + "<yellow>! You won the giveaway!");
                    Msg.titleAll("<green><bold>GIVEAWAY WINNER!", "<aqua><bold>" + winner.getName()
                            + " <yellow>won the giveaway!", 4, 80, 10);
                    OrchidPlugins.getInstance().getLogger().info("[Giveaway] WINNER SELECTED: "
                            + winner.getName() + " (Total entries: " + entries.size() + ")");
                    cancel();
                });
    }
}