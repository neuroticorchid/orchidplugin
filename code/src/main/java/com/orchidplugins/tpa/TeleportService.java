package com.orchidplugins.tpa;

import com.orchidplugins.managers.ConfigManager;
import com.orchidplugins.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class TeleportService {

    private final Plugin plugin;
    private final ConfigManager config;
    private final TrapReportManager trapReport;

    public TeleportService(Plugin plugin, ConfigManager config, TrapReportManager trapReport) {
        this.plugin = plugin;
        this.config = config;
        this.trapReport = trapReport;
    }

    public void begin(Player teleporter, java.util.UUID destinationUuid,
                      String destinationName, Request.Type type) {
        if (teleporter.getUniqueId().equals(destinationUuid)) {
            TpaMsg.send(config, teleporter, "<red>You cannot teleport to yourself.");
            return;
        }
        int countdown = config.tpaCountdownSeconds();
        teleporter.sendMessage(Msg.parse(TpaMsg.tptrapWarning(destinationName)));
        TpaMsg.send(config, teleporter, type == Request.Type.TPA
                ? "<green>Teleporting to <yellow>" + destinationName
                        + " <green>in <yellow>" + countdown + " <green>seconds..."
                : "<green>Teleporting <yellow>" + destinationName
                        + " <green>to you in <yellow>" + countdown + " <green>seconds...");

        int[] remaining = {countdown};
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!teleporter.isOnline()) {
                    cancel();
                    return;
                }
                if (remaining[0] > 0) {
                    teleporter.sendActionBar(Msg.parse("<dark_purple><bold>" + remaining[0]
                            + "</bold></dark_purple> <gray>... teleporting to <white>" + destinationName));
                    TpaAnimationManager.countdownTick(teleporter, remaining[0], countdown);
                    remaining[0]--;
                    return;
                }
                Player destination = Bukkit.getPlayer(destinationUuid);
                if (destination == null || !destination.isOnline()) {
                    TpaMsg.send(config, teleporter, "<red>" + destinationName + " went offline. Teleport cancelled.");
                    cancel();
                    return;
                }
                teleporter.teleport(destination.getLocation());
                TpaAnimationManager.arrive(teleporter);
                trapReport.track(teleporter.getUniqueId(), destinationUuid, destinationName);
                if (type == Request.Type.TPA) {
                    TpaMsg.send(config, teleporter, "<green>You teleported to <yellow>" + destinationName
                            + "<green>.");
                    TpaMsg.send(config, destination, "<yellow>" + teleporter.getName()
                            + " <green>was teleported to you. <gray>(TPTrap is not allowed!)");
                } else {
                    TpaMsg.send(config, teleporter, "<yellow>" + destinationName
                            + " <green>has been teleported to you.");
                    TpaMsg.send(config, destination, "<green>You teleported to <yellow>" + teleporter.getName()
                            + "<green>. <gray>(TPTrap is not allowed!)");
                }
                cancel();
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}