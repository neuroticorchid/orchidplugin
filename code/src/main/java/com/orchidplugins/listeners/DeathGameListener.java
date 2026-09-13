package com.orchidplugins.listeners;

import com.orchidplugins.managers.DeathGameManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public final class DeathGameListener implements Listener {

    private final DeathGameManager deathGameManager;

    public DeathGameListener(DeathGameManager deathGameManager) {
        this.deathGameManager = deathGameManager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.hasChangedPosition()) {
            deathGameManager.onMove(event.getPlayer(), event.getFrom());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        deathGameManager.onJoin(event.getPlayer());
    }
}