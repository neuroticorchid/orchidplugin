package com.orchidplugins.listeners;

import com.orchidplugins.managers.ModerationManager;
import com.orchidplugins.util.Msg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.List;
import java.util.regex.Pattern;

public final class ModerationListener implements Listener {

    private static final Pattern AMPERSAND = Pattern.compile("&");

    private final ModerationManager moderationManager;

    public ModerationListener(ModerationManager moderationManager) {
        this.moderationManager = moderationManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(PlayerLoginEvent event) {
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            return;
        }
        String name = event.getPlayer().getName();
        if (Bukkit.getBanList(BanList.Type.NAME).isBanned(name)) {
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, buildBanKick(name));
        }
        if (event.getAddress() != null && Bukkit.getBanList(BanList.Type.IP).isBanned(event.getAddress().getHostAddress())) {
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, MiniMessage.miniMessage().deserialize("<red>Your IP is banned."));
        }
    }

    private Component buildBanKick(String name) {
        org.bukkit.BanEntry entry = Bukkit.getBanList(BanList.Type.NAME).getBanEntry(name);
        if (entry == null) {
            return MiniMessage.miniMessage().deserialize("<red>You are banned.");
        }
        Component reason = entry.getReason() != null && !entry.getReason().isEmpty()
                ? MiniMessage.miniMessage().deserialize("<red>Banned: <white>" + entry.getReason())
                : MiniMessage.miniMessage().deserialize("<red>You are banned.");
        Component expiry = Component.empty();
        if (entry.getExpiration() != null) {
            long left = (entry.getExpiration().getTime() - System.currentTimeMillis()) / 1000L;
            if (left > 0) {
                expiry = MiniMessage.miniMessage().deserialize("\n<gray>Expires in: <yellow>" + com.orchidplugins.util.TimeParser.format(left));
            }
        }
        return reason.append(expiry);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        moderationManager.recordIp(event.getPlayer());
        moderationManager.purgeExpiredWarnings(event.getPlayer().getUniqueId());
        List<String> queued = moderationManager.drainOfflineMessages(event.getPlayer().getUniqueId());
        for (String message : queued) {
            Msg.send(event.getPlayer(), message);
        }
    }
}