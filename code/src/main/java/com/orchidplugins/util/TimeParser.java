package com.orchidplugins.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeParser {

    private static final Pattern TOKEN = Pattern.compile("(\\d+)([wdhms])");

    private TimeParser() {
    }

    /** Parses a duration string into seconds, or -1 if invalid. Supports 0/perm/permanent, raw seconds, and w/d/h/m/s combos. */
    public static long parseSeconds(String input) {
        if (input == null) {
            return -1;
        }
        String s = input.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) {
            return -1;
        }
        if (s.equals("0") || s.equals("perm") || s.equals("permanent")) {
            return 0;
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException ignored) {
        }
        long total = 0;
        Matcher matcher = TOKEN.matcher(s);
        boolean matched = false;
        while (matcher.find()) {
            matched = true;
            long value;
            try {
                value = Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                return -1;
            }
            switch (matcher.group(2)) {
                case "w" -> total += value * 604800L;
                case "d" -> total += value * 86400L;
                case "h" -> total += value * 3600L;
                case "m" -> total += value * 60L;
                case "s" -> total += value;
            }
        }
        return matched ? total : -1;
    }

    /** Scans args (skipping index 0, the player name) for the first token that parses as a duration and splits it out. */
    public static Parsed extractDuration(String[] args) {
        if (args.length <= 1) {
            return new Parsed(-1, "");
        }
        int durationIndex = -1;
        long seconds = -1;
        for (int i = 1; i < args.length; i++) {
            long secs = parseSeconds(args[i]);
            if (secs != -1) {
                durationIndex = i;
                seconds = secs;
                break;
            }
        }
        if (durationIndex == -1) {
            return new Parsed(-1, String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)));
        }
        StringBuilder reason = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i == durationIndex) {
                continue;
            }
            if (reason.length() > 0) {
                reason.append(' ');
            }
            reason.append(args[i]);
        }
        return new Parsed(seconds, reason.toString());
    }

    /** Formats seconds into a compact duration like 2w, 1d6h, 45m, 30s. */
    public static String format(long seconds) {
        if (seconds <= 0) {
            return "permanently";
        }
        StringBuilder sb = new StringBuilder();
        long weeks = seconds / 604800L;
        long days = (seconds % 604800L) / 86400L;
        long hours = (seconds % 86400L) / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long secs = seconds % 60L;
        if (weeks > 0) {
            sb.append(weeks).append('w');
        }
        if (days > 0) {
            sb.append(days).append('d');
        }
        if (hours > 0) {
            sb.append(hours).append('h');
        }
        if (minutes > 0) {
            sb.append(minutes).append('m');
        }
        if (secs > 0 || sb.length() == 0) {
            sb.append(secs).append('s');
        }
        return sb.toString();
    }

    public record Parsed(long seconds, String reason) {
    }
}