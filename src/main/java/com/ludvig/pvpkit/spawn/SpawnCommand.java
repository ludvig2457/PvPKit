package com.ludvig.pvpkit.spawn;

import com.ludvig.pvpkit.PvPKitPlugin;
import com.ludvig.pvpkit.util.Text;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SpawnCommand implements CommandExecutor {

    private final PvPKitPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public SpawnCommand(PvPKitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.prefix("&cOnly players can teleport to spawn."));
            return true;
        }

        if (plugin.getConfig().getBoolean("settings.spawn.block-in-combat", true)
                && plugin.getCombatManager().isTagged(player)) {
            player.sendMessage(Text.prefix("&cYou cannot teleport to spawn while in combat for &f"
                    + Text.duration(plugin.getCombatManager().getTagRemaining(player)) + "&c."));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8F, 1.0F);
            return true;
        }

        int cooldownSeconds = Math.max(0, plugin.getConfig().getInt("settings.spawn.cooldown-seconds", 10));
        long now = System.currentTimeMillis();
        long until = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (until > now && !player.hasPermission("pvpplugin.spawn.bypass")) {
            player.sendMessage(Text.prefix("&cPlease wait &f" + Text.duration(until - now) + " &cbefore using /spawn."));
            return true;
        }

        Location spawn = player.getWorld().getSpawnLocation();
        player.teleport(spawn);
        player.setFallDistance(0.0F);
        player.playSound(spawn, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8F, 1.3F);
        player.sendMessage(Text.prefix("&aTeleported to spawn."));

        if (cooldownSeconds > 0 && !player.hasPermission("pvpplugin.spawn.bypass")) {
            cooldowns.put(player.getUniqueId(), now + cooldownSeconds * 1000L);
        }
        return true;
    }
}
