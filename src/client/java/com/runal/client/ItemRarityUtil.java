package com.runal.client;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

/**
 * Detects item rarity from the last line of the item's lore, e.g. "&e&lLEGENDARY WEAPON" or
 * "&e&lLEGENDARY ARMOR" - the server marks rarity there rather than via any data component, so
 * this just checks that line's plain text for one of the known rarity words.
 */
public final class ItemRarityUtil {
    private ItemRarityUtil() {
    }

    public static Integer getRarityColor(ItemStack stack) {
        if (stack.isEmpty()) return null;

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return null;

        List<net.minecraft.network.chat.Component> lines = lore.lines();
        if (lines.isEmpty()) return null;

        String lastLine = lines.get(lines.size() - 1).getString().toUpperCase();
        ItemRarityState state = ItemRarityState.INSTANCE;

        if (lastLine.contains("MYTHICAL")) return state.mythicalColor;
        if (lastLine.contains("EPIC")) return state.epicColor;
        if (lastLine.contains("LEGENDARY")) return state.legendaryColor;
        if (lastLine.contains("SCROLL")) return state.scrollColor;
        if (lastLine.contains("RARE")) return state.rareColor;
        if (lastLine.contains("UNCOMMON")) return state.uncommonColor;
        return null;
    }
}
