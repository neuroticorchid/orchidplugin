package com.orchidplugins.managers;

import com.google.gson.Gson;
import com.orchidplugins.util.Msg;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class WebhookManager {

    public static final String WARN = "warn";
    public static final String BAN = "ban";
    public static final String BANIP = "banip";
    public static final String UNBAN = "unban";
    public static final String UNWARN = "unwarn";
    public static final String SUSPEND = "suspendstaff";
    public static final String UNSUSPEND = "unsuspendstaff";
    public static final String BOUNTY = "bounty";
    public static final String ABUSE = "admin-abuse";

    private static final List<String> EVENTS = List.of(
            WARN, BAN, BANIP, UNBAN, UNWARN, SUSPEND, UNSUSPEND, BOUNTY, ABUSE);

    private static final int COLOR_WARN = 0xE67E22;
    private static final int COLOR_BAN = 0xE74C3C;
    private static final int COLOR_IPBAN = 0x8E44AD;
    private static final int COLOR_UNBAN = 0x2ECC71;
    private static final int COLOR_UNWARN = 0x2ECC71;
    private static final int COLOR_SUSPEND = 0xE67E22;
    private static final int COLOR_RESTORE = 0x2ECC71;
    private static final int COLOR_BOUNTY = 0xF1C40F;
    private static final int COLOR_ABUSE = 0x9B59B6;

    private final Plugin plugin;
    private final HttpClient client = HttpClient.newBuilder().build();
    private final Gson gson = new Gson();
    private final Map<String, String> eventUrls = new ConcurrentHashMap<>();
    private final Map<String, Boolean> eventEnabled = new ConcurrentHashMap<>();
    private boolean enabled;
    private String defaultUrl;

    public WebhookManager(Plugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        reload(config);
    }

    public void reload(FileConfiguration config) {
        enabled = config.getBoolean("discord-webhook.enabled", false);
        defaultUrl = config.getString("discord-webhook.default-url", "");
        eventUrls.clear();
        eventEnabled.clear();
        for (String event : EVENTS) {
            eventUrls.put(event, config.getString("discord-webhook.events." + event + ".url", ""));
            eventEnabled.put(event, config.getBoolean("discord-webhook.events." + event + ".enabled", true));
        }
    }

    public void send(String event, String title, int color, Map<String, String> fields, String content) {
        if (!enabled || !Boolean.TRUE.equals(eventEnabled.getOrDefault(event, false))) {
            return;
        }
        String url = eventUrls.getOrDefault(event, "");
        if (url == null || url.isBlank()) {
            url = defaultUrl;
        }
        if (url.isBlank()) {
            plugin.getLogger().warning("Webhook event '" + event + "' is enabled but has no URL configured.");
            return;
        }
        post(url, title, color, fields, content, event);
    }

    public void sendTest(CommandSender sender) {
        if (!enabled) {
            Msg.send(sender, "<red>Webhooks are disabled (discord-webhook.enabled).");
            return;
        }
        if (defaultUrl == null || defaultUrl.isBlank()) {
            Msg.send(sender, "<red>Set <white>discord-webhook.default-url<red> before testing.");
            return;
        }
        post(defaultUrl, "OrchidPlugins Webhook Test", COLOR_RESTORE,
                Map.of("Plugin", "OrchidPlugins v" + plugin.getPluginMeta().getVersion(),
                        "Status", "Connected", "Time", Instant.now().toString()),
                null, "test");
        Msg.send(sender, "<green>Test embed sent to the default URL.");
    }

    // Convenience helpers used by commands/managers.

    public void sendWarn(String player, String staff, String reason, String duration, String content) {
        send(WARN, "Player Warned", COLOR_WARN,
                fields("Player", player, "Staff", staff, "Reason", reason, "Duration", duration), content);
    }

    public void sendBan(String player, String staff, String reason, String duration, String content) {
        send(BAN, "Player Banned", COLOR_BAN,
                fields("Player", player, "Staff", staff, "Reason", reason, "Duration", duration), content);
    }

    public void sendBanIp(String player, String staff, String reason, String duration, String content) {
        send(BANIP, "Player IP Banned", COLOR_IPBAN,
                fields("Player", player, "Staff", staff, "Reason", reason, "Duration", duration), content);
    }

    public void sendUnban(String player, String staff, String content) {
        send(UNBAN, "Player Unbanned", COLOR_UNBAN, fields("Player", player, "Staff", staff), content);
    }

    public void sendUnwarn(String player, String staff, String removed, String reason, String content) {
        send(UNWARN, "Warnings Removed", COLOR_UNWARN,
                fields("Player", player, "Staff", staff, "Removed", removed, "Reason", reason), content);
    }

    public void sendSuspend(String player, String staff, String reason, String duration, String content) {
        send(SUSPEND, "Staff Suspended", COLOR_SUSPEND,
                fields("Player", player, "Staff", staff, "Reason", reason, "Duration", duration), content);
    }

    public void sendRestore(String player, String reason, String content) {
        send(UNSUSPEND, "Staff Restored", COLOR_RESTORE,
                fields("Player", player, "Reason", reason), content);
    }

    public void sendBountyPlaced(String target, String amount, String content) {
        send(BOUNTY, "Bounty Placed", COLOR_BOUNTY, fields("Target", target, "Reward", amount), content);
    }

    public void sendBountyClaimed(String killer, String victim, String amount, String content) {
        send(BOUNTY, "Bounty Claimed", COLOR_BOUNTY,
                fields("Killer", killer, "Victim", victim, "Reward", amount), content);
    }

    public void sendAbuse(String executor, String target, String action, String detail, boolean publicView, String content) {
        send(ABUSE, "Admin Abuse Executed", COLOR_ABUSE,
                fields("Executor", executor, "Target", target, "Action", action,
                        "Detail", detail, "Visibility", publicView ? "public" : "private"), content);
    }

    private Map<String, String> fields(String... kv) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            map.put(kv[i], kv[i + 1] == null || kv[i + 1].isEmpty() ? "-" : kv[i + 1]);
        }
        return map;
    }

    private void post(String url, String title, int color, Map<String, String> fields,
                      String content, String event) {
        Map<String, Object> embed = new LinkedHashMap<>();
        embed.put("title", title);
        embed.put("color", color);
        embed.put("timestamp", Instant.now().toString());
        List<Map<String, Object>> fieldList = new ArrayList<>();
        for (Map.Entry<String, String> e : fields.entrySet()) {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("name", e.getKey());
            f.put("value", e.getValue());
            f.put("inline", true);
            fieldList.add(f);
        }
        embed.put("fields", fieldList);
        Map<String, Object> payload = new LinkedHashMap<>();
        if (content != null && !content.isEmpty()) {
            payload.put("content", content);
        }
        payload.put("embeds", List.of(embed));

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                .build();
        client.sendAsync(request, HttpResponse.BodyHandlers.discarding()).whenComplete((resp, err) -> {
            if (err != null) {
                plugin.getLogger().warning("Webhook '" + event + "' failed: " + err.getMessage());
            } else if (resp.statusCode() >= 300) {
                plugin.getLogger().warning("Webhook '" + event + "' returned HTTP " + resp.statusCode());
            }
        });
    }
}