package com.runal.client;

import java.util.LinkedHashMap;
import java.util.Map;

public class RunalChatState {
    public static boolean enabled = true;

    public static final Map<String, Integer> CHAT_COLORS = new LinkedHashMap<>();
    static {
        CHAT_COLORS.put("1", 0x0000AA);
        CHAT_COLORS.put("2", 0x00AA00);
        CHAT_COLORS.put("3", 0x00AAAA);
        CHAT_COLORS.put("4", 0xAA0000);
        CHAT_COLORS.put("5", 0xAA00AA);
        CHAT_COLORS.put("6", 0xFFAA00);
        CHAT_COLORS.put("7", 0xAAAAAA);
        CHAT_COLORS.put("8", 0x555555);
        CHAT_COLORS.put("9", 0x5555FF);
        CHAT_COLORS.put("a", 0x55FF55);
        CHAT_COLORS.put("e", 0xFFFF55);
        CHAT_COLORS.put("f", 0xFFFFFF);
    }

    public static String chatColor = "f";

    public static int chatColorRgb(String code) {
        return CHAT_COLORS.getOrDefault(code, 0xFFFFFF);
    }
}
