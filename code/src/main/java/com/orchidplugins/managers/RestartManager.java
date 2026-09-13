package com.orchidplugins.managers;

import com.orchidplugins.util.Msg;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class RestartManager {

    private final Plugin plugin;
    private boolean active = false;
    private BukkitTask task;
    private int remaining;

    public RestartManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean isActive() {
        return active;
    }

    public boolean parseTime(String input) {
        if (input.equalsIgnoreCase("cancel")) {
            if (!cancel()) {
                Msg.broadcast("<red>\u274c [SERVER] There is no active restart countdown to cancel.");
                return true;
            }
            return true;
        }
        try {
            return start(Integer.parseInt(input.trim()));
        } catch (NumberFormatException ignored) {
        }
        int total = 0;
        String current = input;
        if (current.contains("h")) {
            String[] parts = current.split("h", 2);
            try {
                total += Integer.parseInt(parts[0].trim()) * 3600;
            } catch (NumberFormatException ignored) {
            }
            current = parts.length > 1 ? parts[1] : "";
        }
        if (current.contains("m")) {
            String[] parts = current.split("m", 2);
            try {
                total += Integer.parseInt(parts[0].trim()) * 60;
            } catch (NumberFormatException ignored) {
            }
            current = parts.length > 1 ? parts[1] : "";
        }
        if (current.contains("s")) {
            try {
                total += Integer.parseInt(current.replace("s", "").trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return total > 0 && start(total);
    }

    public boolean start(int seconds) {
        if (active) {
            return false;
        }
        if (seconds <= 0) {
            return false;
        }
        active = true;
        remaining = seconds;
        Msg.broadcast("<yellow><bold>\u26a0\ufe0f [SERVER] <yellow>Server restart initiated! Restarting in <red>"
                + formatSeconds(seconds) + "<yellow>...");
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
        return true;
    }

    private void tick() {
        if (!active) {
            if (task != null) {
                task.cancel();
            }
            return;
        }
        if (remaining > 0) {
            if (remaining == 1 || remaining == 2 || remaining == 3 || remaining == 4 || remaining == 5
                    || remaining == 10 || remaining == 15 || remaining == 30 || remaining == 60
                    || remaining == 300 || remaining == 600 || remaining == 900 || remaining == 1800
                    || remaining == 3600) {
                Msg.titleAll("<red><bold>\u26a0\ufe0f SERVER RESTART",
                        "<yellow>Restarting in <red>" + formatSeconds(remaining) + "<yellow>!", 4, 40, 10);
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    p.sendActionBar(Msg.parse("<gold>\u26a1 Please find a safe spot!"));
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
                }
            }
            remaining--;
            return;
        }

        Msg.titleAll("<dark_red><bold>\uD83D\uDED1 RESTARTING...", "<gray>See you soon!", 4, 60, 10);
        if (task != null) {
            task.cancel();
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                p.kick(Msg.parse("<red>\u26a0 <bold>[SERVER] Server is restarting...</bold>\n<gray>Please rejoin in a few moments!"));
            }
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                active = false;
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "restart");
            }, 20L);
        }, 40L);
    }

    public boolean cancel() {
        if (!active) {
            return false;
        }
        active = false;
        if (task != null) {
            task.cancel();
        }
        Msg.broadcast("<green><bold>\u2714 [SERVER] <green>Server restart has been canceled!");
        Msg.titleAll("<green><bold>RESTART CANCELED", "<gray>The server will remain online.", 4, 60, 10);
        return true;
    }

    private String formatSeconds(int seconds) {
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        int s = seconds % 60;
        StringBuilder sb = new StringBuilder();
        if (h > 0) {
            sb.append(h).append("h ");
        }
        if (m > 0) {
            sb.append(m).append("m ");
        }
        if (s > 0) {
            sb.append(s).append("s");
        } else if (sb.length() == 0) {
            sb.append("0s");
        }
        return sb.toString().trim();
    }
}