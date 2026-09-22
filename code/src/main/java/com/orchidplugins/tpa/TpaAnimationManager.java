package com.orchidplugins.tpa;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Vanilla particle + effect burst around a teleporting player, escalating toward arrival. */
public final class TpaAnimationManager {

    private TpaAnimationManager() {
    }

    public static void countdownTick(Player player, int remaining, int total) {
        if (player == null || !player.isOnline()) {
            return;
        }
        int stage = total - remaining;
        Particle particle = switch (stage) {
            case 0, 1 -> Particle.SMOKE;
            case 2, 3 -> Particle.PORTAL;
            default -> Particle.DRAGON_BREATH;
        };
        int count = remaining <= 2 ? 24 : 12;
        player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), count, 0.4, 0.7, 0.4, 0.02, 1.0f);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.4f);
    }

    public static void arrive(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, player.getLocation().add(0, 1, 0), 40, 0.4, 0.7, 0.4, 0.05);
        player.getWorld().spawnParticle(Particle.FLASH, player.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0, Color.WHITE);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 80, 0, false, false, true));
    }
}