package com.orchidplugins.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class Msg {

    private static MiniMessage mm;
    private static Plugin plugin;

    private Msg() {
    }

    public static void init(Plugin p) {
        plugin = p;
        mm = MiniMessage.miniMessage();
    }

    public static Component parse(String miniString) {
        return mm.deserialize(miniString);
    }

    public static Component legacy(String legacyWithAmpersand) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(legacyWithAmpersand);
    }

    public static void send(CommandSender sender, String miniString) {
        sender.sendMessage(parse(miniString));
    }

    public static void broadcast(String miniString) {
        plugin.getServer().broadcast(parse(miniString));
    }

    public static void actionBar(Player player, String miniString) {
        player.sendActionBar(parse(miniString));
    }

    public static void title(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.showTitle(net.kyori.adventure.title.Title.title(
                parse(title),
                parse(subtitle),
                net.kyori.adventure.title.Title.Times.times(
                        java.time.Duration.ofMillis(fadeIn * 50L),
                        java.time.Duration.ofMillis(stay * 50L),
                        java.time.Duration.ofMillis(fadeOut * 50L))));
    }

    public static void titleAll(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            title(player, title, subtitle, fadeIn, stay, fadeOut);
        }
    }
}