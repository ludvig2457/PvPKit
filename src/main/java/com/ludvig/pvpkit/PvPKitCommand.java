package com.ludvig.pvpkit;

import com.ludvig.pvpkit.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public final class PvPKitCommand implements CommandExecutor, TabCompleter {

    private final PvPKitPlugin plugin;

    public PvPKitCommand(PvPKitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("pvpplugin.admin")) {
                sender.sendMessage(Text.prefix("&cYou don't have permission to do that."));
                return true;
            }
            plugin.reloadAll();
            sender.sendMessage(Text.prefix("&aPvPKit configuration reloaded."));
            return true;
        }

        sender.sendMessage(Text.color("&8&m--------------------------------"));
        sender.sendMessage(Text.color("&b&lPvPKit &7v" + plugin.getDescription().getVersion()));
        sender.sendMessage(Text.color("&7HUD: &f/hud &8| &7Stats: &f/stats &8| &7Sidebar: &f/scoreboard"));
        sender.sendMessage(Text.color("&7Settings: &f/settings &8| &7Reload: &f/pvpkit reload"));
        sender.sendMessage(Text.color("&7Modules: &f" + modules()));
        sender.sendMessage(Text.color("&7Combat: &f/combat &8| &7Spawn: &f/spawn &8| &7Kits: &f/kit &8| &7Reports: &f/reports"));
        sender.sendMessage(Text.color("&8&m--------------------------------"));
        return true;
    }

    private String modules() {
        List<String> result = new java.util.ArrayList<>();
        addModule(result, "CombatPlusPlus", "Combat++");
        addModule(result, "SpawnPlusPlus", "Spawn++");
        addModule(result, "KitPvP", "KitPvP");
        addModule(result, "ReportPlusPlus", "Report++");
        return result.isEmpty() ? "&7none" : String.join(" &8| &f", result);
    }

    private void addModule(List<String> result, String pluginName, String label) {
        if (plugin.getServer().getPluginManager().isPluginEnabled(pluginName)) {
            result.add(label);
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            if ("reload".startsWith(prefix)) {
                return List.of("reload");
            }
        }
        return List.of();
    }
}
