package com.runal.client;

public final class ItemRarityState {
    public static final ItemRarityState INSTANCE = new ItemRarityState();

    private boolean enabled;

    public boolean showInInventory = true;
    public boolean showInHotbar = true;
    public float thickness = 1f;

    // Defaults match the vanilla legacy formatting codes the server itself uses for each
    // rarity's lore line (&d mythical, &5 epic, &e legendary, &2 scroll, &9 rare, &a uncommon).
    public int mythicalColor = 0xFFFF55FF;
    public int epicColor = 0xFFAA00AA;
    public int legendaryColor = 0xFFFFFF55;
    public int scrollColor = 0xFF00AA00;
    public int rareColor = 0xFF5555FF;
    public int uncommonColor = 0xFF55FF55;

    private ItemRarityState() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
    }
}
