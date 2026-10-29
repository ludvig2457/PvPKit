package com.ludvig.reportplus;

import com.ludvig.reportplus.report.ReportCommand;
import com.ludvig.reportplus.report.ReportGui;
import com.ludvig.reportplus.report.ReportManager;
import com.ludvig.reportplus.report.ReportsCommand;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public final class ReportPlusPlusPlugin extends JavaPlugin {

    private ReportManager reportManager;
    private ReportGui reportGui;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.reportManager = new ReportManager(this);
        this.reportManager.load();
        this.reportGui = new ReportGui(this);

        getServer().getPluginManager().registerEvents(reportGui, this);
        register("report", new ReportCommand(this));
        register("reports", new ReportsCommand(this));

        getLogger().info("Report++ enabled.");
    }

    @Override
    public void onDisable() {
        if (reportManager != null) {
            reportManager.save();
        }
    }

    public void reloadAll() {
        reloadConfig();
        reportManager.load();
        getLogger().info("Report++ configuration reloaded.");
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

    public ReportManager getReportManager() {
        return reportManager;
    }

    public ReportGui getReportGui() {
        return reportGui;
    }
}
