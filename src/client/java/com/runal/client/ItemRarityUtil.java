package com.runal.client;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

/**
 * Detects item rarity from the item's lore, e.g. "&e&lLEGENDARY WEAPON" or "&e&lLEGENDARY
 * ARMOR" - the server marks rarity there rather than via any data component. Scans from the
 * bottom up and uses the first non-blank line, rather than assuming the very last lore entry
 * is it - some items have a blank trailing line under the rarity text for spacing.
 */
public final class ItemRarityUtil {
    private ItemRarityUtil() {
    }

    public static Integer getRarityColor(ItemStack stack) {
        if (stack.isEmpty()) return null;

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return null;

        List<net.minecraft.network.chat.Component> lines = lore.lines();
        ItemRarityState state = ItemRarityState.INSTANCE;

        for (int i = lines.size() - 1; i >= 0; i--) {
            String line = lines.get(i).getString().trim();
            if (line.isEmpty()) continue;

            String upper = line.toUpperCase();
            if (upper.contains("MYTHICAL")) return state.mythicalColor;
            if (upper.contains("EPIC")) return state.epicColor;
            if (upper.contains("LEGENDARY")) return state.legendaryColor;
            if (upper.contains("SCROLL")) return state.scrollColor;
            if (upper.contains("RARE")) return state.rareColor;
            if (upper.contains("UNCOMMON")) return state.uncommonColor;
            // First non-blank line from the bottom didn't match any rarity word - stop here
            // rather than keep scanning upward into flavor text that might coincidentally
            // contain one of these words (e.g. "prepare" contains "rare").
            return null;
        }
        return null;
    }
}
