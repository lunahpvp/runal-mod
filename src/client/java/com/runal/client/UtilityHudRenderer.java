package com.runal.client;

//? if 1.21.4 || 1.21.11 {
/*import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
*///?} else {
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
//?}
import net.minecraft.client.Minecraft;
//? if 1.21.4 || 1.21.11 {
//?} else {
import net.minecraft.resources.Identifier;
//?}
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UtilityHudRenderer {
    private static final int TRACKING_FONT_SIZE = 6;
    private static final int COOLDOWN_FONT_SIZE = 8;
    private static final int UTILITY_FONT_SIZE = 10;
    private static final int NEUTRAL_PANEL_COLOR = 0xD0000000;

    public static void register() {
        //? if 1.21.4 || 1.21.11 {
        /*HudRenderCallback.EVENT.register(UtilityHudRenderer::render);
        *///?} else {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("runal", "utility_huds"), UtilityHudRenderer::render);
        //?}
    }

    private static void render(net.minecraft.client.gui.GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        renderPreview(graphics);
    }

    public static void renderPreview(net.minecraft.client.gui.GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();

        if (SessionManagerState.enabled && SessionManagerState.showHud) {
            long seconds = (System.currentTimeMillis() - SessionManagerState.startTimeMs) / 1000L;
            drawTrackingPanel(graphics, mc, "Session", SessionManagerState.x, SessionManagerState.y,
                    List.of(new TrackingLine("Playtime", formatTime(seconds), SessionManagerState.labelColor, SessionManagerState.valueColor)),
                    SessionManagerState.widgetColor);
        }

        if (PerformanceHudState.enabled) {
            List<TrackingLine> lines = new ArrayList<>();
            if (PerformanceHudState.fps) lines.add(new TrackingLine("FPS", String.valueOf(mc.getFps()), PerformanceHudState.nameColor, PerformanceHudState.valueColor));
            if (PerformanceHudState.tps) lines.add(new TrackingLine("TPS", "20.0", PerformanceHudState.nameColor, PerformanceHudState.valueColor));
            if (PerformanceHudState.ping && mc.player != null && mc.getConnection() != null && mc.getConnection().getPlayerInfo(mc.player.getUUID()) != null) {
                lines.add(new TrackingLine("Ping", mc.getConnection().getPlayerInfo(mc.player.getUUID()).getLatency() + "ms", PerformanceHudState.nameColor, PerformanceHudState.valueColor));
            }
            if (PerformanceHudState.direction && mc.player != null) lines.add(new TrackingLine("Direction", mc.player.getDirection().getName(), PerformanceHudState.nameColor, PerformanceHudState.valueColor));
            if (!lines.isEmpty()) drawTrackingPanel(graphics, mc, "Performance", PerformanceHudState.x, PerformanceHudState.y, lines, PerformanceHudState.nameColor);
        }

        if (ArmorHudState.enabled && ArmorHudState.showHud && mc.player != null) {
            drawArmor(graphics, mc);
        }

        if (InventoryHudState.enabled && mc.player != null) {
            drawInventory(graphics, mc);
        }

        drawCooldowns(graphics, mc);

        if (EventTrackerState.enabled && !EventTrackerState.events.isEmpty()) {
            List<TrackingLine> lines = new ArrayList<>();
            for (EventTrackerState.TrackedEvent event : EventTrackerState.events.values()) {
                String value = event.remainingSeconds > 0 ? EventTrackerState.formatTime(event.remainingSeconds) : "Active";
                lines.add(new TrackingLine(event.name, value, EventTrackerState.nameColor, EventTrackerState.valueColor));
            }
            drawTrackingPanel(graphics, mc, "Events", EventTrackerState.x, EventTrackerState.y, lines, EventTrackerState.nameColor);
        }

        if (DungeonTrackerState.enabled && DungeonTrackerState.dungeonName != null) {
            drawDungeonTracker(graphics, mc);
        }

        String activeTitleKind = LowHealthWarning.getActiveTitleKind();
        if ("low".equals(activeTitleKind) && !LowHealthWarning.lowHpTitle.isEmpty()) {
            drawWarningTitle(graphics, mc, LowHealthWarning.lowHpTitle, LowHealthWarning.lowTitleX, LowHealthWarning.lowTitleY);
        } else if ("mid".equals(activeTitleKind) && !LowHealthWarning.midHpTitle.isEmpty()) {
            drawWarningTitle(graphics, mc, LowHealthWarning.midHpTitle, LowHealthWarning.midTitleX, LowHealthWarning.midTitleY);
        }

        if (BossTitleState.enabled && BossTitleState.currentText != null) {
            BossTitleState.ensureDefaultPosition(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
            drawBossTitle(graphics, mc);
        }

        if (BossDefeatState.enabled && BossTitleState.isFightingBoss()) {
            drawBossDefeatCounter(graphics, mc);
        }
    }

    public static final float WARNING_TITLE_SCALE = 2.5f;

    private static void drawWarningTitle(net.minecraft.client.gui.GuiGraphicsExtractor graphics, Minecraft mc, String title, int x, int y) {
        int size = Math.max(UTILITY_FONT_SIZE, Math.round(mc.font.lineHeight * WARNING_TITLE_SCALE));
        PortableTextRenderer.draw(graphics, title, x, y, size, 0xFFFF3B3B);
    }

    private static void drawBossTitle(net.minecraft.client.gui.GuiGraphicsExtractor graphics, Minecraft mc) {
        int size = Math.max(UTILITY_FONT_SIZE, Math.round(mc.font.lineHeight * BossTitleState.scale));
        int textWidth = PortableTextRenderer.width(BossTitleState.currentText, size);
        int textHeight = PortableTextRenderer.height(BossTitleState.currentText, size);
        PortableTextRenderer.draw(
                graphics,
                BossTitleState.currentText,
                BossTitleState.x - textWidth / 2,
                BossTitleState.y - textHeight / 2,
                size,
                BossTitleState.textColor
        );
    }

    private static void drawBossDefeatCounter(net.minecraft.client.gui.GuiGraphicsExtractor graphics, Minecraft mc) {
        String bossName = BossTitleState.lastBossName;
        String value = String.valueOf(BossDefeatState.getCount(bossName));

        drawTrackingPanel(graphics, mc, "Boss Defeats", BossDefeatState.x, BossDefeatState.y,
                List.of(new TrackingLine(bossName, value, BossDefeatState.nameColor, BossDefeatState.valueColor)),
                BossDefeatState.nameColor);
    }

    private static void drawTrackingPanel(net.minecraft.client.gui.GuiGraphicsExtractor graphics, Minecraft mc, String title,
                                          int x, int y, List<TrackingLine> lines, int accentColor) {
        drawTrackingPanel(graphics, mc, title, x, y, lines, accentColor, TRACKING_FONT_SIZE);
    }

    private static void drawTrackingPanel(net.minecraft.client.gui.GuiGraphicsExtractor graphics, Minecraft mc, String title,
                                          int x, int y, List<TrackingLine> lines, int accentColor, int fontSize) {
        int width = PortableTextRenderer.width(title, fontSize) + 12;
        for (TrackingLine line : lines) {
            width = Math.max(width,
                    PortableTextRenderer.width(line.label(), fontSize)
                            + PortableTextRenderer.width(line.value(), fontSize) + 12);
        }

        int textHeight = PortableTextRenderer.height("Ag", fontSize);
        int lineSpacing = textHeight + 1;
        int height = textHeight + 3 + lines.size() * lineSpacing;
        int borderColor = 0xFF000000 | (accentColor & 0x00FFFFFF);
        drawTrackingFrame(graphics, title, x, y, width, height, borderColor, fontSize);
        int lineY = y + textHeight + 1;
        for (TrackingLine line : lines) {
            PortableTextRenderer.draw(graphics, line.label(), x + 4, lineY, fontSize, line.labelColor());
            int valueWidth = PortableTextRenderer.width(line.value(), fontSize);
            PortableTextRenderer.draw(graphics, line.value(), x + width - valueWidth - 4, lineY, fontSize, line.valueColor());
            lineY += lineSpacing;
        }
    }

    private static void drawTrackingFrame(
            net.minecraft.client.gui.GuiGraphicsExtractor graphics,
            String title,
            int x,
            int y,
            int width,
            int height,
            int borderColor,
            int fontSize
    ) {
        graphics.fill(x + 1, y, x + width - 1, y + height, NEUTRAL_PANEL_COLOR);
        graphics.fill(x, y + 1, x + 1, y + height - 1, NEUTRAL_PANEL_COLOR);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, NEUTRAL_PANEL_COLOR);

        int textHeight = PortableTextRenderer.height("Ag", fontSize);
        int titleMidY = y + textHeight / 2;
        int titleGapEnd = x + PortableTextRenderer.width(title, fontSize) + 9;
        graphics.fill(x + 2, titleMidY, x + 6, titleMidY + 1, borderColor);
        graphics.fill(titleGapEnd, titleMidY, x + width - 2, titleMidY + 1, borderColor);
        graphics.fill(x + 2, y + height - 2, x + width - 2, y + height - 1, borderColor);
        graphics.fill(x + 1, titleMidY, x + 2, y + height - 2, borderColor);
        graphics.fill(x + width - 2, titleMidY, x + width - 1, y + height - 2, borderColor);
        PortableTextRenderer.draw(graphics, title, x + 6, y, fontSize, borderColor);
    }

    private record CooldownEntry(String name, String value, int nameColor, int valueColor) {
    }

    private record TrackingLine(String label, String value, int labelColor, int valueColor) {
    }

    private static boolean showSeconds() {
        return "Seconds".equals(RunalSettings.cooldownDisplayMode);
    }

    private static String formatTicksAsSeconds(int ticks) {
        return ((ticks + 19) / 20) + "s";
    }

    private static void drawCooldowns(net.minecraft.client.gui.GuiGraphicsExtractor graphics, Minecraft mc) {
        List<CooldownEntry> entries = new ArrayList<>();
        boolean seconds = showSeconds();

        if (ItemCooldownHudState.enabled && mc.player != null && mc.player.containerMenu == mc.player.inventoryMenu) {
            Set<String> seenNames = new HashSet<>();
            Set<String> activeKeys = new HashSet<>();
            long nowTick = mc.player.tickCount;
            for (int slotId = InventoryMenu.INV_SLOT_START; slotId < InventoryMenu.USE_ROW_SLOT_END; slotId++) {
                Slot slot = mc.player.containerMenu.getSlot(slotId);
                if (!slot.hasItem()) continue;

                ItemStack stack = slot.getItem();
                if (!mc.player.getCooldowns().isOnCooldown(stack)) continue;

                String name = stack.getHoverName().getString();
                if (!seenNames.add(name)) continue;

                float percent = mc.player.getCooldowns().getCooldownPercent(stack, 1.0f);
                String key = "item:" + name;
                activeKeys.add(key);
                String value = valueFor(seconds, percent, key, nowTick);
                // Use the item's own real name color (its actual rarity color, straight from the
                // game) instead of one fixed color for every item, falling back to the configured
                // default only for items with no explicit color of their own.
                var itemColor = stack.getHoverName().getStyle().getColor();
                int nameColor = itemColor != null ? (0xFF000000 | itemColor.getValue()) : ItemCooldownHudState.nameColor;
                entries.add(new CooldownEntry(name, value, nameColor, ItemCooldownHudState.valueColor));
            }
            CooldownDurationEstimator.pruneExcept("item:", activeKeys);
        }

        if (ArmorCooldownHudState.enabled) {
            for (int i = 0; i < ArmorCooldownHudState.names.size(); i++) {
                int percent = ArmorCooldownHudState.percents.get(i);
                String value = seconds && ArmorCooldownHudState.secondsRemaining.get(i) != null
                        ? ArmorCooldownHudState.secondsRemaining.get(i) + "s"
                        : percent + "%";
                entries.add(new CooldownEntry(ArmorCooldownHudState.names.get(i), value, ArmorCooldownHudState.nameColor, ArmorCooldownHudState.valueColor));
            }
        }

        if (WeaponCooldownState.enabled) {
            for (Map.Entry<String, WeaponCooldownState.ActiveCooldown> entry : WeaponCooldownState.active.entrySet()) {
                WeaponCooldownState.ActiveCooldown cooldown = entry.getValue();
                String value;
                if (seconds) {
                    value = cooldown.remainingSecondsCeil() + "s";
                } else {
                    int percent = Math.round((cooldown.remainingTicks / (float) cooldown.totalTicks) * 100f);
                    value = percent + "%";
                }
                entries.add(new CooldownEntry(entry.getKey(), value, WeaponCooldownState.nameColor, WeaponCooldownState.valueColor));
            }
        }

        if (AccessoryCooldownState.enabled) {
            for (Map.Entry<String, AccessoryCooldownState.ActiveCooldown> entry : AccessoryCooldownState.active.entrySet()) {
                AccessoryCooldownState.ActiveCooldown cooldown = entry.getValue();
                String value;
                if (seconds) {
                    value = cooldown.remainingSecondsCeil() + "s";
                } else {
                    int percent = Math.round((cooldown.remainingTicks / (float) cooldown.totalTicks) * 100f);
                    value = percent + "%";
                }
                entries.add(new CooldownEntry(entry.getKey(), value, AccessoryCooldownState.nameColor, AccessoryCooldownState.valueColor));
            }
        }

        if (entries.isEmpty()) return;

        List<TrackingLine> lines = new ArrayList<>(entries.size());
        for (CooldownEntry entry : entries) {
            lines.add(new TrackingLine(entry.name(), entry.value(), entry.nameColor(), entry.valueColor()));
        }
        drawTrackingPanel(
                graphics,
                mc,
                "Cooldowns",
                ItemCooldownHudState.x,
                ItemCooldownHudState.y,
                lines,
                entries.get(0).nameColor(),
                COOLDOWN_FONT_SIZE
        );
    }

    private static String valueFor(boolean seconds, float percent, String key, long nowTick) {
        if (!seconds) return Math.round(percent * 100f) + "%";
        Integer remainingTicks = CooldownDurationEstimator.estimateRemainingTicks(key, percent, nowTick);
        return remainingTicks != null ? formatTicksAsSeconds(remainingTicks) : Math.round(percent * 100f) + "%";
    }

    private static void drawDungeonTracker(net.minecraft.client.gui.GuiGraphicsExtractor graphics, Minecraft mc) {
        int roomsUntilParkour = DungeonTrackerController.roomsUntil("Parkour");
        int roomsUntilBoss = DungeonTrackerController.roomsUntil("Boss");
        int roomsUntilTreasure = DungeonTrackerController.roomsUntil("Treasure");

        String roomStr = String.valueOf(DungeonTrackerState.currentRoom);
        String parkourStr = roomsUntilParkour == 0 ? "Now" : roomsUntilParkour + " rooms";
        String bossStr = roomsUntilBoss == 0 ? "Now" : roomsUntilBoss + " rooms";
        String chestStr = roomsUntilTreasure == 0 ? "Now" : roomsUntilTreasure + " rooms";
        int color = DungeonTrackerState.themeColor != null ? DungeonTrackerState.themeColor : DungeonTrackerState.nameColor;
        int valueColor = DungeonTrackerState.themeColor != null ? DungeonTrackerState.themeColor : DungeonTrackerState.valueColor;

        drawTrackingPanel(graphics, mc, "Dungeon", DungeonTrackerState.x, DungeonTrackerState.y, List.of(
                new TrackingLine("Dungeon", DungeonTrackerState.dungeonName, color, valueColor),
                new TrackingLine("Room", roomStr, color, valueColor),
                new TrackingLine("Parkour", parkourStr, color, valueColor),
                new TrackingLine(DungeonTrackerState.bossName, bossStr, color, valueColor),
                new TrackingLine("Treasure Chest", chestStr, color, valueColor)
        ), color);
    }

    private static void drawArmor(net.minecraft.client.gui.GuiGraphicsExtractor graphics, Minecraft mc) {
        EquipmentSlot[] slots = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
        int x = ArmorHudState.x;
        int y = ArmorHudState.y;
        boolean vertical = "Vertical".equals(ArmorHudState.orientation);
        int contentWidth = vertical ? 16 : 76;
        int contentHeight = vertical ? 76 : 16;
        int textHeight = PortableTextRenderer.height("Ag", TRACKING_FONT_SIZE);
        int w = Math.max(PortableTextRenderer.width("Armor", TRACKING_FONT_SIZE) + 12, contentWidth + 8);
        int h = textHeight + contentHeight + 7;
        int borderColor = 0xFF000000 | (ArmorHudState.widgetColor & 0x00FFFFFF);
        drawTrackingFrame(graphics, "Armor", x, y, w, h, borderColor, TRACKING_FONT_SIZE);
        int contentY = y + textHeight + 2;

        for (int i = 0; i < slots.length; i++) {
            ItemStack stack = mc.player.getItemBySlot(slots[i]);
            int ix = vertical ? x + 4 : x + 4 + i * 20;
            int iy = vertical ? contentY + i * 20 : contentY;
            if (!stack.isEmpty()) {
                graphics.item(stack, ix, iy);
                graphics.itemDecorations(mc.font, stack, ix, iy);
            } else {
                graphics.outline(ix, iy, 16, 16, (ArmorHudState.widgetColor & 0x00FFFFFF) | 0x55000000);
            }
        }
    }

    public static int armorHudWidth() {
        boolean vertical = "Vertical".equals(ArmorHudState.orientation);
        return Math.max(PortableTextRenderer.width("Armor", TRACKING_FONT_SIZE) + 12, (vertical ? 16 : 76) + 8);
    }

    public static int armorHudHeight() {
        boolean vertical = "Vertical".equals(ArmorHudState.orientation);
        return PortableTextRenderer.height("Ag", TRACKING_FONT_SIZE) + (vertical ? 76 : 16) + 7;
    }

    private static void drawInventory(net.minecraft.client.gui.GuiGraphicsExtractor graphics, Minecraft mc) {
        for (int row = 0; row < InventoryHudState.ROWS; row++) {
            for (int column = 0; column < InventoryHudState.COLUMNS; column++) {
                int inventoryIndex = row * InventoryHudState.COLUMNS + column;
                Slot slot = mc.player.inventoryMenu.getSlot(InventoryMenu.INV_SLOT_START + inventoryIndex);
                ItemStack stack = slot.getItem();
                if (stack.isEmpty()) continue;

                int x = InventoryHudState.x + column * InventoryHudState.SLOT_SPACING;
                int y = InventoryHudState.y + row * InventoryHudState.SLOT_SPACING;
                graphics.item(stack, x, y);
                graphics.itemDecorations(mc.font, stack, x, y);
            }
        }
    }

    private static String formatTime(long seconds) {
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long secs = seconds % 60L;
        if ("Long".equals(SessionManagerState.timeFormat)) return hours + "h " + minutes + "m " + secs + "s";
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }
}
