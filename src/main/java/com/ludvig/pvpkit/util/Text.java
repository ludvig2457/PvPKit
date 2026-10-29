package com.ludvig.pvpkit.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.concurrent.TimeUnit;

public final class Text {

    public static final String PREFIX = "&8[&bPvPKit&8] &7";
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private Text() {
    }

    public static Component color(String input) {
        return LEGACY.deserialize(input);
    }

    public static Component prefix(String input) {
        return color(PREFIX + input);
    }

    public static String duration(long millis) {
        if (millis <= 0L) {
            return "0s";
        }

        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        long minutes = seconds / 60L;
        long hours = minutes / 60L;

        if (hours > 0L) {
            return hours + "h " + (minutes % 60L) + "m";
        }
        if (minutes > 0L) {
            return minutes + "m " + (seconds % 60L) + "s";
        }
        return seconds + "s";
    }

    public static String formatDouble(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
