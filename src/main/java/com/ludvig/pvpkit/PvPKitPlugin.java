package com.ludvig.pvpkit;

import com.ludvig.pvpkit.hud.HudCommand;
import com.ludvig.pvpkit.hud.HudListener;
import com.ludvig.pvpkit.hud.HudManager;
import com.ludvig.pvpkit.settings.SettingsCommand;
import com.ludvig.pvpkit.settings.SettingsGui;
import com.ludvig.pvpkit.sidebar.SidebarCommand;
import com.ludvig.pvpkit.sidebar.SidebarManager;
import com.ludvig.pvpkit.stats.StatsCommand;
import com.ludvig.pvpkit.stats.StatsManager;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public final class PvPKitPlugin extends JavaPlugin {

    private HudManager hudManager;
    private StatsManager statsManager;
    private SidebarManager sidebarManager;
    private SettingsGui settingsGui;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.statsManager = new StatsManager(this);
        this.statsManager.load();
        this.hudManager = new HudManager(this);
        this.sidebarManager = new SidebarManager(this);
        this.settingsGui = new SettingsGui(this);

        this.hudManager.start();
        this.sidebarManager.start();

        registerListener(new HudListener(this));
        registerListener(statsManager);
        registerListener(sidebarManager);
        registerListener(settingsGui);

        register("hud", new HudCommand(this));
        register("pvpkit", new PvPKitCommand(this));
        register("stats", new StatsCommand(this));
        register("scoreboard", new SidebarCommand(this));
        register("settings", new SettingsCommand(this));

        getLogger().info("PvPKit core enabled: HUD, stats, sidebar and settings are ready.");
    }

    @Override
    public void onDisable() {
        if (hudManager != null) {
            hudManager.stop();
        }
        if (sidebarManager != null) {
            sidebarManager.stop();
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
        hudManager.start();
        sidebarManager.start();
    }

    public HudManager getHudManager() {
        return hudManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public SidebarManager getSidebarManager() {
        return sidebarManager;
    }

    public SettingsGui getSettingsGui() {
        return settingsGui;
    }
}
