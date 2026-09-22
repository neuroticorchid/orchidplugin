package com.orchidplugins.tpa;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class TpaUtil {

    private TpaUtil() {
    }

    public static UUID resolve(String name) {
        Player online = getOnlinePlayer(name);
        if (online != null) {
            return online.getUniqueId();
        }
        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            String offlineName = offline.getName();
            if (offlineName != null && offlineName.equalsIgnoreCase(name)) {
                return offline.getUniqueId();
            }
        }
        return null;
    }

    public static Player getOnlinePlayer(String name) {
        Player exact = Bukkit.getPlayerExact(name);
        if (exact != null) {
            return exact;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(name)) {
                return player;
            }
        }
        return null;
    }

    public static String nameOf(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            return player.getName();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        if (offline != null && offline.getName() != null) {
            return offline.getName();
        }
        return uuid.toString();
    }

    public static String location(org.bukkit.Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return "unknown location";
        }
        return "%s (%s, %s, %s)".formatted(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}