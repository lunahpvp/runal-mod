package com.runal.client;

import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Temporary diagnostic logging for Item Rarity - logs each unique item once so we can see
 * whether the render injection is actually firing at all, and what color (if any) is being
 * read off it, instead of guessing blind again. Safe to remove once rarity detection is
 * confirmed working.
 */
public final class ItemRarityDebug {
    private static final Logger LOGGER = LoggerFactory.getLogger("Runal ItemRarity");
    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();

    private ItemRarityDebug() {
    }

    public static void log(String surface, ItemStack stack) {
        if (stack.isEmpty()) return;

        String name = stack.getHoverName().getString();
        String key = surface + ":" + name;
        if (!LOGGED.add(key)) return;

        Style style = stack.getHoverName().getStyle();
        String colorText = style.getColor() != null
                ? Integer.toHexString(style.getColor().getValue())
                : "null";

        Integer rarityColor = ItemRarityUtil.getRarityColor(stack);
        LOGGER.info(
                "[{}] name=\"{}\" nameColor=0x{} rarityMatch={}",
                surface,
                name,
                colorText,
                rarityColor != null ? Integer.toHexString(rarityColor) : "none"
        );
    }
}
