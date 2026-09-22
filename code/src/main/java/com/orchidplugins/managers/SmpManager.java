package com.orchidplugins.managers;

import com.orchidplugins.OrchidPlugins;
import com.orchidplugins.util.Msg;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public final class SmpManager {

    public enum Phase {
        IDLE, GATHER, COUNTDOWN
    }

    private final OrchidPlugins plugin;
    private Phase phase = Phase.IDLE;
    private BukkitTask task;

    public SmpManager(OrchidPlugins plugin) {
        this.plugin = plugin;
    }

    public boolean isActive() {
        return phase != Phase.IDLE;
    }

    public boolean isGathering() {
        return phase == Phase.GATHER;
    }

    public boolean isCounting() {
        return phase == Phase.COUNTDOWN;
    }

    /** Shrinks the border to the small start radius, teleports everyone to the center, and opens the gather phase. */
    public boolean gather() {
        if (phase == Phase.COUNTDOWN) {
            return false;
        }
        World world = targetWorld();
        if (world == null) {
            return false;
        }
        ConfigManager config = plugin.getConfigManager();
        setBorder(world, config.smpStartRadius(), config.smpCenterX(), config.smpCenterZ(), 1);
        teleportAll();
        phase = Phase.GATHER;
        Msg.broadcast(config.smpPrefix() + "<yellow><bold>Gather at <gold>0, 0<yellow>! "
                + "<white>The SMP season is about to start - get to spawn!</white>");
        Msg.titleAll("<gold><bold>GATHER AT 0, 0", "<yellow>The SMP starts soon!", 4, 60, 10);
        playToAll(Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
        return true;
    }

    /** Runs the launch countdown, then expands the border + GO. seconds <= 0 launches instantly. */
    public boolean start(int seconds) {
        if (phase == Phase.COUNTDOWN) {
            return false;
        }
        if (phase == Phase.IDLE && !gather()) {
            return false;
        }
        phase = Phase.COUNTDOWN;
        final int[] remaining = {Math.max(0, seconds)};
        Msg.broadcast(plugin.getConfigManager().smpPrefix()
                + "<yellow>The SMP starts in <red><bold>" + Math.max(seconds, 0)
                + "<yellow> seconds! <gray>Get ready...");
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (phase != Phase.COUNTDOWN) {
                cancelTask();
                return;
            }
            if (remaining[0] <= 0) {
                cancelTask();
                launch();
                return;
            }
            int current = remaining[0];
            remaining[0]--;
            tick(current);
        }, 0L, 20L);
        return true;
    }

    /** Sets the world border around the given center without teleporting or changing the phase. */
    public boolean setBorder(int radius, double centerX, double centerZ) {
        World world = targetWorld();
        if (world == null) {
            return false;
        }
        setBorder(world, radius, centerX, centerZ, 1);
        Msg.broadcast(plugin.getConfigManager().smpPrefix() + "<yellow>Border set to <aqua>" + radius
                + " <yellow>blocks radius around <gold>" + (int) centerX + ", " + (int) centerZ + "<yellow>.");
        return true;
    }

    /** Teleports late joiners to the center while the gather phase or countdown is running. */
    public void onJoin(Player player) {
        if (phase == Phase.IDLE) {
            return;
        }
        World world = targetWorld();
        if (world == null) {
            return;
        }
        player.teleport(centerLocation(world));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
        if (phase == Phase.GATHER) {
            Msg.send(player, plugin.getConfigManager().smpPrefix()
                    + "<yellow>You've been moved to <gold>0, 0<yellow> - the SMP is about to start!");
        } else {
            Msg.send(player, plugin.getConfigManager().smpPrefix()
                    + "<yellow>You've been moved to <gold>0, 0<yellow> - the SMP countdown is running!");
        }
    }

    private void tick(int seconds) {
        String color = seconds <= 3 ? "<red>" : (seconds <= 10 ? "<gold>" : "<yellow>");
        Msg.titleAll(color + "<bold>SMP STARTS IN " + seconds,
                seconds <= 3 ? "<yellow>GO GO GO!" : "<gray>Find your spot at 0, 0!", 1, 18, 1);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Msg.actionBar(player, "<gold><bold>" + seconds + "<gray> until the SMP starts!");
            player.playSound(player.getLocation(),
                    seconds <= 3 ? Sound.ENTITY_PLAYER_LEVELUP : Sound.BLOCK_NOTE_BLOCK_PLING, 1f,
                    seconds <= 3 ? 2f : 1.5f);
        }
    }

    private void launch() {
        phase = Phase.IDLE;
        ConfigManager config = plugin.getConfigManager();
        World world = targetWorld();
        if (world != null) {
            setBorder(world, config.smpEndRadius(), config.smpCenterX(), config.smpCenterZ(),
                    config.smpSwellSeconds());
        }
        Msg.broadcast(config.smpPrefix() + "<green><bold>THE SMP HAS STARTED! "
                + "<aqua>GO!<green> The border is expanding - run and claim your land!");
        Msg.titleAll("<green><bold>GO!", "<gold>The border is expanding!", 4, 60, 10);
        playToAll(Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
    }

    private void setBorder(World world, int radius, double centerX, double centerZ, long swellSeconds) {
        WorldBorder border = world.getWorldBorder();
        border.setCenter(centerX, centerZ);
        border.setSize(Math.max(1, radius * 2), Math.max(1, swellSeconds));
    }

    private void teleportAll() {
        World world = targetWorld();
        if (world == null) {
            return;
        }
        Location center = centerLocation(world);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.teleport(center);
        }
        Msg.broadcast(plugin.getConfigManager().smpPrefix()
                + "<yellow>Everyone has been moved to <gold>0, 0<yellow>!");
    }

    private Location centerLocation(World world) {
        ConfigManager config = plugin.getConfigManager();
        int x = (int) config.smpCenterX();
        int z = (int) config.smpCenterZ();
        int y = world.getHighestBlockYAt(x, z);
        if (y < world.getMinHeight()) {
            y = world.getMinHeight();
        }
        return new Location(world, x + 0.5, y + 1, z + 0.5);
    }

    private World targetWorld() {
        World world = plugin.getServer().getWorld(plugin.getConfigManager().smpWorld());
        if (world == null) {
            plugin.getLogger().warning("SMP world '" + plugin.getConfigManager().smpWorld()
                    + "' not found - /smp unavailable.");
        }
        return world;
    }

    private void cancelTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void playToAll(Sound sound, float volume, float pitch) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }
}