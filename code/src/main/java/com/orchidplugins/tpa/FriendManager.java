package com.orchidplugins.tpa;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class FriendManager {

    private static class FriendRequest {
        final UUID requester;
        final long expiresAt;

        FriendRequest(UUID requester, long ttlMillis) {
            this.requester = requester;
            this.expiresAt = System.currentTimeMillis() + ttlMillis;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    private final Map<UUID, FriendRequest> requests = new HashMap<>();
    private final long ttlMillis;

    public FriendManager(long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }

    public boolean hasPending(UUID target) {
        FriendRequest pending = requests.get(target);
        return pending != null && !pending.isExpired();
    }

    public void send(UUID target, UUID requester) {
        requests.put(target, new FriendRequest(requester, ttlMillis));
    }

    public boolean isRequester(UUID target, UUID requester) {
        FriendRequest pending = requests.get(target);
        return pending != null && !pending.isExpired() && pending.requester.equals(requester);
    }

    public Optional<UUID> requesterOf(UUID target) {
        FriendRequest pending = requests.get(target);
        if (pending == null || pending.isExpired()) {
            return Optional.empty();
        }
        return Optional.of(pending.requester);
    }

    public void cancel(UUID target) {
        requests.remove(target);
    }

    public void cleanupExpired(java.util.function.BiConsumer<UUID, UUID> onExpire) {
        Iterator<Map.Entry<UUID, FriendRequest>> it = requests.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, FriendRequest> entry = it.next();
            if (entry.getValue().isExpired()) {
                it.remove();
                if (onExpire != null) {
                    Player requester = Bukkit.getPlayer(entry.getValue().requester);
                    if (requester != null) {
                        onExpire.accept(requester.getUniqueId(), entry.getKey());
                    }
                }
            }
        }
    }
}