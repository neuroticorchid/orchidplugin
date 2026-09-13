package com.orchidplugins.listeners;

import com.orchidplugins.managers.GiveawayManager;
import com.orchidplugins.managers.PollManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public final class ChatListener implements Listener {

    private final GiveawayManager giveawayManager;
    private final PollManager pollManager;

    public ChatListener(GiveawayManager giveawayManager, PollManager pollManager) {
        this.giveawayManager = giveawayManager;
        this.pollManager = pollManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onChat(AsyncPlayerChatEvent event) {
        if (pollManager.castVote(event.getPlayer(), event.getMessage())) {
            event.setCancelled(true);
            return;
        }
        if (giveawayManager.isOpen()) {
            if (giveawayManager.submitEntry(event.getPlayer(), event.getMessage())) {
                event.setCancelled(true);
            }
        }
    }
}