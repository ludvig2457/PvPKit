package com.ludvig.pvpkit.kit;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.List;

public record Kit(
        String id,
        Component displayName,
        Material icon,
        int cooldownSeconds,
        List<ItemStack> items,
        ItemStack helmet,
        ItemStack chestplate,
        ItemStack leggings,
        ItemStack boots,
        ItemStack offhand,
        List<PotionEffect> effects
) {
}
