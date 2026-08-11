package com.runal.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public class Message {

    public static final int SUCCESS = 0x03FF00;
    public static final int ERROR = 0xFF0000;
    public static final int INFO = 0xAAAAAA;

    private static final int[] GRADIENT_STOPS_POS = {0, 50};
    private static final int[] GRADIENT_STOPS_COLOR = {0x005155, 0x458066};

    private static int interpolate(int color1, int color2, float t) {
        int r1 = (color1 >> 16) & 0xFF, g1 = (color1 >> 8) & 0xFF, b1 = color1 & 0xFF;
        int r2 = (color2 >> 16) & 0xFF, g2 = (color2 >> 8) & 0xFF, b2 = color2 & 0xFF;
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        return (r << 16) | (g << 8) | b;
    }

    private static int colorAt(float percent) {
        int lastIndex = GRADIENT_STOPS_POS.length - 1;
        if (percent <= GRADIENT_STOPS_POS[0]) return GRADIENT_STOPS_COLOR[0];
        if (percent >= GRADIENT_STOPS_POS[lastIndex]) return GRADIENT_STOPS_COLOR[lastIndex];

        for (int i = 0; i < lastIndex; i++) {
            int startPos = GRADIENT_STOPS_POS[i];
            int endPos = GRADIENT_STOPS_POS[i + 1];
            if (percent >= startPos && percent <= endPos) {
                float t = (percent - startPos) / (float) (endPos - startPos);
                return interpolate(GRADIENT_STOPS_COLOR[i], GRADIENT_STOPS_COLOR[i + 1], t);
            }
        }
        return GRADIENT_STOPS_COLOR[lastIndex];
    }

    public static MutableComponent gradientText(String text) {
        MutableComponent result = Component.empty();
        int len = text.length();
        for (int i = 0; i < len; i++) {
            float percent = len == 1 ? 0f : (i / (float) (len - 1)) * 100f;
            int color = colorAt(percent);
            result.append(Component.literal(String.valueOf(text.charAt(i)))
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color))));
        }
        return result;
    }

    public static MutableComponent colored(String text, int rgb) {
        return Component.literal(text).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));
    }

    public static void sendRaw(MutableComponent message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        //? if 1.21.4 || 1.21.11 {
        /*mc.player.displayClientMessage(message, false);
        *///?} else {
        mc.player.sendSystemMessage(message);
        //?}
    }

    public static void chat(String statusText, int statusColor) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !RunalSettings.chatNotifications) return;

        MutableComponent message = gradientText("Runal")
                .append(colored(" » ", 0x555555))
                .append(colored(statusText, statusColor));

        //? if 1.21.4 || 1.21.11 {
        /*mc.player.displayClientMessage(message, false);
        *///?} else {
        mc.player.sendSystemMessage(message);
        //?}
    }

    public static void success(String statusText) {
        chat(statusText, SUCCESS);
    }

    public static void error(String statusText) {
        chat(statusText, ERROR);
    }

    public static void info(String statusText) {
        chat(statusText, INFO);
    }

    public static void commandSuccess(String statusText) {
        command(statusText, SUCCESS);
    }

    public static void commandError(String statusText) {
        command(statusText, ERROR);
    }

    public static void commandInfo(String statusText) {
        command(statusText, INFO);
    }

    private static void command(String statusText, int statusColor) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        MutableComponent message = gradientText("Runal")
                .append(colored(" » ", 0x555555))
                .append(colored(statusText, statusColor));

        //? if 1.21.4 || 1.21.11 {
        /*mc.player.displayClientMessage(message, false);
        *///?} else {
        mc.player.sendSystemMessage(message);
        //?}
    }
}
