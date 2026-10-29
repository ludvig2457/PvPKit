package com.ludvig.pvpkit.settings;

import com.ludvig.pvpkit.PvPKitPlugin;
import com.ludvig.pvpkit.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class SettingsGui implements Listener {

    private final PvPKitPlugin plugin;

    public SettingsGui(PvPKitPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        SettingsHolder holder = new SettingsHolder();
        Inventory inventory = Bukkit.createInventory(holder, 27, Text.color("&8Settings &7| &bPvPKit"));
        holder.inventory = inventory;

        ItemStack filler = filler();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }

        inventory.setItem(10, toggleItem(Material.COMPASS, "&bHUD",
                plugin.getHudManager().isEnabled(player), "CPS, armor, keystrokes"));
        inventory.setItem(12, toggleItem(Material.CLOCK, "&6Scoreboard",
                plugin.getSidebarManager().isEnabled(player), "Kills, KDR, streak, combat"));
        inventory.setItem(14, infoItem(Material.NETHERITE_SWORD, "&cCombat system",
                List.of(Text.color("&7Current: " + plugin.getCombatManager().getMode().coloredName()),
                        Text.color("&7Change: &f/combat <mode>"),
                        Text.color("&7Modes: &f1.8.9 &7| &f1.12 &7| &f1.21.11"))));
        inventory.setItem(16, actionItem(Material.CHEST, "&eKits", "Open the KitPvP menu"));
        inventory.setItem(20, actionItem(Material.PLAYER_HEAD, "&aStats", "View your PvP stats"));
        inventory.setItem(22, actionItem(Material.WRITABLE_BOOK, "&cReports", "Open the Report++ staff menu"));
        inventory.setItem(24, infoItem(Material.BOOK, "&7PvPKit",
                List.of(Text.color("&7Version: &f" + plugin.getPluginMeta().getVersion()),
                        Text.color("&7Author: &fludvig2457"),
                        Text.color("&8github.com/ludvig2457"))));

        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.6F, 1.3F);
    }

    private ItemStack toggleItem(Material material, String name, boolean enabled, String description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.color(name));
            meta.lore(List.of(
                    Text.color("&7" + description),
                    Component.empty(),
                    Text.color("&7Status: " + (enabled ? "&aenabled" : "&cdisabled")),
                    Text.color("&eClick &7to toggle")));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack actionItem(Material material, String name, String description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.color(name));
            meta.lore(List.of(Text.color("&7" + description), Text.color("&eClick &7to open")));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack infoItem(Material material, String name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.color(name));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack filler() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SettingsHolder)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getInventory())) {
            return;
        }

        switch (event.getSlot()) {
            case 10 -> {
                plugin.getHudManager().toggle(player);
                refresh(player);
            }
            case 12 -> {
                plugin.getSidebarManager().toggle(player);
                refresh(player);
            }
            case 14 -> {
                player.closeInventory();
                player.performCommand("combat");
            }
            case 16 -> {
                player.closeInventory();
                if (Bukkit.getPluginManager().isPluginEnabled("KitPvP")) {
                    player.performCommand("kit");
                } else {
                    player.sendMessage(Text.prefix("&cKitPvP is not installed."));
                }
            }
            case 20 -> {
                player.closeInventory();
                player.performCommand("stats");
            }
            case 22 -> {
                if (!player.hasPermission("reportplus.report.staff")) {
                    player.sendMessage(Text.prefix("&cYou don't have permission to view reports."));
                    return;
                }
                if (!Bukkit.getPluginManager().isPluginEnabled("ReportPlusPlus")) {
                    player.sendMessage(Text.prefix("&cReport++ is not installed."));
                    return;
                }
                player.closeInventory();
                player.performCommand("reports gui");
            }
            default -> {
                // Ignore filler clicks.
            }
        }
    }

    private void refresh(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                open(player);
            }
        });
    }

    private static final class SettingsHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}
