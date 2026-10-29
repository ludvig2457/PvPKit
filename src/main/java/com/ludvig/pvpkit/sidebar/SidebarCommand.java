package com.ludvig.pvpkit.sidebar;

import com.ludvig.pvpkit.PvPKitPlugin;
import com.ludvig.pvpkit.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public final class SidebarCommand implements CommandExecutor, TabCompleter {

    private final SidebarManager sidebarManager;

    public SidebarCommand(PvPKitPlugin plugin) {
        this.sidebarManager = plugin.getSidebarManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.prefix("&cOnly players can toggle the scoreboard."));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("toggle")) {
            sidebarManager.toggle(player);
            return true;
        }

        String option = args[0].toLowerCase(Locale.ROOT);
        boolean enabled = sidebarManager.isEnabled(player);
        switch (option) {
            case "on" -> {
                if (enabled) {
                    player.sendMessage(Text.prefix("&eScoreboard is already enabled."));
                } else {
                    sidebarManager.toggle(player);
                }
            }
            case "off" -> {
                if (!enabled) {
                    player.sendMessage(Text.prefix("&eScoreboard is already disabled."));
                } else {
                    sidebarManager.toggle(player);
                }
            }
            default -> player.sendMessage(Text.prefix("&7Usage: &f/scoreboard [on|off]"));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return List.of("on", "off").stream().filter(value -> value.startsWith(prefix)).toList();
        }
        return List.of();
    }
}
