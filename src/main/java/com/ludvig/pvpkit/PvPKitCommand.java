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
            sender.sendMessage(Text.prefix("&aConfiguration, kits and reports reloaded."));
            return true;
        }

        sender.sendMessage(Text.color("&8&m--------------------------------"));
        sender.sendMessage(Text.color("&b&lPvPKit &7v" + plugin.getPluginMeta().getVersion()));
        sender.sendMessage(Text.color("&7HUD: &f/hud &8| &7Kits: &f/kit &8| &7Reports: &f/report"));
        sender.sendMessage(Text.color("&7Reload: &f/pvpkit reload"));
        sender.sendMessage(Text.color("&8&m--------------------------------"));
        return true;
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
