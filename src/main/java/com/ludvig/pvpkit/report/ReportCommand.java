package com.ludvig.pvpkit.report;

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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class ReportCommand implements CommandExecutor, TabCompleter {

    private final PvPKitPlugin plugin;
    private final ReportManager reportManager;

    public ReportCommand(PvPKitPlugin plugin) {
        this.plugin = plugin;
        this.reportManager = plugin.getReportManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player reporter)) {
            sender.sendMessage(Text.prefix("&cOnly players can create reports."));
            return true;
        }

        if (args.length < 2) {
            reporter.sendMessage(Text.prefix("&7Usage: &f/report <player> <reason>"));
            return true;
        }

        String targetName = args[0];
        OfflinePlayer target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
            if (!offline.hasPlayedBefore() && !offline.isOnline()) {
                reporter.sendMessage(Text.prefix("&cPlayer &f" + targetName + " &cwas not found."));
                return true;
            }
            target = offline;
        }

        if (target.getUniqueId().equals(reporter.getUniqueId())) {
            reporter.sendMessage(Text.prefix("&cYou cannot report yourself."));
            return true;
        }

        int maxLength = plugin.getConfig().getInt("settings.reports.max-reason-length", 200);
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
        if (reason.isEmpty()) {
            reporter.sendMessage(Text.prefix("&cPlease provide a reason."));
            return true;
        }
        if (reason.length() > maxLength) {
            reporter.sendMessage(Text.prefix("&cReason is too long (maximum " + maxLength + " characters)."));
            return true;
        }

        int cooldownSeconds = plugin.getConfig().getInt("settings.reports.cooldown-seconds", 60);
        long remaining = reportManager.getRemainingCooldown(reporter.getUniqueId(), cooldownSeconds);
        if (remaining > 0L && !reporter.hasPermission("pvpplugin.report.bypass")) {
            reporter.sendMessage(Text.prefix("&cPlease wait &f" + Text.duration(remaining)
                    + " &cbefore sending another report."));
            return true;
        }

        Report report = reportManager.create(reporter, target, reason);
        reporter.sendMessage(Text.prefix("&aReport &f#" + report.id() + " &aagainst &f"
                + report.targetName() + " &asent to the staff."));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> !name.equalsIgnoreCase(sender.getName()))
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .toList();
        }
        return List.of();
    }
}
