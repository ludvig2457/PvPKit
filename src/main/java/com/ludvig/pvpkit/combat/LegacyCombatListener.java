package com.ludvig.pvpkit.combat;

import com.ludvig.pvpkit.PvPKitPlugin;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;

public final class LegacyCombatListener implements Listener {

    private final LegacyCombatManager legacy;

    public LegacyCombatListener(PvPKitPlugin plugin) {
        this.legacy = plugin.getLegacyCombatManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!legacy.isLegacy() || !(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (isUnblockable(event.getCause()) || !legacy.isBlocking(victim)) {
            return;
        }
        event.setDamage(legacy.blockedDamage(event.getDamage()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!legacy.isLegacy() || event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            return;
        }
        if (!(event.getDamager() instanceof Player attacker)
                || !(event.getEntity() instanceof LivingEntity victim)
                || attacker == victim) {
            return;
        }

        boolean sprinting = attacker.isSprinting();
        Vector before = victim.getVelocity().clone();
        legacy.applyKnockback(attacker, victim, before, sprinting);
        if (sprinting) {
            attacker.setSprinting(false);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!legacy.isLegacy()) {
            return;
        }

        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            legacy.stopBlocking(event.getPlayer());
            return;
        }
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getItem() == null || !event.getItem().getType().name().endsWith("_SWORD")) {
            return;
        }
        legacy.startBlocking(event.getPlayer());
    }

    @EventHandler
    public void onSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() == PlayerAnimationType.ARM_SWING) {
            legacy.stopBlocking(event.getPlayer());
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        legacy.stopBlocking(event.getPlayer());
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        legacy.stopBlocking(event.getPlayer());
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (event.getItemDrop().getItemStack().getType() == Material.AIR) {
            return;
        }
        legacy.stopBlocking(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        legacy.stopBlocking(event.getPlayer());
    }

    private boolean isUnblockable(EntityDamageEvent.DamageCause cause) {
        return switch (cause) {
            case KILL, VOID, STARVATION, SUICIDE, CUSTOM, WORLD_BORDER, DRAGON_BREATH -> true;
            default -> false;
        };
    }
}
