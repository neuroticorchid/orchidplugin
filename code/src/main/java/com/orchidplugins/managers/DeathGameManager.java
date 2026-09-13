package com.orchidplugins.managers;

import com.orchidplugins.util.Msg;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.UUID;

public final class DeathGameManager {

    public enum Phase {
        IDLE, STARTING, ACTIVE, VOTING
    }

    public enum VoteMode {
        BOTH, DIE, GAY
    }

    private final com.orchidplugins.OrchidPlugins plugin;
    private Phase phase = Phase.IDLE;
    private VoteMode voteMode = VoteMode.BOTH;
    private final Map<UUID, Location> frozenPositions = new HashMap<>();
    private final Set<UUID> gayTagged = new HashSet<>();
    private Player target;
    private int votesDie;
    private int votesGay;
    private final Map<UUID, Boolean> hasVoted = new HashMap<>();
    private int countdownTask = -1;
    private int voteTask = -1;
    private int glowTask = -1;

    public DeathGameManager(com.orchidplugins.OrchidPlugins plugin) {
        this.plugin = plugin;
    }

    public boolean isActive() {
        return phase == Phase.ACTIVE;
    }

    public boolean isVoting() {
        return phase == Phase.VOTING;
    }

    public boolean start(VoteMode mode) {
        if (phase == Phase.STARTING || phase == Phase.ACTIVE || phase == Phase.VOTING) {
            return false;
        }
        this.voteMode = mode;
        phase = Phase.STARTING;
        int countdown = plugin.getConfigManager().deathgameCountdownSeconds();
        final int[] remaining = {countdown};
        countdownTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (phase != Phase.STARTING) {
                plugin.getServer().getScheduler().cancelTask(countdownTask);
                countdownTask = -1;
                return;
            }
            if (remaining[0] <= 0) {
                plugin.getServer().getScheduler().cancelTask(countdownTask);
                countdownTask = -1;
                beginActive();
                return;
            }
            int seconds = remaining[0];
            remaining[0]--;
            Msg.broadcast("<red><bold>[DEATH GAME] <yellow>Game starting in <red>" + seconds
                    + (seconds == 1 ? " second" : " seconds") + "<yellow>...");
            playToAll(Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
        }, 0L, 20L).getTaskId();
        return true;
    }

    private void beginActive() {
        phase = Phase.ACTIVE;
        frozenPositions.clear();
        gayTagged.clear();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            frozenPositions.put(player.getUniqueId(), player.getLocation().clone());
        }
        Msg.broadcast("<red><bold>[DEATH GAME] <green><bold>FREEZE! DO NOT MOVE!");
        playToAll(Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
        if (voteMode == VoteMode.DIE) {
            Msg.broadcast("<red><bold>[DEATH GAME] <gray>Mode: <red>INSTANT DEATH <gray>- move and you die!");
        } else if (voteMode == VoteMode.GAY) {
            Msg.broadcast("<red><bold>[DEATH GAME] <gray>Mode: <light_purple>GAY <gray>- move and you get marked gay! (glow only shows when someone is near)");
        }
    }

    public void stop() {
        if (countdownTask != -1) {
            plugin.getServer().getScheduler().cancelTask(countdownTask);
            countdownTask = -1;
        }
        if (voteTask != -1) {
            plugin.getServer().getScheduler().cancelTask(voteTask);
            voteTask = -1;
        }
        cancelGlowTask();
        clearGayTags();
        phase = Phase.IDLE;
        voteMode = VoteMode.BOTH;
        frozenPositions.clear();
        target = null;
        hasVoted.clear();
        votesDie = 0;
        votesGay = 0;
        Msg.broadcast("<red><bold>[DEATH GAME] <green>The game has been canceled/ended.");
    }

    private void clearGayTags() {
        for (UUID id : gayTagged) {
            Player tagged = plugin.getServer().getPlayer(id);
            if (tagged != null && tagged.isOnline() && tagged.hasPotionEffect(PotionEffectType.GLOWING)) {
                tagged.removePotionEffect(PotionEffectType.GLOWING);
            }
        }
        gayTagged.clear();
    }

    public void onMove(Player player, Location from) {
        if (phase != Phase.ACTIVE) {
            return;
        }
        Location frozen = frozenPositions.get(player.getUniqueId());
        if (frozen == null) {
            return;
        }
        if (player.getLocation().distanceSquared(frozen) > 0.01) {
            switch (voteMode) {
                case DIE:
                    executeMover(player);
                    break;
                case GAY:
                    tagGay(player);
                    break;
                default:
                    triggerVote(player);
                    break;
            }
        }
    }

    private void executeMover(Player player) {
        frozenPositions.remove(player.getUniqueId());
        Location loc = player.getLocation();
        player.getWorld().strikeLightningEffect(loc);
        player.getWorld().spawnParticle(Particle.FLASH, loc, 1);
        player.setHealth(0.0);
        playToAll(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1f);
        Msg.broadcast("<red><bold>[DEATH GAME] <aqua>" + player.getName()
                + " <yellow>moved and was <red>EXECUTED <yellow>instantly!");
    }

    private void tagGay(Player player) {
        frozenPositions.remove(player.getUniqueId());
        gayTagged.add(player.getUniqueId());
        playToAll(Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        Msg.broadcast("<light_purple><bold>[DEATH GAME] <aqua>" + player.getName()
                + " <yellow>moved and is now <light_purple><bold>GAY<yellow>!");
        Msg.broadcast("<gray>[DEATH GAME] <gray>Glow shows only when someone is near - bases stay safe!");
        Msg.title(player, "<light_purple><bold>YOU ARE NOW GAY!",
                "<yellow>Run for cover! Glow hides when you're alone.", 4, 80, 10);
        startGlowTaskIfNeeded();
    }

    public void onJoin(Player player) {
        if (phase == Phase.ACTIVE) {
            frozenPositions.put(player.getUniqueId(), player.getLocation().clone());
        }
    }

    private void startGlowTaskIfNeeded() {
        if (glowTask != -1) return;
        glowTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::refreshGayGlow, 0L, 10L).getTaskId();
    }

    private void refreshGayGlow() {
        if (gayTagged.isEmpty()) {
            cancelGlowTask();
            return;
        }
        try {
            double distSq = Math.pow(plugin.getConfigManager().deathgameGlowDistance(), 2);
            Set<UUID> remove = new HashSet<>();
            for (UUID id : gayTagged) {
                Player tagged = plugin.getServer().getPlayer(id);
                if (tagged == null || !tagged.isOnline() || tagged.isDead()) {
                    remove.add(id);
                    continue;
                }
                boolean near = false;
                for (Player other : plugin.getServer().getOnlinePlayers()) {
                    if (other.getUniqueId().equals(id)) continue;
                    if (other.getWorld().equals(tagged.getWorld())
                            && other.getLocation().distanceSquared(tagged.getLocation()) <= distSq) {
                        near = true;
                        break;
                    }
                }
                if (near) {
                    if (!tagged.hasPotionEffect(PotionEffectType.GLOWING)) {
                        tagged.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0, false, false));
                    }
                } else {
                    if (tagged.hasPotionEffect(PotionEffectType.GLOWING)) {
                        tagged.removePotionEffect(PotionEffectType.GLOWING);
                    }
                }
            }
            gayTagged.removeAll(remove);
        } catch (Throwable t) {
            plugin.getLogger().warning("Death game glow task error: " + t.getMessage());
        }
    }

    private void cancelGlowTask() {
        if (glowTask != -1) {
            plugin.getServer().getScheduler().cancelTask(glowTask);
            glowTask = -1;
        }
    }

    private void triggerVote(Player mover) {
        phase = Phase.VOTING;
        frozenPositions.clear();
        gayTagged.clear();
        cancelGlowTask();
        target = mover;
        votesDie = 0;
        votesGay = 0;
        hasVoted.clear();

        Msg.broadcast("<red><bold>[DEATH GAME] <aqua>" + mover.getName() + " <yellow>moved first!");
        Msg.broadcast("<red><bold>[DEATH GAME] <yellow><bold>VOTING STARTED! <gray>You have "
                + plugin.getConfigManager().deathgameVoteSeconds() + " seconds:");
        Msg.broadcast("<red><bold>[DEATH GAME] <yellow>Type <red><bold>/vote die <yellow>or <light_purple><bold>/vote gay");
        playToAll(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

        voteTask = plugin.getServer().getScheduler().runTaskLater(plugin, this::resolveVotes,
                plugin.getConfigManager().deathgameVoteSeconds() * 20L).getTaskId();
    }

    public boolean castVote(Player voter, String choice) {
        if (phase != Phase.VOTING) {
            Msg.send(voter, "<red>[Death Game] There is no active vote right now!");
            return true;
        }
        if (hasVoted.containsKey(voter.getUniqueId())) {
            Msg.send(voter, "<red>[Death Game] You have already cast your vote!");
            return true;
        }
        if (choice.equalsIgnoreCase("die")) {
            votesDie++;
            hasVoted.put(voter.getUniqueId(), true);
            Msg.send(voter, "<green>[Death Game] You voted for <red><bold>DIE<green>!");
            voter.playSound(voter.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1f);
        } else if (choice.equalsIgnoreCase("gay")) {
            votesGay++;
            hasVoted.put(voter.getUniqueId(), true);
            Msg.send(voter, "<green>[Death Game] You voted for <light_purple><bold>BE GAY<green>!");
            voter.playSound(voter.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1f);
        } else {
            Msg.send(voter, "<red>[Death Game] Invalid option! Use <yellow>/vote die <red>or <yellow>/vote gay<red>.");
        }
        return true;
    }

    private void resolveVotes() {
        if (phase != Phase.VOTING) {
            return;
        }
        phase = Phase.IDLE;
        voteTask = -1;

        int die = votesDie;
        int gay = votesGay;
        Player victim = target;

        Msg.broadcast("<red><bold>[DEATH GAME] <yellow><bold>VOTING ENDED!");
        Msg.broadcast("<red><bold>[DEATH GAME] <gray>Results: <red>Die (" + die + ") <gray>vs <light_purple>Be Gay (" + gay + ")");

        if (victim == null) {
            hasVoted.clear();
            return;
        }

        if (gay >= die) {
            Msg.broadcast("<light_purple><bold>[DEATH GAME] <light_purple><bold>" + victim.getName()
                    + " WAS VOTED TO BE GAY!");
            victim.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 600, 0, false));
            playToAll(Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            Msg.title(victim, "<light_purple><bold>YOU ARE NOW GAY!", "<yellow>The server has spoken!", 4, 100, 10);
        } else {
            Msg.broadcast("<red><bold>[DEATH GAME] <red><bold>" + victim.getName() + " WAS VOTED TO DIE!");
            victim.getWorld().strikeLightningEffect(victim.getLocation());
            victim.getWorld().spawnParticle(Particle.FLASH, victim.getLocation(), 1);
            playToAll(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1f);
            victim.setHealth(0.0);
        }

        target = null;
        hasVoted.clear();
    }

    private void playToAll(Sound sound, float volume, float pitch) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }
}