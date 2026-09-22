package com.orchidplugins.managers;

import com.orchidplugins.tpa.Request;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TpaManager {

    /** Per-player settings. tpa/tpahere are per-player overrides; null = use global default. */
    public static class PlayerSettings {
        private Boolean tpa;
        private Boolean tpahere;
        private boolean autoTpa;
        private boolean autoTpaHere;
        private final Set<UUID> blockedTpa = new HashSet<>();
        private final Set<UUID> blockedTpaHere = new HashSet<>();
        private final Set<UUID> friends = new HashSet<>();

        public Boolean getTpa() {
            return tpa;
        }

        public void setTpa(Boolean tpa) {
            this.tpa = tpa;
        }

        public Boolean getTpaHere() {
            return tpahere;
        }

        public void setTpaHere(Boolean tpahere) {
            this.tpahere = tpahere;
        }

        public boolean isAutoTpa() {
            return autoTpa;
        }

        public void setAutoTpa(boolean autoTpa) {
            this.autoTpa = autoTpa;
        }

        public boolean isAutoTpaHere() {
            return autoTpaHere;
        }

        public void setAutoTpaHere(boolean autoTpaHere) {
            this.autoTpaHere = autoTpaHere;
        }

        public boolean isAuto(Request.Type type) {
            return type == Request.Type.TPA ? autoTpa : autoTpaHere;
        }

        public boolean isBlocked(UUID requester, Request.Type type) {
            return (type == Request.Type.TPA ? blockedTpa : blockedTpaHere).contains(requester);
        }

        public void setBlocked(UUID requester, Request.Type type, boolean blocked) {
            Set<UUID> list = type == Request.Type.TPA ? blockedTpa : blockedTpaHere;
            if (blocked) {
                list.add(requester);
            } else {
                list.remove(requester);
            }
        }

        public Set<UUID> getBlocked(Request.Type type) {
            return type == Request.Type.TPA ? blockedTpa : blockedTpaHere;
        }

        public boolean isFriend(UUID uuid) {
            return friends.contains(uuid);
        }

        public void addFriend(UUID uuid) {
            friends.add(uuid);
        }

        public boolean removeFriend(UUID uuid) {
            return friends.remove(uuid);
        }

        public Set<UUID> getFriends() {
            return friends;
        }
    }

    private final Map<UUID, PlayerSettings> players = new HashMap<>();
    private boolean globalTpa = true;
    private boolean globalTpaHere = true;
    private final Plugin plugin;
    private File file;

    public TpaManager(Plugin plugin) {
        this.plugin = plugin;
        load();
    }

    public PlayerSettings get(UUID uuid) {
        return players.computeIfAbsent(uuid, k -> new PlayerSettings());
    }

    public boolean isGlobalOn(Request.Type type) {
        return type == Request.Type.TPA ? globalTpa : globalTpaHere;
    }

    public void setGlobal(Request.Type type, boolean on) {
        if (type == Request.Type.TPA) {
            globalTpa = on;
        } else {
            globalTpaHere = on;
        }
    }

    /** Effective accept state for a player: friend bypass > per-type block > override > global. */
    public boolean accepts(UUID target, UUID requester, Request.Type type) {
        PlayerSettings settings = get(target);
        if (settings.isFriend(requester)) {
            return true;
        }
        if (settings.isBlocked(requester, type)) {
            return false;
        }
        Boolean override = type == Request.Type.TPA ? settings.getTpa() : settings.getTpaHere();
        return override != null ? override : isGlobalOn(type);
    }

    public boolean isFriend(UUID owner, UUID other) {
        return owner.equals(other) || get(owner).isFriend(other);
    }

    public List<UUID> getFriends(UUID uuid) {
        return new ArrayList<>(get(uuid).friends);
    }

    /** Adds a mutual friendship between two players. */
    public void addFriendship(UUID a, UUID b) {
        get(a).addFriend(b);
        get(b).addFriend(a);
    }

    /** Removes a mutual friendship. */
    public void removeFriendship(UUID a, UUID b) {
        get(a).removeFriend(b);
        get(b).removeFriend(a);
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "toggles.yml");
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (config.contains("global.tpa")) {
            globalTpa = config.getBoolean("global.tpa");
        }
        if (config.contains("global.tpahere")) {
            globalTpaHere = config.getBoolean("global.tpahere");
        }
        for (String key : config.getKeys(false)) {
            if (key.equals("global")) {
                continue;
            }
            try {
                UUID uuid = UUID.fromString(key);
                PlayerSettings settings = players.computeIfAbsent(uuid, k -> new PlayerSettings());
                if (config.contains(key + ".tpa")) {
                    settings.setTpa(config.getBoolean(key + ".tpa"));
                }
                if (config.contains(key + ".tpahere")) {
                    settings.setTpaHere(config.getBoolean(key + ".tpahere"));
                }
                if (config.contains(key + ".auto-tpa")) {
                    settings.setAutoTpa(config.getBoolean(key + ".auto-tpa"));
                }
                if (config.contains(key + ".auto-tpahere")) {
                    settings.setAutoTpaHere(config.getBoolean(key + ".auto-tpahere"));
                }
                settings.getBlocked(Request.Type.TPA).addAll(uuidList(config.getStringList(key + ".blocked-tpa")));
                settings.getBlocked(Request.Type.TPAHERE).addAll(uuidList(config.getStringList(key + ".blocked-tpahere")));
                settings.getFriends().addAll(uuidList(config.getStringList(key + ".friends")));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save() {
        if (file == null) {
            file = new File(plugin.getDataFolder(), "toggles.yml");
        }
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        YamlConfiguration config = new YamlConfiguration();
        config.set("global.tpa", globalTpa);
        config.set("global.tpahere", globalTpaHere);
        for (Map.Entry<UUID, PlayerSettings> entry : players.entrySet()) {
            PlayerSettings settings = entry.getValue();
            if (settings.getTpa() == null
                    && settings.getTpaHere() == null
                    && !settings.isAutoTpa()
                    && !settings.isAutoTpaHere()
                    && settings.getBlocked(Request.Type.TPA).isEmpty()
                    && settings.getBlocked(Request.Type.TPAHERE).isEmpty()
                    && settings.getFriends().isEmpty()) {
                continue;
            }
            String path = entry.getKey().toString();
            if (settings.getTpa() != null) {
                config.set(path + ".tpa", settings.getTpa());
            }
            if (settings.getTpaHere() != null) {
                config.set(path + ".tpahere", settings.getTpaHere());
            }
            config.set(path + ".auto-tpa", settings.isAutoTpa());
            config.set(path + ".auto-tpahere", settings.isAutoTpaHere());
            config.set(path + ".blocked-tpa", uuids(settings.getBlocked(Request.Type.TPA)));
            config.set(path + ".blocked-tpahere", uuids(settings.getBlocked(Request.Type.TPAHERE)));
            config.set(path + ".friends", uuids(settings.getFriends()));
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save toggles.yml: " + e.getMessage());
        }
    }

    private List<String> uuids(Set<UUID> set) {
        List<String> out = new ArrayList<>();
        for (UUID uuid : set) {
            out.add(uuid.toString());
        }
        return out;
    }

    private List<UUID> uuidList(List<String> strings) {
        List<UUID> result = new ArrayList<>();
        for (String value : strings) {
            try {
                result.add(UUID.fromString(value));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return result;
    }
}