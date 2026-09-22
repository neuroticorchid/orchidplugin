package com.orchidplugins.listeners;

import com.orchidplugins.tpa.RequestManager;
import com.orchidplugins.tpa.TrapReportManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class TpListener implements Listener {

    private final RequestManager requestManager;
    private final TrapReportManager trapReport;

    public TpListener(RequestManager requestManager, TrapReportManager trapReport) {
        this.requestManager = requestManager;
        this.trapReport = trapReport;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        trapReport.checkDeath(event.getEntity());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        requestManager.removeAll(event.getPlayer().getUniqueId());
        trapReport.remove(event.getPlayer().getUniqueId());
    }
}