package com.runal.client.emoji;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.Optional;

public final class EmojiReplacer {
    private EmojiReplacer() {
    }

    public static Component replaceIn(Component input) {
        if (input == null || !input.getString().contains(":")) return input;

        MutableComponent result = Component.empty();
        input.visit((style, text) -> {
            appendWithEmojis(result, text, style);
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }

    private static void appendWithEmojis(MutableComponent result, String text, Style style) {
        int plainStart = 0;
        int index = 0;

        while (index < text.length()) {
            if (text.charAt(index) != ':') {
                index++;
                continue;
            }

            int closingColon = findClosingColon(text, index);
            if (closingColon < 0) {
                index++;
                continue;
            }

            String shortcode = text.substring(index, closingColon + 1);
            String glyph = EmojiShortcodes.mappings().get(shortcode);
            if (glyph == null) {
                index++;
                continue;
            }

            if (plainStart < index) {
                result.append(Component.literal(text.substring(plainStart, index)).withStyle(style));
            }
            result.append(Component.literal(glyph).withStyle(style.withColor(0xFFFFFF)));
            index = closingColon + 1;
            plainStart = index;
        }

        if (plainStart < text.length()) {
            result.append(Component.literal(text.substring(plainStart)).withStyle(style));
        }
    }

    private static int findClosingColon(String text, int openingColon) {
        int end = Math.min(text.length(), openingColon + 32);
        for (int index = openingColon + 1; index < end; index++) {
            char character = text.charAt(index);
            if (character == ':') return index;
            if (!(Character.isLetterOrDigit(character) || character == '_')) return -1;
        }
        return -1;
    }
}
