package com.ludvig.pvpkit.combat;

import com.ludvig.pvpkit.PvPKitPlugin;
import com.ludvig.pvpkit.util.Text;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CombatManager {

    private final PvPKitPlugin plugin;
    private final Map<UUID, Long> taggedUntil = new HashMap<>();
    private final Map<UUID, Combo> combos = new HashMap<>();

    private CombatMode mode;
    private BukkitTask attackStrengthTask;
    private boolean attackStrengthWarned;
    private boolean attackStrengthConfirmed;

    public CombatManager(PvPKitPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        this.mode = CombatMode.parse(plugin.getConfig().getString("settings.combat.mode", "1.21.11"));
    }

    public void start() {
        if (attackStrengthTask != null) {
            attackStrengthTask.cancel();
        }
        attackStrengthTask = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, this::enforceNoCooldown, 0L, 1L);
        plugin.getLogger().info("Legacy cooldown hook: " + (AttackStrength.available() ? "ready" : "unavailable"));
    }

    public void stop() {
        if (attackStrengthTask != null) {
            attackStrengthTask.cancel();
            attackStrengthTask = null;
        }
    }

    private void enforceNoCooldown() {
        if (mode.attackCooldown()) {
            return;
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (AttackStrength.fill(player)) {
                if (!attackStrengthConfirmed) {
                    attackStrengthConfirmed = true;
                    plugin.getLogger().info("Legacy mode active: attack cooldown disabled.");
                }
            } else if (!attackStrengthWarned) {
                attackStrengthWarned = true;
                plugin.getLogger().warning("Could not reset attack cooldown; 1.8.9 mode will feel modern.");
            }
        }
    }

    public CombatMode getMode() {
        return mode;
    }

    public void setMode(CombatMode mode) {
        this.mode = mode;
        plugin.getConfig().set("settings.combat.mode", mode.id());
        plugin.saveConfig();

        if (!mode.attackCooldown()) {
            enforceNoCooldown();
        }
    }

    public void handlePlayerHit(Player attacker, Player victim) {
        long tagMillis = Math.max(1, plugin.getConfig().getInt("settings.combat.tag-seconds", 15)) * 1000L;
        long until = System.currentTimeMillis() + tagMillis;
        taggedUntil.put(attacker.getUniqueId(), until);
        taggedUntil.put(victim.getUniqueId(), until);

        Combo combo = combos.computeIfAbsent(attacker.getUniqueId(), ignored -> new Combo());
        long comboWindow = Math.max(1, plugin.getConfig().getInt("settings.combat.combo-seconds", 3)) * 1000L;
        long now = System.currentTimeMillis();
        if (now - combo.lastHit > comboWindow || !victim.getUniqueId().equals(combo.lastVictim)) {
            combo.count = 0;
        }
        combo.count++;
        combo.lastHit = now;
        combo.lastVictim = victim.getUniqueId();

    }

    public int getCombo(Player player) {
        Combo combo = combos.get(player.getUniqueId());
        if (combo == null) {
            return 0;
        }

        long window = Math.max(1, plugin.getConfig().getInt("settings.combat.combo-seconds", 3)) * 1000L;
        if (System.currentTimeMillis() - combo.lastHit > window) {
            return 0;
        }
        return combo.count;
    }

    public boolean isTagged(Player player) {
        Long until = taggedUntil.get(player.getUniqueId());
        return until != null && until > System.currentTimeMillis();
    }

    public long getTagRemaining(Player player) {
        Long until = taggedUntil.get(player.getUniqueId());
        return until == null ? 0L : Math.max(0L, until - System.currentTimeMillis());
    }

    public void clear(Player player) {
        taggedUntil.remove(player.getUniqueId());
        combos.remove(player.getUniqueId());
    }

    public void clearTag(Player player) {
        taggedUntil.remove(player.getUniqueId());
    }

    public void cleanup() {
        long now = System.currentTimeMillis();
        taggedUntil.entrySet().removeIf(entry -> entry.getValue() <= now);
        combos.entrySet().removeIf(entry -> now - entry.getValue().lastHit > 10_000L);
    }

    public String describe() {
        return "&7Mode: " + mode.coloredName()
                + " &8| &7Cooldown: " + (mode.attackCooldown() ? "&aon" : "&coff")
                + " &8| &7Sweep: " + (mode.sweepAttack() ? "&aon" : "&coff")
                + " &8| &7Shields: " + (mode.shields() ? "&aon" : "&coff");
    }

    public String status(Player player) {
        if (!isTagged(player)) {
            return "&aSafe";
        }
        return "&c" + Text.duration(getTagRemaining(player));
    }

    private static final class Combo {
        private int count;
        private long lastHit;
        private UUID lastVictim;
    }
}
