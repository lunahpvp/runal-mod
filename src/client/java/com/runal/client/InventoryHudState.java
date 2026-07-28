package com.runal.client;

public final class InventoryHudState {
    public static final int COLUMNS = 9;
    public static final int ROWS = 3;
    public static final int SLOT_SPACING = 18;
    public static final int WIDTH = COLUMNS * SLOT_SPACING;
    public static final int HEIGHT = ROWS * SLOT_SPACING;

    public static boolean enabled = false;
    public static int x = 8;
    public static int y = 104;

    private InventoryHudState() {
    }
}
