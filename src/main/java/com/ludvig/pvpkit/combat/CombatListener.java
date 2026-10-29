package com.ludvig.pvpkit.combat;

import com.ludvig.pvpkit.PvPKitPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class CombatListener implements Listener {

    private final CombatManager combatManager;

    public CombatListener(PvPKitPlugin plugin) {
        this.combatManager = plugin.getCombatManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK
                && !combatManager.getMode().sweepAttack()) {
            event.setCancelled(true);
            return;
        }

        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        Entity damaged = event.getEntity();
        if (!(damaged instanceof LivingEntity) || damaged == attacker) {
            return;
        }

        if (damaged instanceof Player victim) {
            combatManager.handlePlayerHit(attacker, victim);
        }

    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (combatManager.getMode().shields()) {
            return;
        }
        if (event.getItem() == null || event.getItem().getType() != Material.SHIELD) {
            return;
        }
        Action action = event.getAction();
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        combatManager.clear(event.getPlayer());
    }
}
