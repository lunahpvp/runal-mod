package com.runal.client;

import net.minecraft.ChatFormatting;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
//? if 26.2 {
/*import net.minecraft.network.chat.TextColor;
*///?}

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AutoGGController {

    private static final String FOUND_MARKER = "has found ";
    private static final String ACHIEVEMENT_MARKER = "has completed the achievement ";

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!AutoGGState.INSTANCE.isEnabled()) return;
            if (overlay) return;

            if (!shouldTrigger(message)) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.getConnection() == null) return;

            String response = AutoGGState.INSTANCE.response.trim();
            if (response.isEmpty()) return;

            mc.getConnection().sendChat(response);
        });
    }

    private static boolean shouldTrigger(Component message) {
        List<StyledRun> runs = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        List<int[]> ranges = new ArrayList<>();

        message.visit((style, text) -> {
            int start = sb.length();
            sb.append(text);
            ranges.add(new int[]{start, sb.length()});
            runs.add(new StyledRun(text, style));
            return Optional.<Void>empty();
        }, Style.EMPTY);

        String fullText = sb.toString().replaceAll("\\p{Zs}", " ");

        if (fullText.contains("[G]")) return false;

        if (fullText.contains("|")) {
            if (fullText.contains(ACHIEVEMENT_MARKER)) return true;
            return matchesFoundRarity(fullText, runs, ranges);
        }

        return matchesFoundRarity(fullText, runs, ranges);
    }

    private static boolean matchesFoundRarity(String fullText, List<StyledRun> runs, List<int[]> ranges) {
        int foundIdx = fullText.indexOf(FOUND_MARKER);
        if (foundIdx < 0) return false;
        int itemStart = foundIdx + FOUND_MARKER.length();

        boolean matchesRarity = false;
        String itemSegment = fullText.substring(itemStart);
        if (containsLegacyCode(itemSegment, '6')) matchesRarity = AutoGGState.INSTANCE.triggerLegendary;
        else if (containsLegacyCode(itemSegment, '5')) matchesRarity = AutoGGState.INSTANCE.triggerEpic;
        else if (containsLegacyCode(itemSegment, 'd')) matchesRarity = AutoGGState.INSTANCE.triggerMythical;
        else {
            Style itemStyle = null;
            for (int i = 0; i < ranges.size(); i++) {
                int[] range = ranges.get(i);
                if (itemStart >= range[0] && itemStart < range[1]) {
                    itemStyle = runs.get(i).style;
                    break;
                }
            }
            if (itemStyle != null) {
                if (isRarity(itemStyle, ChatFormatting.GOLD)) matchesRarity = AutoGGState.INSTANCE.triggerLegendary;
                else if (isRarity(itemStyle, ChatFormatting.DARK_PURPLE)) matchesRarity = AutoGGState.INSTANCE.triggerEpic;
                else if (isRarity(itemStyle, ChatFormatting.LIGHT_PURPLE)) matchesRarity = AutoGGState.INSTANCE.triggerMythical;
            }
        }

        return matchesRarity;
    }

    private static boolean containsLegacyCode(String text, char colorCode) {
        String lower = text.toLowerCase();
        return lower.contains("§" + colorCode);
    }

    private static boolean isRarity(Style style, ChatFormatting expected) {
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

    private record StyledRun(String text, Style style) {
    }
}
