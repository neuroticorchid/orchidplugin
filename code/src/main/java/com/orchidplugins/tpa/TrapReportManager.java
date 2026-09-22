package com.orchidplugins.tpa;

import com.orchidplugins.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class TrapReportManager {

    private final long windowMillis;
    private final Plugin plugin;
    private final Map<UUID, TrackedTeleport> teleports = new HashMap<>();

    private static class TrackedTeleport {
        final UUID suspect;
        final long since;
        final String suspectName;

        TrackedTeleport(UUID suspect, String suspectName) {
            this.suspect = suspect;
            this.suspectName = suspectName;
            this.since = System.currentTimeMillis();
        }
    }

    public TrapReportManager(Plugin plugin, long windowMillis) {
        this.plugin = plugin;
        this.windowMillis = windowMillis;
    }

    public void track(UUID teleported, UUID suspect, String suspectName) {
        teleports.put(teleported, new TrackedTeleport(suspect, suspectName));
    }

    public void checkDeath(Player victim) {
        TrackedTeleport tracked = teleports.remove(victim.getUniqueId());
        if (tracked == null) {
            return;
        }
        long elapsed = System.currentTimeMillis() - tracked.since;
        if (elapsed > windowMillis) {
            return;
        }
        double seconds = elapsed / 1000.0;
        String window = "%.0f second%s".formatted(Math.floor(seconds), seconds < 2.0 ? "" : "s");
        TpaMsg.staffReport(plugin, TpaMsg.report(
                victim.getName(), tracked.suspectName, window, TpaUtil.location(victim.getLocation())));
    }

    public void purgeExpired() {
        long cutoff = System.currentTimeMillis() - windowMillis;
        Iterator<Map.Entry<UUID, TrackedTeleport>> it = teleports.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().since < cutoff) {
                it.remove();
            }
        }
    }

    public void remove(UUID teleported) {
        teleports.remove(teleported);
    }
}