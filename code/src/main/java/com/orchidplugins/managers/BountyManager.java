package com.orchidplugins.managers;

import com.orchidplugins.util.Msg;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BountyManager implements Listener {

    private final Plugin plugin;
    private final ConfigManager config;
    private final WebhookManager webhook;
    private final Map<UUID, Double> bounties = new ConcurrentHashMap<>();
    private Economy economy;
    private final Map<UUID, Double> balances = new ConcurrentHashMap<>();
    private boolean rolling = false;

    public BountyManager(Plugin plugin, ConfigManager config, WebhookManager webhook) {
        this.plugin = plugin;
        this.config = config;
        this.webhook = webhook;
    }

    public void setEconomy(Economy economy) {
        this.economy = economy;
    }

    public boolean isRolling() {
        return rolling;
    }

    public double getBounty(Player player) {
        return bounties.getOrDefault(player.getUniqueId(), 0.0);
    }

    public double getBalance(Player player) {
        if (economy != null) {
            return economy.getBalance(player);
        }
        return balances.getOrDefault(player.getUniqueId(), 0.0);
    }

    public void roll(Player executor) {
        if (rolling) {
            Msg.send(executor, "<red>Please wait for the current bounty roll to finish!");
            return;
        }
        java.util.List<Player> candidates = new java.util.ArrayList<>(plugin.getServer().getOnlinePlayers());
        if (candidates.isEmpty()) {
            Msg.send(executor, "<gray>[Bounty] No online players available to select!");
            return;
        }
        rolling = true;
        plugin.getServer().getLogger().info("[Bounty] Rolling bounty animation started...");

        new AnimationManager(plugin).roll(candidates, "TARGETING...", "<gold><bold>", target -> {
            double min = config.bountyMinAmount();
            double max = config.bountyMaxAmount();
            double amount = min + (Math.random() * (max - min));
            bounties.merge(target.getUniqueId(), amount, Double::sum);
            rolling = false;

            playToAll(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1f);
            Msg.titleAll("<red><bold>BOUNTY PLACED!",
                    "<yellow>" + target.getName() + " <white>has <green>$" + (long) amount
                            + " <white>on their head!", 4, 60, 10);
            Msg.broadcast(config.prefix("bounty") + "<red><bold>" + target.getName()
                    + " <gray>is now targeted with a <green>$" + (long) amount + " <gray>reward!");
            plugin.getServer().getLogger().info("[Bounty] ROLL COMPLETE! Target: " + target.getName()
                    + " | Bounty: $" + (long) amount);
            webhook.sendBountyPlaced(target.getName(), "$" + (long) amount, null);
        });
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (!bounties.containsKey(victim.getUniqueId())) {
            return;
        }
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) {
            if (killer != null) {
                Msg.send(killer, "<red>You cannot claim your own bounty!");
            }
            return;
        }
        double reward = bounties.remove(victim.getUniqueId());
        if (economy != null) {
            economy.depositPlayer(killer, reward);
        } else {
            balances.merge(killer.getUniqueId(), reward, Double::sum);
        }
        Msg.broadcast(config.prefix("bounty") + "<aqua><bold>" + killer.getName()
                + " <gray>claimed the <green>$" + (long) reward + " <gray>bounty on <red><bold>" + victim.getName() + "<gray>!");
        killer.playSound(killer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        webhook.sendBountyClaimed(killer.getName(), victim.getName(), "$" + (long) reward, null);
    }

    public void shutdownScheduler() {
        // nothing to cancel - kept for future use
    }

    private void playToAll(Sound sound, float volume, float pitch) {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            p.playSound(p.getLocation(), sound, volume, pitch);
        }
    }
}