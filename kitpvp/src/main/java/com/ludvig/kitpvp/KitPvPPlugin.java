package com.ludvig.kitpvp;

import com.ludvig.kitpvp.kit.KitCommand;
import com.ludvig.kitpvp.kit.KitGui;
import com.ludvig.kitpvp.kit.KitManager;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public final class KitPvPPlugin extends JavaPlugin {

    private KitManager kitManager;
    private KitGui kitGui;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.kitManager = new KitManager(this);
        this.kitManager.load();
        this.kitGui = new KitGui(this);

        getServer().getPluginManager().registerEvents(kitGui, this);
        register("kit", new KitCommand(this));

        getLogger().info("KitPvP enabled with " + kitManager.getKits().size() + " kit(s).");
    }

    @Override
    public void onDisable() {
        if (kitManager != null) {
            kitManager.save();
        }
    }

    public void reloadAll() {
        reloadConfig();
        kitManager.load();
        getLogger().info("KitPvP configuration reloaded.");
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

    public KitManager getKitManager() {
        return kitManager;
    }

    public KitGui getKitGui() {
        return kitGui;
    }
}
