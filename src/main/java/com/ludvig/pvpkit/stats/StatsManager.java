package com.ludvig.pvpkit.stats;

import com.ludvig.pvpkit.PvPKitPlugin;
import com.ludvig.pvpkit.util.CombatBridge;
import com.ludvig.pvpkit.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class StatsManager implements Listener {

    private final PvPKitPlugin plugin;
    private final Map<UUID, PlayerStats> stats = new HashMap<>();
    private final File statsFile;

    public StatsManager(PvPKitPlugin plugin) {
        this.plugin = plugin;
        this.statsFile = new File(plugin.getDataFolder(), "stats.yml");
    }

    public void load() {
        stats.clear();
        if (!statsFile.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(statsFile);
        ConfigurationSection root = yaml.getConfigurationSection("players");
        if (root == null) {
            return;
        }

        for (String uuidText : root.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidText);
                ConfigurationSection section = root.getConfigurationSection(uuidText);
                if (section == null) {
                    continue;
                }

                PlayerStats playerStats = new PlayerStats(uuid, section.getString("name", "unknown"));
                playerStats.kills(section.getInt("kills"));
                playerStats.deaths(section.getInt("deaths"));
                playerStats.bestStreak(section.getInt("best-streak"));
                playerStats.streak(section.getInt("streak"));
                stats.put(uuid, playerStats);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Skipping invalid stats UUID: " + uuidText);
            }
        }

        plugin.getLogger().info("Loaded stats for " + stats.size() + " player(s).");
    }

    public PlayerStats getOrCreate(Player player) {
        return getOrCreate(player.getUniqueId(), player.getName());
    }

    public PlayerStats getOrCreate(OfflinePlayer player) {
        return getOrCreate(player.getUniqueId(), player.getName());
    }

    public PlayerStats getOrCreate(UUID uuid, String name) {
        return stats.computeIfAbsent(uuid, ignored -> new PlayerStats(uuid, name == null ? "unknown" : name));
    }

    public PlayerStats get(UUID uuid) {
        return stats.get(uuid);
    }

    public PlayerStats addKill(Player killer, Player victim) {
        PlayerStats killerStats = getOrCreate(killer);
        PlayerStats victimStats = getOrCreate(victim);
        killerStats.name(killer.getName());
        victimStats.name(victim.getName());
        killerStats.addKill();
        victimStats.addDeath();
        save();
        return killerStats;
    }

    public void addDeath(Player victim) {
        PlayerStats victimStats = getOrCreate(victim);
        victimStats.name(victim.getName());
        victimStats.addDeath();
        save();
    }

    public void reset(UUID uuid) {
        PlayerStats playerStats = stats.get(uuid);
        if (playerStats != null) {
            playerStats.kills(0);
            playerStats.deaths(0);
            playerStats.streak(0);
            playerStats.bestStreak(0);
            save();
        }
    }

    public List<PlayerStats> top(int limit) {
        List<PlayerStats> sorted = new ArrayList<>(stats.values());
        sorted.sort(Comparator.comparingInt((PlayerStats value) -> value.kills()).reversed()
                .thenComparing(Comparator.comparingDouble((PlayerStats value) -> value.kdr()).reversed()));
        return sorted.subList(0, Math.min(limit, sorted.size()));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerStats playerStats = getOrCreate(player);
        playerStats.name(player.getName());
        save();
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null && !killer.getUniqueId().equals(victim.getUniqueId())) {
            PlayerStats killerStats = addKill(killer, victim);
            CombatBridge.clearTag(victim);

            if (plugin.getConfig().getBoolean("settings.stats.kill-feed", true)) {
                Component feed = Text.color("&8[&c&lKILL&8] &f" + killer.getName()
                        + " &7killed &f" + victim.getName() + " &8(&7streak &6"
                        + killerStats.streak() + "&8)");
                Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(feed));
            }

            int streak = killerStats.streak();
            if (plugin.getConfig().getBoolean("settings.stats.killstreak-announce", true)
                    && (streak == 5 || streak == 10 || streak == 15 || streak == 20
                    || streak == 25 || streak == 30 || streak == 40 || streak == 50)) {
                Bukkit.broadcast(Text.color("&8[&6&lSTREAK&8] &f" + killer.getName()
                        + " &7is on a &6" + streak + " &7kill streak!"));
            }
            return;
        }

        addDeath(victim);
        CombatBridge.clearTag(victim);
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();

        for (PlayerStats playerStats : stats.values()) {
            String path = "players." + playerStats.uuid();
            yaml.set(path + ".name", playerStats.name());
            yaml.set(path + ".kills", playerStats.kills());
            yaml.set(path + ".deaths", playerStats.deaths());
            yaml.set(path + ".streak", playerStats.streak());
            yaml.set(path + ".best-streak", playerStats.bestStreak());
        }

        try {
            File parent = statsFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("Could not create plugin data folder");
            }
            yaml.save(statsFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save stats.yml: " + exception.getMessage());
        }
    }
}
