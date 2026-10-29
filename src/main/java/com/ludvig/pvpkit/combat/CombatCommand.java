package com.ludvig.pvpkit.combat;

import com.ludvig.pvpkit.PvPKitPlugin;
import com.ludvig.pvpkit.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public final class CombatCommand implements CommandExecutor, TabCompleter {

    private final PvPKitPlugin plugin;

    public CombatCommand(PvPKitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        CombatManager combatManager = plugin.getCombatManager();

        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            sender.sendMessage(Text.color("&8&m------&r &c&lCombat system &8&m------"));
            sender.sendMessage(Text.color(combatManager.describe()));
            sender.sendMessage(Text.color("&7Available: &f1.8.9 &8| &f1.12 &8| &f1.21.11"));
            if (sender instanceof Player player) {
                sender.sendMessage(Text.color("&7Your combat tag: " + combatManager.status(player)));
                sender.sendMessage(Text.color("&7Your combo: &e" + combatManager.getCombo(player)));
            }
            return true;
        }

        String requested = args[0];
        if (!CombatMode.isValid(requested)) {
            sender.sendMessage(Text.prefix("&cUnknown combat mode. Use &f1.8.9&c, &f1.12 &cor &f1.21.11&c."));
            return true;
        }

        if (!sender.hasPermission("pvpplugin.combat.admin")) {
            sender.sendMessage(Text.prefix("&cYou don't have permission to change the combat system."));
            return true;
        }

        CombatMode mode = CombatMode.parse(requested);
        combatManager.setMode(mode);

        String message = Text.PREFIX + "&7Combat system changed to " + mode.coloredName() + "&7.";
        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(Text.color(message)));
        plugin.getServer().getConsoleSender().sendMessage(Text.color(message));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return List.of("1.8.9", "1.12", "1.21.11", "info").stream()
                    .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .toList();
        }
        return List.of();
    }
}
