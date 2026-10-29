package com.ludvig.pvpkit;

import com.ludvig.pvpkit.hud.HudCommand;
import com.ludvig.pvpkit.hud.HudListener;
import com.ludvig.pvpkit.hud.HudManager;
import com.ludvig.pvpkit.kit.KitCommand;
import com.ludvig.pvpkit.kit.KitManager;
import com.ludvig.pvpkit.report.ReportCommand;
import com.ludvig.pvpkit.report.ReportManager;
import com.ludvig.pvpkit.report.ReportsCommand;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public final class PvPKitPlugin extends JavaPlugin {

    private HudManager hudManager;
    private KitManager kitManager;
    private ReportManager reportManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.hudManager = new HudManager(this);
        this.kitManager = new KitManager(this);
        this.reportManager = new ReportManager(this);

        this.kitManager.load();
        this.reportManager.load();
        this.hudManager.start();

        getServer().getPluginManager().registerEvents(new HudListener(this), this);

        register("hud", new HudCommand(this));
        register("kit", new KitCommand(this));
        register("report", new ReportCommand(this));
        register("reports", new ReportsCommand(this));
        register("pvpkit", new PvPKitCommand(this));

        getLogger().info("PvPKit enabled: HUD, kits and reports are ready.");
    }

    @Override
    public void onDisable() {
        if (hudManager != null) {
            hudManager.stop();
        }
        if (kitManager != null) {
            kitManager.save();
        }
        if (reportManager != null) {
            reportManager.save();
        }
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
        kitManager.load();
        reportManager.load();
        hudManager.start();
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
}
