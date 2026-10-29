package com.ludvig.pvpkit.stats;

import com.ludvig.pvpkit.PvPKitPlugin;
import com.ludvig.pvpkit.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class StatsCommand implements CommandExecutor, TabCompleter {

    private static final int PAGE_SIZE = 10;

    private final StatsManager statsManager;

    public StatsCommand(PvPKitPlugin plugin) {
        this.statsManager = plugin.getStatsManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Text.prefix("&7Usage: &f/stats <player> &8| &f/stats top"));
                return true;
            }
            showStats(sender, statsManager.getOrCreate(player));
            return true;
        }

        if (args[0].equalsIgnoreCase("top")) {
            showTop(sender, args);
            return true;
        }

        if (args[0].equalsIgnoreCase("reset")) {
            reset(sender, args);
            return true;
        }

        OfflinePlayer target = resolve(args[0]);
        if (target == null) {
            sender.sendMessage(Text.prefix("&cPlayer &f" + args[0] + " &cwas not found."));
            return true;
        }

        PlayerStats playerStats = statsManager.get(target.getUniqueId());
        if (playerStats == null) {
            sender.sendMessage(Text.prefix("&eNo stats for &f" + args[0] + " &eyet."));
            return true;
        }
        showStats(sender, playerStats);
        return true;
    }

    private void showStats(CommandSender sender, PlayerStats playerStats) {
        sender.sendMessage(Text.color("&8&m------&r &b&l" + playerStats.name() + " &8&m------"));
        sender.sendMessage(Text.color("&7Kills: &a" + playerStats.kills() + " &8| &7Deaths: &c" + playerStats.deaths()));
        sender.sendMessage(Text.color("&7KDR: &e" + Text.formatDouble(playerStats.kdr())));
        sender.sendMessage(Text.color("&7Streak: &6" + playerStats.streak()
                + " &8| &7Best streak: &6" + playerStats.bestStreak()));
        sender.sendMessage(Text.color("&8&m----------------------------"));
    }

    private void showTop(CommandSender sender, String[] args) {
        int page = 1;
        if (args.length > 1) {
            try {
                page = Math.max(1, Integer.parseInt(args[1]));
            } catch (NumberFormatException exception) {
                sender.sendMessage(Text.prefix("&cInvalid page number."));
                return;
            }
        }

        List<PlayerStats> all = statsManager.top(Integer.MAX_VALUE);
        int pages = Math.max(1, (all.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.min(page, pages);

        sender.sendMessage(Text.color("&8&m------&r &6&lTop kills &7(" + page + "/" + pages + ") &8&m------"));
        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(all.size(), start + PAGE_SIZE);

        for (int index = start; index < end; index++) {
            PlayerStats playerStats = all.get(index);
            sender.sendMessage(Text.color("&e#" + (index + 1) + " &f" + playerStats.name()
                    + " &8- &a" + playerStats.kills() + " kills &8| &7KDR &e"
                    + Text.formatDouble(playerStats.kdr())));
        }

        if (all.isEmpty()) {
            sender.sendMessage(Text.color("&7No stats recorded yet."));
        }
        sender.sendMessage(Text.color("&8&m--------------------------------"));
    }

    private void reset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pvpplugin.stats.admin")) {
            sender.sendMessage(Text.prefix("&cYou don't have permission to reset stats."));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Text.prefix("&7Usage: &f/stats reset <player>"));
            return;
        }

        OfflinePlayer target = resolve(args[1]);
        if (target == null) {
            sender.sendMessage(Text.prefix("&cPlayer &f" + args[1] + " &cwas not found."));
            return;
        }

        statsManager.reset(target.getUniqueId());
        sender.sendMessage(Text.prefix("&aStats reset for &f" + args[1] + "&a."));
    }

    private OfflinePlayer resolve(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        return offline.hasPlayedBefore() ? offline : null;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            suggestions.add("top");
            if (sender.hasPermission("pvpplugin.stats.admin")) {
                suggestions.add("reset");
            }
            suggestions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            suggestions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        }

        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return suggestions.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix))
                .distinct()
                .toList();
    }
}
