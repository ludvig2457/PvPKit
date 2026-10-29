package com.ludvig.pvpkit.combat;

import com.ludvig.pvpkit.PvPKitPlugin;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class LegacyCombatManager {

    private final PvPKitPlugin plugin;
    private final NamespacedKey slowKey;
    private final Set<UUID> blocking = new HashSet<>();

    private BukkitTask task;

    public LegacyCombatManager(PvPKitPlugin plugin) {
        this.plugin = plugin;
        this.slowKey = new NamespacedKey(plugin, "legacy_sword_block");
    }

    public void start() {
        stop();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        stopAllBlocks();
    }

    public boolean isLegacy() {
        return plugin.getCombatManager().getMode() == CombatMode.LEGACY_1_8_9;
    }

    public boolean isBlocking(Player player) {
        return isLegacy()
                && blocking.contains(player.getUniqueId())
                && isSword(player.getInventory().getItemInMainHand());
    }

    public boolean startBlocking(Player player) {
        if (!isLegacy() || !isSword(player.getInventory().getItemInMainHand())) {
            return false;
        }
        if (!blocking.add(player.getUniqueId())) {
            return false;
        }
        applySlow(player);
        return true;
    }

    public void stopBlocking(Player player) {
        if (blocking.remove(player.getUniqueId())) {
            removeSlow(player);
        }
    }

    public double blockedDamage(double damage) {
        double multiplier = clamp(plugin.getConfig().getDouble(
                "settings.combat.legacy.blocking-damage-multiplier", 0.5), 0.0, 1.0);
        double flat = Math.max(0.0, plugin.getConfig().getDouble(
                "settings.combat.legacy.blocking-damage-flat", 0.5));
        return damage * multiplier + flat;
    }

    public void applyKnockback(Player attacker, LivingEntity victim, Vector before, boolean sprinting) {
        AttributeInstance resistanceAttribute = victim.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        double resistance = resistanceAttribute == null ? 0.0 : resistanceAttribute.getValue();
        if (resistance >= 1.0 || (resistance > 0.0 && Math.random() < resistance)) {
            return;
        }

        double horizontal = plugin.getConfig().getDouble("settings.combat.legacy.knockback-horizontal", 0.4);
        double sprintHorizontal = plugin.getConfig().getDouble("settings.combat.legacy.knockback-sprint-horizontal", 0.9);
        double vertical = plugin.getConfig().getDouble("settings.combat.legacy.knockback-vertical", 0.4);
        double force = sprinting ? sprintHorizontal : horizontal;

        double yaw = Math.toRadians(attacker.getLocation().getYaw());
        double x = before.getX() / 2.0 - (-Math.sin(yaw)) * force;
        double z = before.getZ() / 2.0 - Math.cos(yaw) * force;
        double y = before.getY() / 2.0 + vertical;
        if (y > vertical) {
            y = vertical;
        }

        Vector velocity = new Vector(x, y, z);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (victim.isValid() && !victim.isDead()) {
                victim.setVelocity(velocity);
            }
        });
    }

    private void tick() {
        if (!isLegacy()) {
            if (!blocking.isEmpty()) {
                stopAllBlocks();
            }
            return;
        }

        for (UUID id : new ArrayList<>(blocking)) {
            Player player = Bukkit.getPlayer(id);
            if (player == null || !player.isOnline() || player.isDead()
                    || !isSword(player.getInventory().getItemInMainHand())) {
                if (player != null) {
                    stopBlocking(player);
                } else {
                    blocking.remove(id);
                }
            }
        }
    }

    private void stopAllBlocks() {
        for (UUID id : new ArrayList<>(blocking)) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                removeSlow(player);
            }
        }
        blocking.clear();
    }

    private void applySlow(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attribute == null) {
            return;
        }
        attribute.removeModifier(slowKey);
        double multiplier = clamp(plugin.getConfig().getDouble(
                "settings.combat.legacy.blocking-speed-multiplier", 0.2), 0.05, 1.0);
        attribute.addModifier(new AttributeModifier(
                slowKey, multiplier - 1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
    }

    private void removeSlow(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attribute != null) {
            attribute.removeModifier(slowKey);
        }
    }

    private boolean isSword(ItemStack item) {
        return item != null && item.getType().name().endsWith("_SWORD");
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
