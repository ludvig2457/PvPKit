package com.ludvig.pvpkit.report;

import com.ludvig.pvpkit.PvPKitPlugin;
import com.ludvig.pvpkit.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ReportManager {

    private final PvPKitPlugin plugin;
    private final Map<Integer, Report> reports = new LinkedHashMap<>();
    private final Map<UUID, Long> lastReportAt = new HashMap<>();
    private final File reportFile;

    private int nextId = 1;

    public ReportManager(PvPKitPlugin plugin) {
        this.plugin = plugin;
        this.reportFile = new File(plugin.getDataFolder(), "reports.yml");
    }

    public void load() {
        reports.clear();
        nextId = 1;

        if (!reportFile.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(reportFile);
        nextId = Math.max(1, yaml.getInt("next-id", 1));
        ConfigurationSection root = yaml.getConfigurationSection("reports");
        if (root == null) {
            return;
        }

        int highestId = 0;
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }

            try {
                int id = Integer.parseInt(key);
                UUID reporterId = UUID.fromString(section.getString("reporter-uuid", ""));
                UUID targetId = UUID.fromString(section.getString("target-uuid", ""));

                ReportLocation location = null;
                ConfigurationSection locationSection = section.getConfigurationSection("location");
                if (locationSection != null && locationSection.isString("world")) {
                    location = new ReportLocation(
                            locationSection.getString("world", "world"),
                            locationSection.getDouble("x"),
                            locationSection.getDouble("y"),
                            locationSection.getDouble("z"),
                            (float) locationSection.getDouble("yaw"),
                            (float) locationSection.getDouble("pitch"));
                }

                ReportStatus status = ReportStatus.OPEN;
                try {
                    status = ReportStatus.valueOf(section.getString("status", "OPEN"));
                } catch (IllegalArgumentException ignored) {
                    // Keep OPEN for malformed values.
                }

                Report report = new Report(
                        id,
                        reporterId,
                        section.getString("reporter", "unknown"),
                        targetId,
                        section.getString("target", "unknown"),
                        section.getString("reason", "No reason"),
                        section.getLong("created-at"),
                        location,
                        status,
                        section.getString("handled-by", ""));
                reports.put(id, report);
                highestId = Math.max(highestId, id);
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("Skipping invalid report '" + key + "': " + exception.getMessage());
            }
        }

        nextId = Math.max(nextId, highestId + 1);
        plugin.getLogger().info("Loaded " + reports.size() + " report(s).");
    }

    public Report create(Player reporter, OfflinePlayer target, String reason) {
        Player onlineTarget = target.getPlayer();
        ReportLocation location = null;
        if (onlineTarget != null) {
            Location where = onlineTarget.getLocation();
            location = new ReportLocation(where.getWorld().getName(), where.getX(), where.getY(), where.getZ(),
                    where.getYaw(), where.getPitch());
        }

        Report report = new Report(nextId++, reporter.getUniqueId(), reporter.getName(),
                target.getUniqueId(), target.getName() == null ? "unknown" : target.getName(),
                reason, System.currentTimeMillis(), location, ReportStatus.OPEN, "");

        reports.put(report.id(), report);
        lastReportAt.put(reporter.getUniqueId(), System.currentTimeMillis());
        save();
        notifyStaff(report);
        return report;
    }

    private void notifyStaff(Report report) {
        Component message = Text.color("&8[&cReport &f#" + report.id() + "&8] &e" + report.targetName()
                + " &7reported by &f" + report.reporterName() + " &8| &7")
                .append(Component.text(report.reason(), NamedTextColor.GRAY))
                .append(Text.color(" &8[&bTP&8]")
                        .clickEvent(ClickEvent.runCommand("/reports tp " + report.id()))
                        .hoverEvent(HoverEvent.showText(Text.color("&7Teleport to the reported player"))))
                .append(Text.color(" &8[&aClose&8]")
                        .clickEvent(ClickEvent.runCommand("/reports close " + report.id()))
                        .hoverEvent(HoverEvent.showText(Text.color("&7Mark this report as handled"))));

        boolean delivered = false;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("pvpplugin.report.notify")) {
                player.sendMessage(message);
                delivered = true;
            }
        }

        if (!delivered) {
            plugin.getLogger().info("New report #" + report.id() + " from " + report.reporterName()
                    + " against " + report.targetName() + ": " + report.reason());
        }
    }

    public List<Report> getOpenReports() {
        return reports.values().stream()
                .filter(report -> report.status() == ReportStatus.OPEN)
                .sorted(Comparator.comparingInt(Report::id))
                .toList();
    }

    public Report getReport(int id) {
        return reports.get(id);
    }

    public Report closeReport(int id, String staffName) {
        Report report = reports.get(id);
        if (report == null || report.status() == ReportStatus.CLOSED) {
            return null;
        }

        Report closed = report.closed(staffName);
        reports.put(id, closed);
        save();

        Player reporter = Bukkit.getPlayer(closed.reporterId());
        if (reporter != null) {
            reporter.sendMessage(Text.prefix("&aYour report &f#" + id + " &aagainst &f"
                    + closed.targetName() + " &awas closed by &f" + staffName + "&a."));
        }
        return closed;
    }

    public long getRemainingCooldown(UUID uuid, int cooldownSeconds) {
        long last = lastReportAt.getOrDefault(uuid, 0L);
        return Math.max(0L, last + cooldownSeconds * 1000L - System.currentTimeMillis());
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("next-id", nextId);

        for (Report report : reports.values()) {
            String path = "reports." + report.id();
            yaml.set(path + ".reporter", report.reporterName());
            yaml.set(path + ".reporter-uuid", report.reporterId().toString());
            yaml.set(path + ".target", report.targetName());
            yaml.set(path + ".target-uuid", report.targetId().toString());
            yaml.set(path + ".reason", report.reason());
            yaml.set(path + ".created-at", report.createdAt());
            yaml.set(path + ".status", report.status().name());
            yaml.set(path + ".handled-by", report.handledBy());

            ReportLocation location = report.location();
            if (location != null) {
                yaml.set(path + ".location.world", location.world());
                yaml.set(path + ".location.x", location.x());
                yaml.set(path + ".location.y", location.y());
                yaml.set(path + ".location.z", location.z());
                yaml.set(path + ".location.yaw", location.yaw());
                yaml.set(path + ".location.pitch", location.pitch());
            }
        }

        try {
            File parent = reportFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("Could not create plugin data folder");
            }
            yaml.save(reportFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save reports.yml: " + exception.getMessage());
        }
    }
}
