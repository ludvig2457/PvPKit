package com.ludvig.pvpkit.hud;

import com.ludvig.pvpkit.PvPKitPlugin;
import com.ludvig.pvpkit.SchedulerCompat;
import com.ludvig.pvpkit.stats.PlayerStats;
import com.ludvig.pvpkit.util.CombatBridge;
import com.ludvig.pvpkit.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public final class HudManager {

    private static final long CPS_WINDOW_MS = 1_000L;
    private static final long MOVEMENT_WINDOW_NANOS = 300_000_000L;

    private final PvPKitPlugin plugin;
    private final NamespacedKey hudKey;
    private final Map<UUID, PlayerState> states = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastLines = new ConcurrentHashMap<>();
    private final Set<UUID> disabled = ConcurrentHashMap.newKeySet();

    private Object task;

    public HudManager(PvPKitPlugin plugin) {
        this.plugin = plugin;
        this.hudKey = new NamespacedKey(plugin, "hud_enabled");
    }

    public void start() {
        stop();

        for (Player player : Bukkit.getOnlinePlayers()) {
            loadPlayer(player);
        }

        int interval = Math.max(1, plugin.getConfig().getInt("settings.hud.update-interval-ticks", 2));
        task = SchedulerCompat.repeatForPlayers(plugin, 20L, interval, this::tickPlayer);
    }

    public void stop() {
        SchedulerCompat.cancel(task);
        task = null;
    }

    public void loadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        Byte stored = player.getPersistentDataContainer().get(hudKey, PersistentDataType.BYTE);

        if (stored == null) {
            boolean enabled = plugin.getConfig().getBoolean("settings.hud.default-enabled", true);
            player.getPersistentDataContainer().set(hudKey, PersistentDataType.BYTE, (byte) (enabled ? 1 : 0));
            updateDisabled(uuid, enabled);
        } else {
            updateDisabled(uuid, stored != 0);
        }

        states.putIfAbsent(uuid, new PlayerState());
        lastLines.remove(uuid);
    }

    public void unloadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        states.remove(uuid);
        lastLines.remove(uuid);
    }

    public boolean toggle(Player player) {
        boolean enabled = disabled.contains(player.getUniqueId());
        updateDisabled(player.getUniqueId(), enabled);
        player.getPersistentDataContainer().set(hudKey, PersistentDataType.BYTE, (byte) (enabled ? 1 : 0));

        if (enabled) {
            player.sendMessage(Text.prefix("&aHUD enabled."));
            lastLines.remove(player.getUniqueId());
        } else {
            player.sendMessage(Text.prefix("&cHUD disabled."));
            player.sendActionBar(Component.empty());
        }
        return enabled;
    }

    public boolean isEnabled(Player player) {
        return !disabled.contains(player.getUniqueId());
    }

    private void updateDisabled(UUID uuid, boolean enabled) {
        if (enabled) {
            disabled.remove(uuid);
        } else {
            disabled.add(uuid);
        }
    }

    public void registerLeftClick(Player player) {
        if (!isEnabled(player)) {
            return;
        }
        PlayerState state = states.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerState());
        state.leftClicks.addLast(System.currentTimeMillis());
        prune(state.leftClicks, System.currentTimeMillis());
    }

    public void registerRightClick(Player player) {
        if (!isEnabled(player)) {
            return;
        }
        PlayerState state = states.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerState());
        state.rightClicks.addLast(System.currentTimeMillis());
        prune(state.rightClicks, System.currentTimeMillis());
    }

    public void onMove(Player player, Location from, Location to) {
        PlayerState state = states.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerState());

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();

        if (dx * dx + dz * dz < 1.0E-6 || dx * dx + dz * dz > 100.0) {
            return;
        }

        double yaw = Math.toRadians(to.getYaw());
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = -forwardZ;
        double rightZ = forwardX;

        double forward = dx * forwardX + dz * forwardZ;
        double right = dx * rightX + dz * rightZ;

        state.forward = forward > 1.0E-4;
        state.backward = forward < -1.0E-4;
        state.right = right > 1.0E-4;
        state.left = right < -1.0E-4;
        state.lastMoveNanos = System.nanoTime();
    }

    private void tickPlayer(Player player) {
        if (!isEnabled(player)) {
            return;
        }

        long now = System.currentTimeMillis();
        long nowNanos = System.nanoTime();
        PlayerState state = states.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerState());
        String line = render(player, state, now, nowNanos);

        if (!line.equals(lastLines.get(player.getUniqueId()))) {
            lastLines.put(player.getUniqueId(), line);
            player.sendActionBar(Text.color(line));
        }
    }

    private String render(Player player, PlayerState state, long now, long nowNanos) {
        int leftCps = pruneAndCount(state.leftClicks, now);
        int rightCps = pruneAndCount(state.rightClicks, now);

        boolean moving = nowNanos - state.lastMoveNanos <= MOVEMENT_WINDOW_NANOS;
        boolean jumping = player.getVelocity().getY() > 0.12
                && !player.isFlying()
                && !player.isGliding()
                && !player.isSwimming();

        List<String> parts = new ArrayList<>();

        if (plugin.getConfig().getBoolean("settings.hud.show-cps", true)) {
            parts.add("&7CPS &f" + leftCps + "&7/&f" + rightCps);
        }

        int combo = CombatBridge.combo(player);
        if (plugin.getConfig().getBoolean("settings.hud.show-combo", true) && combo > 1) {
            parts.add("&7Combo &e" + combo);
        }

        if (plugin.getConfig().getBoolean("settings.hud.show-armor", true)) {
            ItemStack[] armor = player.getInventory().getArmorContents();
            String helmet = armorValue(armor.length > 3 ? armor[3] : null);
            String chestplate = armorValue(armor.length > 2 ? armor[2] : null);
            String leggings = armorValue(armor.length > 1 ? armor[1] : null);
            String boots = armorValue(armor.length > 0 ? armor[0] : null);
            parts.add("&7Armor " + helmet + "&7/" + chestplate + "&7/" + leggings + "&7/" + boots);
        }

        if (plugin.getConfig().getBoolean("settings.hud.show-keys", true)) {
            parts.add("&7Keys " + key(state.forward && moving, "W")
                    + key(state.left && moving, "A")
                    + key(state.backward && moving, "S")
                    + key(state.right && moving, "D")
                    + " " + key(jumping, "SPACE")
                    + " " + key(player.isSneaking(), "SHIFT"));
        }

        PlayerStats stats = plugin.getStatsManager().getOrCreate(player);
        if (plugin.getConfig().getBoolean("settings.hud.show-streak", true)) {
            parts.add("&7Streak &6" + stats.streak());
        }

        if (plugin.getConfig().getBoolean("settings.hud.show-combat", true)) {
            parts.add("&7Combat " + CombatBridge.status(player));
        }

        if (plugin.getConfig().getBoolean("settings.hud.show-ping", true)) {
            parts.add("&7Ping &f" + player.getPing() + "ms");
        }

        if (parts.isEmpty()) {
            return "&8[&bPvP&8] &7HUD";
        }
        return "&8[&bPvP&8] " + String.join(" &8| ", parts);
    }

    private String key(boolean active, String label) {
        return (active ? "&a&l" : "&8") + label + "&r";
    }

    private String armorValue(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return "&8-";
        }

        short maxDurability = item.getType().getMaxDurability();
        if (maxDurability <= 0) {
            return "&a100";
        }

        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable)) {
            return "&a100";
        }

        int remaining = Math.max(0, maxDurability - damageable.getDamage());
        int percent = Math.max(0, Math.min(100, Math.round(remaining * 100.0F / maxDurability)));

        String color = percent >= 70 ? "&a" : percent >= 35 ? "&e" : "&c";
        return color + percent;
    }

    private int pruneAndCount(Deque<Long> clicks, long now) {
        prune(clicks, now);
        return clicks.size();
    }

    private void prune(Deque<Long> clicks, long now) {
        while (!clicks.isEmpty() && now - clicks.peekFirst() > CPS_WINDOW_MS) {
            clicks.removeFirst();
        }
    }

    private static final class PlayerState {
        private final Deque<Long> leftClicks = new ArrayDeque<>();
        private final Deque<Long> rightClicks = new ArrayDeque<>();
        private boolean forward;
        private boolean backward;
        private boolean left;
        private boolean right;
        private long lastMoveNanos;
    }
}
