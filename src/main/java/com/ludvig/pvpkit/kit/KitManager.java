package com.ludvig.pvpkit.kit;

import com.ludvig.pvpkit.PvPKitPlugin;
import com.ludvig.pvpkit.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class KitManager {

    private final PvPKitPlugin plugin;
    private final Map<String, Kit> kits = new LinkedHashMap<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private final File cooldownFile;

    public KitManager(PvPKitPlugin plugin) {
        this.plugin = plugin;
        this.cooldownFile = new File(plugin.getDataFolder(), "cooldowns.yml");
    }

    public void load() {
        kits.clear();

        ConfigurationSection root = plugin.getConfig().getConfigurationSection("kits");
        if (root == null) {
            plugin.getLogger().warning("No kits section found in config.yml");
            return;
        }

        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }

            try {
                kits.put(id.toLowerCase(Locale.ROOT), readKit(id.toLowerCase(Locale.ROOT), section));
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("Failed to load kit '" + id + "': " + exception.getMessage());
            }
        }

        loadCooldowns();
        plugin.getLogger().info("Loaded " + kits.size() + " kit(s).");
    }

    private Kit readKit(String id, ConfigurationSection section) {
        Component displayName = Text.color(section.getString("display-name", "&b" + id));
        Material icon = Material.matchMaterial(section.getString("icon", "CHEST"));
        if (icon == null || icon.isAir()) {
            icon = Material.CHEST;
        }

        List<ItemStack> items = new ArrayList<>();
        for (String raw : section.getStringList("items")) {
            items.add(parseItem(raw));
        }

        ConfigurationSection armor = section.getConfigurationSection("armor");
        ItemStack helmet = armor == null ? null : parseOptionalItem(armor.getString("helmet"));
        ItemStack chestplate = armor == null ? null : parseOptionalItem(armor.getString("chestplate"));
        ItemStack leggings = armor == null ? null : parseOptionalItem(armor.getString("leggings"));
        ItemStack boots = armor == null ? null : parseOptionalItem(armor.getString("boots"));
        ItemStack offhand = parseOptionalItem(section.getString("offhand"));

        List<PotionEffect> effects = new ArrayList<>();
        for (String raw : section.getStringList("effects")) {
            effects.add(parseEffect(raw));
        }

        int cooldown = Math.max(0, section.getInt("cooldown-seconds", 60));
        return new Kit(id, displayName, icon, cooldown, List.copyOf(items), helmet, chestplate,
                leggings, boots, offhand, List.copyOf(effects));
    }

    private ItemStack parseOptionalItem(String raw) {
        return raw == null || raw.isBlank() ? null : parseItem(raw);
    }

    private ItemStack parseItem(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Empty item definition");
        }

        String[] parts = raw.trim().split("\\s+");
        String[] materialParts = parts[0].split(":", 2);

        Material material = Material.matchMaterial(materialParts[0].toUpperCase(Locale.ROOT));
        if (material == null || material.isAir()) {
            throw new IllegalArgumentException("Unknown material: " + materialParts[0]);
        }

        int amount = materialParts.length > 1 ? Integer.parseInt(materialParts[1]) : 1;
        amount = Math.max(1, Math.min(amount, material.getMaxStackSize()));

        ItemStack item = new ItemStack(material, amount);
        if (parts.length == 1) {
            return item;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        for (int index = 1; index < parts.length; index++) {
            String token = parts[index];
            if (token.equalsIgnoreCase("UNBREAKABLE")) {
                meta.setUnbreakable(true);
                continue;
            }

            String[] enchantParts = token.split(":", 2);
            if (enchantParts.length != 2) {
                throw new IllegalArgumentException("Invalid enchantment: " + token);
            }

            Enchantment enchantment = Registry.ENCHANTMENT.get(
                    NamespacedKey.minecraft(enchantParts[0].toLowerCase(Locale.ROOT)));
            if (enchantment == null) {
                throw new IllegalArgumentException("Unknown enchantment: " + enchantParts[0]);
            }

            int level = Integer.parseInt(enchantParts[1]);
            meta.addEnchant(enchantment, level, true);
        }

        item.setItemMeta(meta);
        return item;
    }

    private PotionEffect parseEffect(String raw) {
        String[] parts = raw.trim().split(":");
        if (parts.length < 1) {
            throw new IllegalArgumentException("Empty effect definition");
        }

        PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase(Locale.ROOT));
        if (type == null) {
            throw new IllegalArgumentException("Unknown effect: " + parts[0]);
        }

        int amplifier = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        int seconds = parts.length > 2 ? Integer.parseInt(parts[2]) : 300;
        return new PotionEffect(type, Math.max(1, seconds) * 20, Math.max(0, amplifier), false, true, true);
    }

    public void apply(Player player, Kit kit) {
        if (plugin.getConfig().getBoolean("settings.kits.clear-inventory", true)) {
            player.getInventory().clear();
        }
        if (plugin.getConfig().getBoolean("settings.kits.clear-effects", true)) {
            for (PotionEffect effect : List.copyOf(player.getActivePotionEffects())) {
                player.removePotionEffect(effect.getType());
            }
        }
        if (plugin.getConfig().getBoolean("settings.kits.heal-on-kit", true)) {
            AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
            player.setHealth(maxHealth == null ? 20.0 : maxHealth.getValue());
            player.setFoodLevel(20);
            player.setSaturation(20.0F);
        }

        player.getInventory().setHelmet(itemOrAir(kit.helmet()));
        player.getInventory().setChestplate(itemOrAir(kit.chestplate()));
        player.getInventory().setLeggings(itemOrAir(kit.leggings()));
        player.getInventory().setBoots(itemOrAir(kit.boots()));
        player.getInventory().setItemInOffHand(itemOrAir(kit.offhand()));

        for (ItemStack item : kit.items()) {
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item.clone());
            for (ItemStack leftover : leftovers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }

        for (PotionEffect effect : kit.effects()) {
            player.addPotionEffect(new PotionEffect(effect.getType(), effect.getDuration(), effect.getAmplifier(),
                    effect.isAmbient(), effect.hasParticles(), effect.hasIcon()));
        }

        player.updateInventory();
        setCooldown(player.getUniqueId(), kit.id(), kit.cooldownSeconds() * 1000L);
    }

    private ItemStack itemOrAir(ItemStack item) {
        return item == null ? new ItemStack(Material.AIR) : item.clone();
    }

    public Kit getKit(String id) {
        return kits.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<Kit> getKits() {
        return kits.values();
    }

    public long getRemainingMillis(Player player, Kit kit) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) {
            return 0L;
        }

        Long until = playerCooldowns.get(kit.id());
        if (until == null) {
            return 0L;
        }

        return Math.max(0L, until - System.currentTimeMillis());
    }

    public void setCooldown(UUID uuid, String kitId, long millis) {
        cooldowns.computeIfAbsent(uuid, ignored -> new HashMap<>())
                .put(kitId.toLowerCase(Locale.ROOT), System.currentTimeMillis() + millis);
        save();
    }

    public void resetCooldown(UUID uuid, String kitId) {
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);
        if (playerCooldowns != null) {
            playerCooldowns.remove(kitId.toLowerCase(Locale.ROOT));
        }
        save();
    }

    public void resetAllCooldowns(UUID uuid) {
        cooldowns.remove(uuid);
        save();
    }

    private void loadCooldowns() {
        cooldowns.clear();
        if (!cooldownFile.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(cooldownFile);
        ConfigurationSection root = yaml.getConfigurationSection("cooldowns");
        if (root == null) {
            return;
        }

        for (String uuidText : root.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidText);
                ConfigurationSection playerSection = root.getConfigurationSection(uuidText);
                if (playerSection == null) {
                    continue;
                }

                Map<String, Long> playerCooldowns = new HashMap<>();
                for (String kitId : playerSection.getKeys(false)) {
                    long until = playerSection.getLong(kitId);
                    if (until > System.currentTimeMillis()) {
                        playerCooldowns.put(kitId, until);
                    }
                }
                if (!playerCooldowns.isEmpty()) {
                    cooldowns.put(uuid, playerCooldowns);
                }
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Skipping invalid cooldown UUID: " + uuidText);
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        long now = System.currentTimeMillis();

        for (Map.Entry<UUID, Map<String, Long>> entry : cooldowns.entrySet()) {
            for (Map.Entry<String, Long> cooldown : entry.getValue().entrySet()) {
                if (cooldown.getValue() > now) {
                    yaml.set("cooldowns." + entry.getKey() + "." + cooldown.getKey(), cooldown.getValue());
                }
            }
        }

        try {
            File parent = cooldownFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("Could not create plugin data folder");
            }
            yaml.save(cooldownFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save cooldowns.yml: " + exception.getMessage());
        }
    }
}
