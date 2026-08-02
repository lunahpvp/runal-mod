package com.runal.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
//? if 1.21.4 {
//?} else {
import net.minecraft.client.input.MouseButtonEvent;
//?}
import net.minecraft.network.chat.Component;

public class HudEditorScreen extends Screen {
    private static final int GRID_SIZE = 8;

    private String dragging;
    private int dragOffsetX;
    private int dragOffsetY;

    public HudEditorScreen() {
        super(Component.literal("HUD Editor"));
    }

    //? if 1.21.4 || 1.21.11 {
    /*@Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        renderContent(context, mouseX, mouseY, deltaTicks);
        super.render(context, mouseX, mouseY, deltaTicks);
    }
    *///?} else {
    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        renderContent(context, mouseX, mouseY, deltaTicks);
        super.extractRenderState(context, mouseX, mouseY, deltaTicks);
    }
    //?}

    private void renderContent(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        context.fillGradient(0, 0, width, height, 0xBB000000, 0xDD000000);
        drawGrid(context);
        context.centeredText(font, "HUD Editor", width / 2, 12, 0xFFFFFFFF);
        context.centeredText(font, "Drag widgets. Press Escape to close.", width / 2, 24, 0xFFA7A8B2);

        drawWidget(context, "Session", SessionManagerState.x, SessionManagerState.y, 112, 24, SessionManagerState.widgetColor);
        drawWidget(context, "Performance", PerformanceHudState.x, PerformanceHudState.y, 112, 46, 0xAA101216);
        drawWidget(context, "Armor", ArmorHudState.x, ArmorHudState.y, UtilityHudRenderer.armorHudWidth(), UtilityHudRenderer.armorHudHeight(), 0xAA101216);
        drawWidget(context, "Inventory", InventoryHudState.x, InventoryHudState.y, InventoryHudState.WIDTH, InventoryHudState.HEIGHT, 0x55101216);
        drawWidget(context, "Events", EventTrackerState.x, EventTrackerState.y, 100, 40, 0xAA101216);
        drawWidget(context, "Cooldowns", ItemCooldownHudState.x, ItemCooldownHudState.y, 100, 40, 0xAA101216);
        drawWidget(context, "Dungeon Tracker", DungeonTrackerState.x, DungeonTrackerState.y, 100, 62, 0xAA101216);
        drawWidget(context, "Boss Defeats", BossDefeatState.x, BossDefeatState.y, 100, 16, 0xAA101216);

        String lowTitlePreview = LowHealthWarning.lowHpTitle.isEmpty() ? "LOW HP" : LowHealthWarning.lowHpTitle;
        int lowTitleW = (int) (font.width(lowTitlePreview) * UtilityHudRenderer.WARNING_TITLE_SCALE);
        int lowTitleH = (int) (font.lineHeight * UtilityHudRenderer.WARNING_TITLE_SCALE);
        drawWidget(context, "Low HP Title", LowHealthWarning.lowTitleX, LowHealthWarning.lowTitleY, lowTitleW, lowTitleH, 0xAA101216);

        String midTitlePreview = LowHealthWarning.midHpTitle.isEmpty() ? "MID HP" : LowHealthWarning.midHpTitle;
        int midTitleW = (int) (font.width(midTitlePreview) * UtilityHudRenderer.WARNING_TITLE_SCALE);
        int midTitleH = (int) (font.lineHeight * UtilityHudRenderer.WARNING_TITLE_SCALE);
        drawWidget(context, "Mid HP Title", LowHealthWarning.midTitleX, LowHealthWarning.midTitleY, midTitleW, midTitleH, 0xAA101216);

        BossTitleState.ensureDefaultPosition(width, height);
        String bossTitlePreview = BossTitleState.currentText != null ? BossTitleState.currentText : "Flame Prison!";
        int bossTitleW = (int) (font.width(bossTitlePreview) * BossTitleState.scale);
        int bossTitleH = (int) (font.lineHeight * BossTitleState.scale);
        drawWidget(context, "Boss Title", BossTitleState.x - bossTitleW / 2, BossTitleState.y - bossTitleH / 2, bossTitleW, bossTitleH, 0xAA101216);

        UtilityHudRenderer.renderPreview(context);
    }

    private void drawGrid(GuiGraphicsExtractor context) {
        int color = 0x22FFFFFF;
        for (int x = 0; x < width; x += GRID_SIZE) {
            context.fill(x, 0, x + 1, height, color);
        }
        for (int y = 0; y < height; y += GRID_SIZE) {
            context.fill(0, y, width, y + 1, color);
        }
    }

    private static int snap(int value) {
        return Math.round(value / (float) GRID_SIZE) * GRID_SIZE;
    }

    private void drawWidget(GuiGraphicsExtractor context, String name, int x, int y, int w, int h, int color) {
        context.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xAA35D77A);
        context.fill(x, y, x + w, y + h, color);
        context.text(font, name, x + 4, y + 4, 0xFFFFFFFF, true);
    }

    //? if 1.21.4 {
    /*@Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (handleMouseClicked((int) mouseX, (int) mouseY)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }
    *///?} else {
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (handleMouseClicked((int) event.x(), (int) event.y())) return true;
        return super.mouseClicked(event, doubled);
    }
    //?}

    private boolean handleMouseClicked(int mouseX, int mouseY) {
        if (inside(mouseX, mouseY, SessionManagerState.x, SessionManagerState.y, 112, 24)) return startDrag("session", mouseX, mouseY, SessionManagerState.x, SessionManagerState.y);
        if (inside(mouseX, mouseY, PerformanceHudState.x, PerformanceHudState.y, 112, 46)) return startDrag("performance", mouseX, mouseY, PerformanceHudState.x, PerformanceHudState.y);
        int armorW = UtilityHudRenderer.armorHudWidth();
        int armorH = UtilityHudRenderer.armorHudHeight();
        if (inside(mouseX, mouseY, ArmorHudState.x, ArmorHudState.y, armorW, armorH)) return startDrag("armor", mouseX, mouseY, ArmorHudState.x, ArmorHudState.y);
        if (inside(mouseX, mouseY, InventoryHudState.x, InventoryHudState.y, InventoryHudState.WIDTH, InventoryHudState.HEIGHT)) return startDrag("inventory", mouseX, mouseY, InventoryHudState.x, InventoryHudState.y);
        if (inside(mouseX, mouseY, EventTrackerState.x, EventTrackerState.y, 100, 40)) return startDrag("events", mouseX, mouseY, EventTrackerState.x, EventTrackerState.y);
        if (inside(mouseX, mouseY, ItemCooldownHudState.x, ItemCooldownHudState.y, 100, 40)) return startDrag("itemCooldowns", mouseX, mouseY, ItemCooldownHudState.x, ItemCooldownHudState.y);
        if (inside(mouseX, mouseY, DungeonTrackerState.x, DungeonTrackerState.y, 100, 62)) return startDrag("dungeonTracker", mouseX, mouseY, DungeonTrackerState.x, DungeonTrackerState.y);
        if (inside(mouseX, mouseY, BossDefeatState.x, BossDefeatState.y, 100, 16)) return startDrag("bossDefeats", mouseX, mouseY, BossDefeatState.x, BossDefeatState.y);

        String lowTitlePreview = LowHealthWarning.lowHpTitle.isEmpty() ? "LOW HP" : LowHealthWarning.lowHpTitle;
        int lowTitleW = (int) (font.width(lowTitlePreview) * UtilityHudRenderer.WARNING_TITLE_SCALE);
        int lowTitleH = (int) (font.lineHeight * UtilityHudRenderer.WARNING_TITLE_SCALE);
        if (inside(mouseX, mouseY, LowHealthWarning.lowTitleX, LowHealthWarning.lowTitleY, lowTitleW, lowTitleH)) return startDrag("lowTitle", mouseX, mouseY, LowHealthWarning.lowTitleX, LowHealthWarning.lowTitleY);

        String midTitlePreview = LowHealthWarning.midHpTitle.isEmpty() ? "MID HP" : LowHealthWarning.midHpTitle;
        int midTitleW = (int) (font.width(midTitlePreview) * UtilityHudRenderer.WARNING_TITLE_SCALE);
        int midTitleH = (int) (font.lineHeight * UtilityHudRenderer.WARNING_TITLE_SCALE);
        if (inside(mouseX, mouseY, LowHealthWarning.midTitleX, LowHealthWarning.midTitleY, midTitleW, midTitleH)) return startDrag("midTitle", mouseX, mouseY, LowHealthWarning.midTitleX, LowHealthWarning.midTitleY);

        BossTitleState.ensureDefaultPosition(width, height);
        String bossTitlePreview = BossTitleState.currentText != null ? BossTitleState.currentText : "Flame Prison!";
        int bossTitleW = (int) (font.width(bossTitlePreview) * BossTitleState.scale);
        int bossTitleH = (int) (font.lineHeight * BossTitleState.scale);
        if (inside(mouseX, mouseY, BossTitleState.x - bossTitleW / 2, BossTitleState.y - bossTitleH / 2, bossTitleW, bossTitleH)) return startDrag("bossTitle", mouseX, mouseY, BossTitleState.x, BossTitleState.y);

        return false;
    }

    private boolean startDrag(String id, int mouseX, int mouseY, int x, int y) {
        dragging = id;
        dragOffsetX = mouseX - x;
        dragOffsetY = mouseY - y;
        return true;
    }

    private boolean inside(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    //? if 1.21.4 {
    /*@Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (handleMouseDragged((int) mouseX, (int) mouseY)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    *///?} else {
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (handleMouseDragged((int) event.x(), (int) event.y())) return true;
        return super.mouseDragged(event, dragX, dragY);
    }
    //?}

    private boolean handleMouseDragged(int mouseX, int mouseY) {
        if (dragging == null) return false;
        int x = snap(mouseX - dragOffsetX);
        int y = snap(mouseY - dragOffsetY);
        if ("session".equals(dragging)) { SessionManagerState.x = x; SessionManagerState.y = y; }
        if ("performance".equals(dragging)) { PerformanceHudState.x = x; PerformanceHudState.y = y; }
        if ("armor".equals(dragging)) { ArmorHudState.x = x; ArmorHudState.y = y; }
        if ("inventory".equals(dragging)) { InventoryHudState.x = x; InventoryHudState.y = y; }
        if ("events".equals(dragging)) { EventTrackerState.x = x; EventTrackerState.y = y; }
        if ("itemCooldowns".equals(dragging)) { ItemCooldownHudState.x = x; ItemCooldownHudState.y = y; }
        if ("dungeonTracker".equals(dragging)) { DungeonTrackerState.x = x; DungeonTrackerState.y = y; }
        if ("lowTitle".equals(dragging)) { LowHealthWarning.lowTitleX = x; LowHealthWarning.lowTitleY = y; }
        if ("midTitle".equals(dragging)) { LowHealthWarning.midTitleX = x; LowHealthWarning.midTitleY = y; }
        if ("bossTitle".equals(dragging)) { BossTitleState.x = x; BossTitleState.y = y; }
        if ("bossDefeats".equals(dragging)) { BossDefeatState.x = x; BossDefeatState.y = y; }
        ModuleConfig.save();
        return true;
    }

    //? if 1.21.4 {
    /*@Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        handleMouseReleased();
        return super.mouseReleased(mouseX, mouseY, button);
    }
    *///?} else {
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        handleMouseReleased();
        return super.mouseReleased(event);
    }
    //?}

    private void handleMouseReleased() {
        dragging = null;
        ModuleConfig.save();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
