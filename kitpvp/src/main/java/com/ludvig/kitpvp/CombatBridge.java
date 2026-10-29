package com.ludvig.kitpvp;

import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;

public final class CombatBridge {

    private static final String COMBAT_TAG_KEY = "pvpplugin_combat_until";

    private CombatBridge() {
    }

    public static boolean isTagged(Player player) {
        for (MetadataValue value : player.getMetadata(COMBAT_TAG_KEY)) {
            if (value.asLong() > System.currentTimeMillis()) {
                return true;
            }
        }
        return false;
    }
}
