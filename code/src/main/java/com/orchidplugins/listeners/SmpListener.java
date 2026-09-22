package com.orchidplugins.listeners;

import com.orchidplugins.managers.SmpManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class SmpListener implements Listener {

    private final SmpManager smpManager;

    public SmpListener(SmpManager smpManager) {
        this.smpManager = smpManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        smpManager.onJoin(event.getPlayer());
    }
}