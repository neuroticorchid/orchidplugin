package com.orchidplugins.tpa;

import com.orchidplugins.managers.ConfigManager;
import com.orchidplugins.util.Msg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class TpaMsg {

    private TpaMsg() {
    }

    public static void send(ConfigManager config, CommandSender sender, String content) {
        Msg.send(sender, config.tpaMessage(content));
    }

    public static void sendFriend(ConfigManager config, CommandSender sender, String content) {
        Msg.send(sender, config.friendMessage(content));
    }

    public static Component requestMessage(ConfigManager config, String requesterName, Request.Type type) {
        String label = type == Request.Type.TPA
                ? "requests to teleport to you"
                : "wants you to teleport to them";
        String tag = type == Request.Type.TPA ? "TPA" : "TPAHERE";
        Component accept = Msg.parse("<green><bold>[ACCEPT]</bold></green>")
                .clickEvent(ClickEvent.runCommand("/tpaaccept " + requesterName))
                .hoverEvent(HoverEvent.showText(Msg.parse("<green>Click to accept")));
        Component deny = Msg.parse("<red><bold>[DENY]</bold></red>")
                .clickEvent(ClickEvent.runCommand("/tpadeny " + requesterName))
                .hoverEvent(HoverEvent.showText(Msg.parse("<red>Click to deny")));
        return Msg.parse("<yellow>" + requesterName + " <reset><white>" + label + " <gray>(" + tag + ")  ")
                .append(accept)
                .append(Msg.parse("  "))
                .append(deny);
    }

    public static Component friendRequestMessage(ConfigManager config, String requesterName) {
        Component accept = Msg.parse("<green><bold>[ACCEPT]</bold></green>")
                .clickEvent(ClickEvent.runCommand("/faccept " + requesterName))
                .hoverEvent(HoverEvent.showText(Msg.parse("<green>Click to accept")));
        Component deny = Msg.parse("<red><bold>[DENY]</bold></red>")
                .clickEvent(ClickEvent.runCommand("/fdeny " + requesterName))
                .hoverEvent(HoverEvent.showText(Msg.parse("<red>Click to deny")));
        return Msg.parse("<aqua>" + requesterName + " <reset><yellow>wants to be friends!  ")
                .append(accept)
                .append(Msg.parse("  "))
                .append(deny);
    }

    public static String tptrapWarning(String suspectName) {
        return "<red><bold>TPTRAP WARNING</bold> <white>» <reset><gray>Teleport requests can be used to lure you into a trap. "
                + "<red>TPTrap is NOT allowed on this server.<reset> <gray>If you die within 2 minutes of arriving, "
                + "<yellow>" + suspectName + " <gray>will be automatically reported to staff.";
    }

    public static void staffReport(Plugin plugin, String miniString) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.hasPermission("orchidtpa.staff")) {
                Msg.send(player, miniString);
            }
        }
        plugin.getLogger().warning("[TPA REPORT] "
                + net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(Msg.parse(miniString)));
    }

    public static String report(String victimName, String suspectName, String windowSeconds, String location) {
        return "<red><bold>[TPA REPORT]</bold> <white>» <reset><yellow>" + victimName + " <gray>died <red>"
                + location + "<gray>, " + windowSeconds + " <gray>after teleporting to <yellow>" + suspectName
                + "<gray>. <red>Possible teleport trap <gray>(reported automatically).";
    }
}