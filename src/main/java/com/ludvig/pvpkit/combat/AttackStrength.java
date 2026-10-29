package com.ludvig.pvpkit.combat;

import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

final class AttackStrength {

    private static final Field TICKER;
    private static Method getHandle;

    static {
        Field field = null;
        try {
            Class<?> livingEntity = Class.forName("net.minecraft.world.entity.LivingEntity");
            field = livingEntity.getDeclaredField("attackStrengthTicker");
            field.setAccessible(true);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        TICKER = field;
    }

    private AttackStrength() {
    }

    static boolean available() {
        return TICKER != null;
    }

    static boolean fill(Player player) {
        if (TICKER == null) {
            return false;
        }
        try {
            if (getHandle == null) {
                getHandle = player.getClass().getMethod("getHandle");
            }
            Object handle = getHandle.invoke(player);
            TICKER.setInt(handle, 100);
            return true;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }
}
