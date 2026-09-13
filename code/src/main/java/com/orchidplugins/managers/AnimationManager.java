package com.orchidplugins.managers;

import com.orchidplugins.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Random;

public final class AnimationManager {

    private static final int STEPS = 16;

    private final Plugin plugin;
    private final Random random = new Random();

    public AnimationManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public interface RollCallback {
        void onComplete(Player selected);
    }

    public void roll(List<Player> candidates, String titleText, String subtitlePrefix,
                     RollCallback callback) {
        if (candidates == null || candidates.isEmpty()) {
            return;
        }
        playToAll(Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
        step(candidates, titleText, subtitlePrefix, callback, 0);
    }

    private void step(List<Player> candidates, String titleText, String subtitlePrefix,
                      RollCallback callback, int step) {
        if (step >= STEPS) {
            Player winner = candidates.get(random.nextInt(candidates.size()));
            playToAll(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            callback.onComplete(winner);
            return;
        }

        Player temp = candidates.get(random.nextInt(candidates.size()));
        playToAll(Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1.5f);
        Msg.titleAll("<red><bold>" + titleText, subtitlePrefix + " <yellow><bold>"
                + temp.getName(), 0, 20, 0);

        long delay = step > 12 ? 5L : step > 8 ? 3L : step > 4 ? 2L : 1L;
        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                step(candidates, titleText, subtitlePrefix, callback, step + 1), delay);
    }

    private void playToAll(Sound sound, float volume, float pitch) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }
}