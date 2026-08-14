package com.runal.client;

public class SemiramisTitleState {
    public static int x = Integer.MIN_VALUE;
    public static int y = Integer.MIN_VALUE;
    public static int textColor = 0xFF7CFFB2;
    public static float scale = 1.0f;

    public static String currentText;
    public static int displayTicksRemaining;

    public static void ensureDefaultPosition(int screenWidth, int screenHeight) {
        if (x == Integer.MIN_VALUE) x = screenWidth / 2;
        if (y == Integer.MIN_VALUE) y = screenHeight / 2 + 50;
    }
}
