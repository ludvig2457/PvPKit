package com.ludvig.pvpkit.report;

import com.ludvig.pvpkit.PvPKitPlugin;
import com.ludvig.pvpkit.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
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

public final class ReportsCommand implements CommandExecutor, TabCompleter {

    private final ReportManager reportManager;

    public ReportsCommand(PvPKitPlugin plugin) {
        this.reportManager = plugin.getReportManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("pvpplugin.report.staff")) {
            sender.sendMessage(Text.prefix("&cYou don't have permission to do that."));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            listReports(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        if (subCommand.equals("tp")) {
            teleportToReport(sender, args);
            return true;
        }
        if (subCommand.equals("close")) {
            closeReport(sender, args);
            return true;
        }
        if (subCommand.equals("info")) {
            showInfo(sender, args);
            return true;
        }

        sender.sendMessage(Text.prefix("&7Usage: &f/reports [list|tp <id>|close <id>|info <id>]"));
        return true;
    }

    private void listReports(CommandSender sender) {
        List<Report> open = reportManager.getOpenReports();
        if (open.isEmpty()) {
            sender.sendMessage(Text.prefix("&aThere are no open reports."));
            return;
        }

        sender.sendMessage(Text.color("&8&m------&r &c&lOpen reports &7(" + open.size() + ") &8&m------"));
        long now = System.currentTimeMillis();

        for (Report report : open) {
            String reason = report.reason().length() > 40
                    ? report.reason().substring(0, 37) + "..."
                    : report.reason();

            Component line = Text.color("&8» &f#" + report.id() + " &e" + report.targetName()
                    + " &7by &f" + report.reporterName() + " &8| &7" + Text.duration(now - report.createdAt())
                    + " &8| &7")
                    .append(Component.text(reason, NamedTextColor.GRAY))
                    .append(Text.color(" &8[&bTP&8]")
                            .clickEvent(ClickEvent.runCommand("/reports tp " + report.id()))
                            .hoverEvent(HoverEvent.showText(Text.color("&7Click to teleport"))))
                    .append(Text.color(" &8[&aClose&8]")
                            .clickEvent(ClickEvent.runCommand("/reports close " + report.id()))
                            .hoverEvent(HoverEvent.showText(Text.color("&7Click to close"))))
                    .hoverEvent(HoverEvent.showText(Text.color("&7Reason: &f" + report.reason())));

            sender.sendMessage(line);
        }
        sender.sendMessage(Text.color("&8&m--------------------------------"));
    }

    private void teleportToReport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.prefix("&cOnly players can teleport to reports."));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Text.prefix("&7Usage: &f/reports tp <id>"));
            return;
        }

        Report report = parseReport(sender, args[1]);
        if (report == null) {
            return;
        }

        Player target = Bukkit.getPlayer(report.targetId());
        if (target != null) {
            player.teleport(target);
            sender.sendMessage(Text.prefix("&aTeleported to &f" + target.getName() + "&a."));
            return;
        }

        ReportLocation location = report.location();
        if (location == null) {
            sender.sendMessage(Text.prefix("&cNo saved location for this report."));
            return;
        }

        World world = Bukkit.getWorld(location.world());
        if (world == null) {
            sender.sendMessage(Text.prefix("&cWorld &f" + location.world() + " &cis not loaded."));
            return;
        }

        player.teleport(new Location(world, location.x(), location.y(), location.z(),
                location.yaw(), location.pitch()));
        sender.sendMessage(Text.prefix("&aTeleported to the saved report location."));
    }

    private void closeReport(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Text.prefix("&7Usage: &f/reports close <id>"));
            return;
        }

        Report report = parseReport(sender, args[1]);
        if (report == null) {
            return;
        }
        if (report.status() == ReportStatus.CLOSED) {
            sender.sendMessage(Text.prefix("&eReport &f#" + report.id() + " &eis already closed."));
            return;
        }

        Report closed = reportManager.closeReport(report.id(), sender.getName());
        if (closed == null) {
            sender.sendMessage(Text.prefix("&cCould not close that report."));
            return;
        }

        sender.sendMessage(Text.prefix("&aReport &f#" + closed.id() + " &aclosed."));
    }

    private void showInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Text.prefix("&7Usage: &f/reports info <id>"));
            return;
        }

        Report report = parseReport(sender, args[1]);
        if (report == null) {
            return;
        }

        sender.sendMessage(Text.color("&8&m------&r &c&lReport #" + report.id() + " &8&m------"));
        sender.sendMessage(Text.color("&7Target: &f" + report.targetName()));
        sender.sendMessage(Text.color("&7Reporter: &f" + report.reporterName()));
        sender.sendMessage(Text.color("&7Created: &f" + Text.duration(System.currentTimeMillis() - report.createdAt()) + " ago"));
        sender.sendMessage(Text.color("&7Status: " + (report.status() == ReportStatus.OPEN ? "&aOPEN" : "&cCLOSED")));
        if (report.status() == ReportStatus.CLOSED && !report.handledBy().isBlank()) {
            sender.sendMessage(Text.color("&7Handled by: &f" + report.handledBy()));
        }
        sender.sendMessage(Text.color("&7Reason: &f" + report.reason()));
        sender.sendMessage(Text.color("&8&m----------------------------"));
    }

    private Report parseReport(CommandSender sender, String idText) {
        int id;
        try {
            id = Integer.parseInt(idText);
        } catch (NumberFormatException exception) {
            sender.sendMessage(Text.prefix("&cInvalid report id."));
            return null;
        }

        Report report = reportManager.getReport(id);
        if (report == null) {
            sender.sendMessage(Text.prefix("&cReport &f#" + id + " &cwas not found."));
            return null;
        }
        return report;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            suggestions.addAll(List.of("list", "tp", "close", "info"));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase(Locale.ROOT);
            if (subCommand.equals("tp") || subCommand.equals("close") || subCommand.equals("info")) {
                suggestions.addAll(reportManager.getOpenReports().stream()
                        .map(report -> Integer.toString(report.id()))
                        .toList());
            }
        }

        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return suggestions.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix))
                .distinct()
                .toList();
    }
}
