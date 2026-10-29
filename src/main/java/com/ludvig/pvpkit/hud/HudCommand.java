package com.ludvig.pvpkit.hud;

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

public final class HudCommand implements CommandExecutor, TabCompleter {

    private final HudManager hudManager;

    public HudCommand(PvPKitPlugin plugin) {
        this.hudManager = plugin.getHudManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.prefix("&cOnly players can use the HUD."));
            return true;
        }

        if (args.length == 0) {
            hudManager.toggle(player);
            return true;
        }

        String option = args[0].toLowerCase(Locale.ROOT);
        boolean enabled = hudManager.isEnabled(player);

        switch (option) {
            case "on" -> {
                if (enabled) {
                    player.sendMessage(Text.prefix("&eHUD is already enabled."));
                } else {
                    hudManager.toggle(player);
                }
            }
            case "off" -> {
                if (!enabled) {
                    player.sendMessage(Text.prefix("&eHUD is already disabled."));
                } else {
                    hudManager.toggle(player);
                }
            }
            case "toggle" -> hudManager.toggle(player);
            default -> player.sendMessage(Text.prefix("&7Usage: &f/hud [on|off]"));
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
