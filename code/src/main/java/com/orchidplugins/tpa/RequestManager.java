package com.orchidplugins.tpa;

import com.orchidplugins.managers.ConfigManager;
import com.orchidplugins.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class RequestManager {

    private final ConfigManager config;
    private final Map<UUID, List<Request>> requests = new HashMap<>();

    public RequestManager(ConfigManager config) {
        this.config = config;
    }

    public void add(Request request) {
        requests.computeIfAbsent(request.getTarget(), k -> new ArrayList<>()).add(request);
    }

    public Optional<Request> find(UUID target, UUID requester) {
        for (Request request : requests.getOrDefault(target, List.of())) {
            if (request.getRequester().equals(requester)) {
                return Optional.of(request);
            }
        }
        return Optional.empty();
    }

    public Optional<Request> find(UUID target, UUID requester, Request.Type type) {
        for (Request request : requests.getOrDefault(target, List.of())) {
            if (request.getRequester().equals(requester) && request.getType() == type) {
                return Optional.of(request);
            }
        }
        return Optional.empty();
    }

    public List<Request> forTarget(UUID target) {
        return requests.getOrDefault(target, List.of());
    }

    public Optional<Request> first(UUID target) {
        List<Request> list = requests.get(target);
        return list == null || list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public void cancel(Request request) {
        List<Request> list = requests.get(request.getTarget());
        if (list != null) {
            list.remove(request);
            if (list.isEmpty()) {
                requests.remove(request.getTarget());
            }
        }
    }

    public void cleanupExpired() {
        Iterator<Map.Entry<UUID, List<Request>>> it = requests.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, List<Request>> entry = it.next();
            entry.getValue().removeIf(request -> {
                if (!request.isExpired()) {
                    return false;
                }
                Player requester = Bukkit.getPlayer(request.getRequester());
                if (requester != null) {
                    Msg.send(requester, config.tpaMessage("<red>Your "
                            + (request.getType() == Request.Type.TPA ? "TPA" : "TPAHERE")
                            + " request to <yellow>" + TpaUtil.nameOf(request.getTarget())
                            + " <red>expired."));
                }
                return true;
            });
            if (entry.getValue().isEmpty()) {
                it.remove();
            }
        }
    }

    public void removeAll(UUID uuid) {
        requests.remove(uuid);
        for (List<Request> list : requests.values()) {
            list.removeIf(request -> request.getRequester().equals(uuid));
        }
        requests.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
}