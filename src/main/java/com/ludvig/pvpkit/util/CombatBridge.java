package com.ludvig.pvpkit.util;

import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;

public final class CombatBridge {

    public static final String TAG_KEY = "pvpplugin_combat_until";
    public static final String COMBO_KEY = "combatplus_combo";
    public static final String COMBO_UNTIL_KEY = "combatplus_combo_until";
    public static final String MODE_KEY = "combatplus_mode";

    private CombatBridge() {
    }

    public static long tagUntil(Player player) {
        for (MetadataValue value : player.getMetadata(TAG_KEY)) {
            return value.asLong();
        }
        return 0L;
    }

    public static boolean isTagged(Player player) {
        return tagUntil(player) > System.currentTimeMillis();
    }

    public static long tagRemaining(Player player) {
        return Math.max(0L, tagUntil(player) - System.currentTimeMillis());
    }

    public static String status(Player player) {
        if (!isTagged(player)) {
            return "&aSafe";
        }
        return "&c" + Text.duration(tagRemaining(player));
    }

    public static int combo(Player player) {
        long until = 0L;
        for (MetadataValue value : player.getMetadata(COMBO_UNTIL_KEY)) {
            until = value.asLong();
            break;
        }
        if (until <= System.currentTimeMillis()) {
            return 0;
        }

        for (MetadataValue value : player.getMetadata(COMBO_KEY)) {
            return value.asInt();
        }
        return 0;
    }

    public static String mode(Player player) {
        for (MetadataValue value : player.getMetadata(MODE_KEY)) {
            return value.asString();
        }
        return "—";
    }

    public static void clearTag(Player player) {
        org.bukkit.plugin.Plugin combat = org.bukkit.Bukkit.getPluginManager().getPlugin("CombatPlusPlus");
        if (combat == null) {
            return;
        }
        player.removeMetadata(TAG_KEY, combat);
        player.removeMetadata(COMBO_KEY, combat);
        player.removeMetadata(COMBO_UNTIL_KEY, combat);
    }
}
