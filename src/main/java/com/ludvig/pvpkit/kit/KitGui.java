package com.ludvig.pvpkit.kit;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class KitGui implements Listener {

    private static final int[] KIT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final PvPKitPlugin plugin;
    private final KitManager kitManager;

    public KitGui(PvPKitPlugin plugin) {
        this.plugin = plugin;
        this.kitManager = plugin.getKitManager();
    }

    public void open(Player player) {
        KitMenuHolder holder = new KitMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, 54, Text.color("&8Kits &7| &bPvPKit"));
        holder.inventory = inventory;

        ItemStack filler = filler();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }

        int index = 0;
        for (Kit kit : kitManager.getKits()) {
            if (index >= KIT_SLOTS.length) {
                break;
            }
            int slot = KIT_SLOTS[index++];
            holder.kits.put(slot, kit);
            inventory.setItem(slot, kitItem(player, kit));
        }

        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7F, 1.2F);
    }

    public void openPreview(Player player, Kit kit) {
        PreviewHolder holder = new PreviewHolder();
        Inventory inventory = Bukkit.createInventory(holder, 54,
                Text.color("&8Preview &7| " + kit.id()));
        holder.inventory = inventory;

        ItemStack filler = filler();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }

        inventory.setItem(0, kit.helmet() == null ? emptyArmor(Material.IRON_HELMET, "Helmet") : kit.helmet().clone());
        inventory.setItem(1, kit.chestplate() == null ? emptyArmor(Material.IRON_CHESTPLATE, "Chestplate") : kit.chestplate().clone());
        inventory.setItem(2, kit.leggings() == null ? emptyArmor(Material.IRON_LEGGINGS, "Leggings") : kit.leggings().clone());
        inventory.setItem(3, kit.boots() == null ? emptyArmor(Material.IRON_BOOTS, "Boots") : kit.boots().clone());
        inventory.setItem(4, kit.offhand() == null ? emptyArmor(Material.SHIELD, "Offhand") : kit.offhand().clone());

        int slot = 9;
        for (ItemStack item : kit.items()) {
            if (slot >= 45) {
                break;
            }
            inventory.setItem(slot++, item.clone());
        }

        slot = 45;
        for (var effect : kit.effects()) {
            if (slot >= 54) {
                break;
            }
            inventory.setItem(slot++, effectIcon(effect));
        }

        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8F, 1.2F);
    }

    private ItemStack kitItem(Player player, Kit kit) {
        ItemStack item = new ItemStack(kit.icon());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        long remaining = kitManager.getRemainingMillis(player, kit);
        boolean combatBlocked = plugin.getConfig().getBoolean("settings.kits.block-in-combat", true)
                && plugin.getCombatManager().isTagged(player)
                && !player.hasPermission("pvpplugin.kit.bypass");

        List<Component> lore = new ArrayList<>();
        lore.add(Text.color("&7Cooldown: &f" + kit.cooldownSeconds() + "s"));
        lore.add(Text.color("&7Status: " + (remaining > 0L ? "&c" + Text.duration(remaining) : "&aReady")));
        lore.add(Text.color("&7Items: &f" + kit.items().size() + " &8| &7Effects: &f" + kit.effects().size()));
        lore.add(Component.empty());
        lore.add(Text.color("&eLeft click &7to claim"));
        lore.add(Text.color("&bShift + click &7to preview"));
        if (combatBlocked) {
            lore.add(Text.color("&cYou are in combat!"));
        }

        meta.displayName(kit.displayName());
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack effectIcon(org.bukkit.potion.PotionEffect effect) {
        ItemStack item = new ItemStack(Material.POTION);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = effect.getType().getKey().getKey().toUpperCase(Locale.ROOT).replace('_', ' ');
            meta.displayName(Text.color("&d" + name + " &7" + (effect.getAmplifier() + 1)));
            meta.lore(List.of(Text.color("&7Duration: &f" + Text.duration(effect.getDuration() * 50L))));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack emptyArmor(Material material, String label) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.color("&8" + label + ": none"));
            item.setItemMeta(meta);
        }
        return item;
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

    private void claim(Player player, Kit kit) {
        long remaining = kitManager.getRemainingMillis(player, kit);
        if (remaining > 0L && !player.hasPermission("pvpplugin.kit.bypass")) {
            player.sendMessage(Text.prefix("&cKit &f" + kit.id() + " &cis on cooldown for &f"
                    + Text.duration(remaining) + "&c."));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8F, 1.0F);
            return;
        }

        if (plugin.getConfig().getBoolean("settings.kits.block-in-combat", true)
                && plugin.getCombatManager().isTagged(player)
                && !player.hasPermission("pvpplugin.kit.bypass")) {
            player.sendMessage(Text.prefix("&cYou cannot claim a kit while in combat."));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8F, 1.0F);
            return;
        }

        kitManager.apply(player, kit);
        player.closeInventory();
        player.sendMessage(Text.prefix("&aKit ").append(kit.displayName())
                .append(Text.color(" &aclaimed. &7Cooldown: &f"
                        + Text.duration(kit.cooldownSeconds() * 1000L))));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof PreviewHolder) {
            event.setCancelled(true);
            return;
        }

        if (!(holder instanceof KitMenuHolder menu)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getInventory())) {
            return;
        }

        Kit kit = menu.kits.get(event.getSlot());
        if (kit == null) {
            return;
        }

        if (event.isShiftClick()) {
            openPreview(player, kit);
        } else {
            claim(player, kit);
        }
    }

    private static final class KitMenuHolder implements InventoryHolder {
        private final Map<Integer, Kit> kits = new HashMap<>();
        private Inventory inventory;

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    private static final class PreviewHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}
