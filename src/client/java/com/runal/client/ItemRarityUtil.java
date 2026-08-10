package com.runal.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
//? if 26.2 {
/*import net.minecraft.network.chat.TextColor;
*///?}

/**
 * Detects item rarity from the item's own display-name color, not its lore - the server colors
 * the plain (non-bold) name itself with the rarity's legacy code, e.g. &e for Legendary, &2 for
 * Scroll. Same style-comparison approach AutoGGController already uses for chat rarity, since
 * that's proven to correctly read these exact legacy color codes on this server.
 */
public final class ItemRarityUtil {
    private ItemRarityUtil() {
    }

    public static Integer getRarityColor(ItemStack stack) {
        if (stack.isEmpty()) return null;

        Style style = stack.getHoverName().getStyle();
        ItemRarityState state = ItemRarityState.INSTANCE;

        if (isColor(style, ChatFormatting.LIGHT_PURPLE)) return state.mythicalColor;
        if (isColor(style, ChatFormatting.DARK_PURPLE)) return state.epicColor;
        if (isColor(style, ChatFormatting.YELLOW)) return state.legendaryColor;
        if (isColor(style, ChatFormatting.DARK_GREEN)) return state.scrollColor;
        if (isColor(style, ChatFormatting.BLUE)) return state.rareColor;
        if (isColor(style, ChatFormatting.GREEN)) return state.uncommonColor;
        return null;
    }

    private static boolean isColor(Style style, ChatFormatting expected) {
        //? if 1.21.4 || 1.21.11 || 26.1.2 {
        return style.getColor() != null
                && expected.getColor() != null
                && style.getColor().getValue() == expected.getColor();
        //?}
        //? if 26.2 {
        /*TextColor expectedColor = TextColor.fromLegacyFormat(expected);
        return style.getColor() != null
                && expectedColor != null
                && style.getColor().getValue() == expectedColor.getValue();
        *///?}
    }
}
