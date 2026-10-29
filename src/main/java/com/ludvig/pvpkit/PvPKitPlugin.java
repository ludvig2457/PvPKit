package com.ludvig.pvpkit;

import com.ludvig.pvpkit.combat.CombatCommand;
import com.ludvig.pvpkit.combat.CombatListener;
import com.ludvig.pvpkit.combat.CombatManager;
import com.ludvig.pvpkit.hud.HudCommand;
import com.ludvig.pvpkit.hud.HudListener;
import com.ludvig.pvpkit.hud.HudManager;
import com.ludvig.pvpkit.kit.KitCommand;
import com.ludvig.pvpkit.kit.KitGui;
import com.ludvig.pvpkit.kit.KitManager;
import com.ludvig.pvpkit.report.ReportCommand;
import com.ludvig.pvpkit.report.ReportGui;
import com.ludvig.pvpkit.report.ReportManager;
import com.ludvig.pvpkit.report.ReportsCommand;
import com.ludvig.pvpkit.settings.SettingsCommand;
import com.ludvig.pvpkit.settings.SettingsGui;
import com.ludvig.pvpkit.sidebar.SidebarCommand;
import com.ludvig.pvpkit.sidebar.SidebarManager;
import com.ludvig.pvpkit.spawn.SpawnCommand;
import com.ludvig.pvpkit.stats.StatsCommand;
import com.ludvig.pvpkit.stats.StatsManager;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public final class PvPKitPlugin extends JavaPlugin {

    private HudManager hudManager;
    private KitManager kitManager;
    private ReportManager reportManager;
    private StatsManager statsManager;
    private CombatManager combatManager;
    private SidebarManager sidebarManager;
    private KitGui kitGui;
    private ReportGui reportGui;
    private SettingsGui settingsGui;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.statsManager = new StatsManager(this);
        this.statsManager.load();
        this.combatManager = new CombatManager(this);
        this.kitManager = new KitManager(this);
        this.kitManager.load();
        this.reportManager = new ReportManager(this);
        this.reportManager.load();
        this.hudManager = new HudManager(this);
        this.sidebarManager = new SidebarManager(this);
        this.kitGui = new KitGui(this);
        this.reportGui = new ReportGui(this);
        this.settingsGui = new SettingsGui(this);

        this.hudManager.start();
        this.sidebarManager.start();
        this.combatManager.start();

        registerListener(new HudListener(this));
        registerListener(new CombatListener(this));
        registerListener(statsManager);
        registerListener(sidebarManager);
        registerListener(kitGui);
        registerListener(reportGui);
        registerListener(settingsGui);

        register("hud", new HudCommand(this));
        register("kit", new KitCommand(this));
        register("report", new ReportCommand(this));
        register("reports", new ReportsCommand(this));
        register("pvpkit", new PvPKitCommand(this));
        register("combat", new CombatCommand(this));
        register("stats", new StatsCommand(this));
        register("scoreboard", new SidebarCommand(this));
        register("settings", new SettingsCommand(this));
        register("spawn", new SpawnCommand(this));

        getLogger().info("PvPKit enabled: HUD, kits, reports, stats, sidebar and combat modes are ready.");
    }

    @Override
    public void onDisable() {
        if (hudManager != null) {
            hudManager.stop();
        }
        if (sidebarManager != null) {
            sidebarManager.stop();
        }
        if (combatManager != null) {
            combatManager.stop();
        }
        if (kitManager != null) {
            kitManager.save();
        }
        if (reportManager != null) {
            reportManager.save();
        }
        if (statsManager != null) {
            statsManager.save();
        }
    }

    private void registerListener(org.bukkit.event.Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }

    private void register(String name, Object executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command '" + name + "' is missing from plugin.yml");
            return;
        }
        command.setExecutor((CommandExecutor) executor);
        if (executor instanceof TabCompleter completer) {
            command.setTabCompleter(completer);
        }
    }

    public void reloadAll() {
        reloadConfig();
        combatManager.load();
        kitManager.load();
        reportManager.load();
        hudManager.start();
        sidebarManager.start();
    }

    public HudManager getHudManager() {
        return hudManager;
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public ReportManager getReportManager() {
        return reportManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public SidebarManager getSidebarManager() {
        return sidebarManager;
    }

    public KitGui getKitGui() {
        return kitGui;
    }

    public ReportGui getReportGui() {
        return reportGui;
    }

    public SettingsGui getSettingsGui() {
        return settingsGui;
    }
}
