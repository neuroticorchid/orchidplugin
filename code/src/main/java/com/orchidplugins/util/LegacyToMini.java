package com.orchidplugins.util;

import java.util.Locale;

/**
 * Minimal legacy (&amp;0-9, &amp;a-f, &amp;k-r, &amp;#RRGGBB) to MiniMessage converter so
 * config values like "&amp;l&amp;bCLOUDREND SMP" keep their Minecraft formatting in-game.
 */
public final class LegacyToMini {

    private LegacyToMini() {
    }

    public static String convert(String legacy) {
        if (legacy == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < legacy.length()) {
            char c = legacy.charAt(i);
            if (c == '&' && i + 1 < legacy.length()) {
                char next = legacy.charAt(i + 1);
                if (next == '#' && i + 8 <= legacy.length()) {
                    String hex = legacy.substring(i + 2, i + 8);
                    if (hex.matches("[0-9a-fA-F]{6}")) {
                        out.append("<#").append(hex.toLowerCase(Locale.ROOT)).append('>');
                        i += 8;
                        continue;
                    }
                } else {
                    String tag = tagFor(next);
                    if (tag != null) {
                        out.append('<').append(tag).append('>');
                        i += 2;
                        continue;
                    }
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    private static String tagFor(char code) {
        switch (Character.toLowerCase(code)) {
            case '0': return "black";
            case '1': return "dark_blue";
            case '2': return "dark_green";
            case '3': return "dark_aqua";
            case '4': return "dark_red";
            case '5': return "dark_purple";
            case '6': return "gold";
            case '7': return "gray";
            case '8': return "dark_gray";
            case '9': return "blue";
            case 'a': return "green";
            case 'b': return "aqua";
            case 'c': return "red";
            case 'd': return "light_purple";
            case 'e': return "yellow";
            case 'f': return "white";
            case 'k': return "obfuscated";
            case 'l': return "bold";
            case 'm': return "strikethrough";
            case 'n': return "underlined";
            case 'o': return "italic";
            case 'r': return "reset";
            default: return null;
        }
    }
}