package com.ludvig.pvpkit.sidebar;

import com.ludvig.pvpkit.PvPKitPlugin;
import com.ludvig.pvpkit.combat.CombatManager;
import com.ludvig.pvpkit.stats.PlayerStats;
import com.ludvig.pvpkit.stats.StatsManager;
import com.ludvig.pvpkit.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class SidebarManager implements Listener {

    private static final String OBJECTIVE_NAME = "pvpkit_sidebar";
    private static final String[] ENTRIES = {"§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7", "§8"};

    private final PvPKitPlugin plugin;
    private final NamespacedKey sidebarKey;
    private final Map<UUID, Boolean> enabled = new HashMap<>();
    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    private BukkitTask task;

    public SidebarManager(PvPKitPlugin plugin) {
        this.plugin = plugin;
        this.sidebarKey = new NamespacedKey(plugin, "sidebar_enabled");
    }

    public void start() {
        stop();

        for (Player player : Bukkit.getOnlinePlayers()) {
            loadPlayer(player);
        }

        int interval = Math.max(5, plugin.getConfig().getInt("settings.scoreboard.update-interval-ticks", 20));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, interval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }

        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(main);
        }
        boards.clear();
    }

    public void loadPlayer(Player player) {
        Byte stored = player.getPersistentDataContainer().get(sidebarKey, PersistentDataType.BYTE);
        boolean defaultValue = plugin.getConfig().getBoolean("settings.scoreboard.default-enabled", true);

        boolean value;
        if (stored == null) {
            value = defaultValue;
            player.getPersistentDataContainer().set(sidebarKey, PersistentDataType.BYTE, (byte) (value ? 1 : 0));
        } else {
            value = stored != 0;
        }

        enabled.put(player.getUniqueId(), value);
        if (value) {
            render(player);
        }
    }

    public void unloadPlayer(Player player) {
        enabled.remove(player.getUniqueId());
        boards.remove(player.getUniqueId());
    }

    public boolean isEnabled(Player player) {
        return enabled.getOrDefault(player.getUniqueId(),
                plugin.getConfig().getBoolean("settings.scoreboard.default-enabled", true));
    }

    public boolean toggle(Player player) {
        boolean value = !isEnabled(player);
        enabled.put(player.getUniqueId(), value);
        player.getPersistentDataContainer().set(sidebarKey, PersistentDataType.BYTE, (byte) (value ? 1 : 0));

        if (value) {
            render(player);
            player.sendMessage(Text.prefix("&aScoreboard enabled."));
        } else {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            boards.remove(player.getUniqueId());
            player.sendMessage(Text.prefix("&cScoreboard disabled."));
        }
        return value;
    }

    private void tick() {
        plugin.getCombatManager().cleanup();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isEnabled(player)) {
                render(player);
            }
        }
    }

    private void render(Player player) {
        Scoreboard board = boards.get(player.getUniqueId());
        if (board == null) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective objective = board.registerNewObjective(OBJECTIVE_NAME, "dummy",
                    Text.color("&b&lPvPKit"));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);

            for (int index = 0; index < ENTRIES.length; index++) {
                Team team = board.registerNewTeam("pvpkit_line_" + index);
                team.addEntry(ENTRIES[index]);
                objective.getScore(ENTRIES[index]).setScore(ENTRIES.length - index);
            }

            boards.put(player.getUniqueId(), board);
            player.setScoreboard(board);
        }

        Objective objective = board.getObjective(OBJECTIVE_NAME);
        if (objective == null) {
            boards.remove(player.getUniqueId());
            render(player);
            return;
        }

        objective.displayName(Text.color("&b&lPvPKit &7| " + plugin.getCombatManager().getMode().coloredName()));

        List<Component> lines = buildLines(player);
        for (int index = 0; index < ENTRIES.length; index++) {
            Team team = board.getTeam("pvpkit_line_" + index);
            if (team == null) {
                continue;
            }
            team.prefix(index < lines.size() ? lines.get(index) : Component.empty());
        }
    }

    private List<Component> buildLines(Player player) {
        StatsManager statsManager = plugin.getStatsManager();
        CombatManager combatManager = plugin.getCombatManager();
        PlayerStats stats = statsManager.getOrCreate(player);
        String kit = plugin.getKitManager().getLastKit(player);

        List<Component> lines = new ArrayList<>();
        lines.add(Text.color("&7Kit: &f" + kit));
        lines.add(Text.color("&7Kills: &a" + stats.kills() + " &8| &7Deaths: &c" + stats.deaths()));
        lines.add(Text.color("&7KDR: &e" + Text.formatDouble(stats.kdr())));
        lines.add(Text.color("&7Streak: &6" + stats.streak() + " &8| &7Best: &6" + stats.bestStreak()));
        lines.add(Text.color("&7Combat: " + combatManager.status(player)));
        lines.add(Text.color("&7Combo: &e" + combatManager.getCombo(player)));
        lines.add(Text.color("&7Online: &f" + Bukkit.getOnlinePlayers().size()
                + " &8| &7Ping: &f" + player.getPing() + "ms"));
        lines.add(Text.color("&7HUD: &f/hud &8| &7Kits: &f/kit"));
        lines.add(Text.color("&8github.com/ludvig2457"));
        return lines;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        loadPlayer(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        unloadPlayer(event.getPlayer());
    }

    public String statusText(Player player) {
        return isEnabled(player) ? "&aenabled" : "&cdisabled";
    }

    public String modeId() {
        return plugin.getCombatManager().getMode().id().toLowerCase(Locale.ROOT);
    }
}
