package com.ludvig.reportplus.report;

import com.ludvig.reportplus.ReportPlusPlusPlugin;
import com.ludvig.reportplus.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ReportGui implements Listener {

    private final ReportManager reportManager;

    public ReportGui(ReportPlusPlusPlugin plugin) {
        this.reportManager = plugin.getReportManager();
    }

    public void open(Player player) {
        ReportMenuHolder holder = new ReportMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, 54, Text.color("&8Reports &7| &cReport++"));
        holder.inventory = inventory;

        ItemStack filler = filler();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }

        List<Report> reports = reportManager.getOpenReports();
        if (reports.isEmpty()) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta meta = empty.getItemMeta();
            if (meta != null) {
                meta.displayName(Text.color("&cNo open reports"));
                empty.setItemMeta(meta);
            }
            inventory.setItem(22, empty);
        } else {
            int slot = 10;
            for (Report report : reports) {
                if (slot >= 44) {
                    break;
                }
                if (slot % 9 >= 7) {
                    slot += 3;
                }
                holder.reports.put(slot, report);
                inventory.setItem(slot, reportItem(report));
                slot++;
            }
        }

        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7F, 1.1F);
    }

    private ItemStack reportItem(Report report) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.displayName(Text.color("&c#" + report.id() + " &f" + report.targetName()));

        List<Component> lore = new ArrayList<>();
        lore.add(Text.color("&7Reporter: &f" + report.reporterName()));
        lore.add(Text.color("&7Created: &f" + Text.duration(System.currentTimeMillis() - report.createdAt()) + " ago"));
        lore.add(Text.color("&7Status: " + (report.status() == ReportStatus.OPEN ? "&aOPEN" : "&cCLOSED")));
        lore.add(Component.empty());
        for (String line : wrap(report.reason(), 42)) {
            lore.add(Text.color("&7" + line));
        }
        lore.add(Component.empty());
        lore.add(Text.color("&eClick &7to teleport"));
        lore.add(Text.color("&bRight click &7for details"));
        lore.add(Text.color("&aShift + click &7to close"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private List<String> wrap(String input, int width) {
        List<String> lines = new ArrayList<>();
        String remaining = input;
        while (remaining.length() > width) {
            int split = remaining.lastIndexOf(' ', width);
            if (split <= 0) {
                split = width;
            }
            lines.add(remaining.substring(0, split));
            remaining = remaining.substring(Math.min(split + 1, remaining.length()));
        }
        if (!remaining.isEmpty()) {
            lines.add(remaining);
        }
        return lines;
    }

    private ItemStack filler() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ReportMenuHolder menu)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getInventory())) {
            return;
        }

        Report report = menu.reports.get(event.getSlot());
        if (report == null) {
            return;
        }

        if (event.isShiftClick() && event.isLeftClick()) {
            reportManager.closeReport(report.id(), player.getName());
            player.sendMessage(Text.prefix("&aReport &f#" + report.id() + " &aclosed."));
            open(player);
            return;
        }

        if (event.isRightClick()) {
            player.sendMessage(Text.color("&8&m------&r &c&lReport #" + report.id() + " &8&m------"));
            player.sendMessage(Text.color("&7Target: &f" + report.targetName()));
            player.sendMessage(Text.color("&7Reporter: &f" + report.reporterName()));
            player.sendMessage(Text.color("&7Reason: &f" + report.reason()));
            player.sendMessage(Text.color("&8&m----------------------------"));
            return;
        }

        Player target = Bukkit.getPlayer(report.targetId());
        if (target != null) {
            player.teleport(target);
            player.sendMessage(Text.prefix("&aTeleported to &f" + target.getName() + "&a."));
            return;
        }

        ReportLocation location = report.location();
        if (location == null) {
            player.sendMessage(Text.prefix("&cNo saved location for this report."));
            return;
        }

        World world = Bukkit.getWorld(location.world());
        if (world == null) {
            player.sendMessage(Text.prefix("&cWorld &f" + location.world() + " &cis not loaded."));
            return;
        }

        player.teleport(new Location(world, location.x(), location.y(), location.z(),
                location.yaw(), location.pitch()));
        player.sendMessage(Text.prefix("&aTeleported to the saved report location."));
    }

    private static final class ReportMenuHolder implements InventoryHolder {
        private final Map<Integer, Report> reports = new HashMap<>();
        private Inventory inventory;

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}
