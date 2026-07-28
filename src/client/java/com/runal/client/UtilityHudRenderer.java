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
            drawPanel(graphics, SessionManagerState.x, SessionManagerState.y, 112, 24, SessionManagerState.widgetColor);
            drawLine(graphics, mc, SessionManagerState.x + 6, SessionManagerState.y + 8, "Playtime", formatTime(seconds), SessionManagerState.labelColor, SessionManagerState.valueColor);
        }

        if (PerformanceHudState.enabled) {
            int lines = countPerformanceLines();
            int h = Math.max(16, lines * 11 + 8);
            int y = PerformanceHudState.y + 5;
            drawPanel(graphics, PerformanceHudState.x, PerformanceHudState.y, 112, h, 0xAA101216);
            if (PerformanceHudState.fps) { drawLine(graphics, mc, PerformanceHudState.x + 6, y, "FPS", String.valueOf(mc.getFps()), PerformanceHudState.nameColor, PerformanceHudState.valueColor); y += 11; }
            if (PerformanceHudState.tps) { drawLine(graphics, mc, PerformanceHudState.x + 6, y, "TPS", "20.0", PerformanceHudState.nameColor, PerformanceHudState.valueColor); y += 11; }
            if (PerformanceHudState.ping && mc.player != null && mc.getConnection() != null && mc.getConnection().getPlayerInfo(mc.player.getUUID()) != null) {
                drawLine(graphics, mc, PerformanceHudState.x + 6, y, "Ping", mc.getConnection().getPlayerInfo(mc.player.getUUID()).getLatency() + "ms", PerformanceHudState.nameColor, PerformanceHudState.valueColor); y += 11;
            }
            if (PerformanceHudState.direction && mc.player != null) drawLine(graphics, mc, PerformanceHudState.x + 6, y, "Dir", mc.player.getDirection().getName(), PerformanceHudState.nameColor, PerformanceHudState.valueColor);
        }

        if (ArmorHudState.enabled && ArmorHudState.showHud && mc.player != null) {
            drawArmor(graphics, mc);
        }

        if (InventoryHudState.enabled && mc.player != null) {
            drawInventory(graphics, mc);
        }

        drawCooldowns(graphics, mc);

        if (EventTrackerState.enabled && !EventTrackerState.events.isEmpty()) {
            int lines = EventTrackerState.events.size();
            int h = Math.max(16, lines * 11 + 8);
            int w = 60;
            for (EventTrackerState.TrackedEvent event : EventTrackerState.events.values()) {
                String value = event.remainingSeconds > 0 ? EventTrackerState.formatTime(event.remainingSeconds) : "Active";
                w = Math.max(w, mc.font.width(event.name + ": " + value) + 12);
            }
            drawPanel(graphics, EventTrackerState.x, EventTrackerState.y, w, h, 0xAA101216);
            int y = EventTrackerState.y + 5;
            for (EventTrackerState.TrackedEvent event : EventTrackerState.events.values()) {
                String value = event.remainingSeconds > 0 ? EventTrackerState.formatTime(event.remainingSeconds) : "Active";
                drawLine(graphics, mc, EventTrackerState.x + 6, y, event.name, value, EventTrackerState.nameColor, EventTrackerState.valueColor);
                y += 11;
            }
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
        //? if 1.21.4 {
        /*graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0f);
        graphics.pose().scale(WARNING_TITLE_SCALE, WARNING_TITLE_SCALE, 1f);
        *///?} else {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(WARNING_TITLE_SCALE, WARNING_TITLE_SCALE);
        //?}
        graphics.text(mc.font, title, 0, 0, 0xFFFF3B3B, true);
        //? if 1.21.4 {
        /*graphics.pose().popPose();
        *///?} else {
        graphics.pose().popMatrix();
        //?}
    }

    private static void drawBossTitle(net.minecraft.client.gui.GuiGraphicsExtractor graphics, Minecraft mc) {
        //? if 1.21.4 {
        /*graphics.pose().pushPose();
        graphics.pose().translate(BossTitleState.x, BossTitleState.y, 0f);
        graphics.pose().scale(BossTitleState.scale, BossTitleState.scale, 1f);
        *///?} else {
        graphics.pose().pushMatrix();
        graphics.pose().translate(BossTitleState.x, BossTitleState.y);
        graphics.pose().scale(BossTitleState.scale, BossTitleState.scale);
        //?}
        graphics.centeredText(mc.font, BossTitleState.currentText, 0, 0, BossTitleState.textColor);
        //? if 1.21.4 {
        /*graphics.pose().popPose();
        *///?} else {
        graphics.pose().popMatrix();
        //?}
    }

    private static void drawBossDefeatCounter(net.minecraft.client.gui.GuiGraphicsExtractor graphics, Minecraft mc) {
        String bossName = BossTitleState.lastBossName;
        String value = String.valueOf(BossDefeatState.getCount(bossName));

        int w = Math.max(60, mc.font.width(bossName + ": " + value) + 12);
        int h = 16;

        drawPanel(graphics, BossDefeatState.x, BossDefeatState.y, w, h, 0xAA101216);
        drawLine(graphics, mc, BossDefeatState.x + 6, BossDefeatState.y + 5, bossName, value, BossDefeatState.nameColor, BossDefeatState.valueColor);
    }

    private static int countPerformanceLines() {
        int lines = 0;
        if (PerformanceHudState.fps) lines++;
        if (PerformanceHudState.tps) lines++;
        if (PerformanceHudState.ping) lines++;
        if (PerformanceHudState.direction) lines++;
        return lines;
    }

    private static void drawPanel(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x + 2, y + 3, x + w + 2, y + h + 3, 0x33000000);
        graphics.fill(x, y, x + w, y + h, color);
        graphics.outline(x, y, w, h, 0x5535D77A);
    }

    private record CooldownEntry(String name, String value, int nameColor, int valueColor) {
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
                entries.add(new CooldownEntry(name, value, ItemCooldownHudState.nameColor, ItemCooldownHudState.valueColor));
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

        int h = Math.max(16, entries.size() * 11 + 8);
        int w = 60;
        for (CooldownEntry entry : entries) {
            w = Math.max(w, mc.font.width(entry.name() + ": " + entry.value()) + 12);
        }

        drawPanel(graphics, ItemCooldownHudState.x, ItemCooldownHudState.y, w, h, 0xAA101216);
        int y = ItemCooldownHudState.y + 5;
        for (CooldownEntry entry : entries) {
            drawLine(graphics, mc, ItemCooldownHudState.x + 6, y, entry.name(), entry.value(), entry.nameColor(), entry.valueColor());
            y += 11;
        }
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

        int w = 60;
        w = Math.max(w, mc.font.width("Dungeon: " + DungeonTrackerState.dungeonName) + 12);
        w = Math.max(w, mc.font.width("Room: " + roomStr) + 12);
        w = Math.max(w, mc.font.width("Parkour: " + parkourStr) + 12);
        w = Math.max(w, mc.font.width(DungeonTrackerState.bossName + ": " + bossStr) + 12);
        w = Math.max(w, mc.font.width("Treasure Chest: " + chestStr) + 12);
        int h = Math.max(16, 5 * 11 + 8);

        drawPanel(graphics, DungeonTrackerState.x, DungeonTrackerState.y, w, h, 0xAA101216);
        int y = DungeonTrackerState.y + 5;
        drawLine(graphics, mc, DungeonTrackerState.x + 6, y, "Dungeon", DungeonTrackerState.dungeonName, color, valueColor); y += 11;
        drawLine(graphics, mc, DungeonTrackerState.x + 6, y, "Room", roomStr, color, valueColor); y += 11;
        drawLine(graphics, mc, DungeonTrackerState.x + 6, y, "Parkour", parkourStr, color, valueColor); y += 11;
        drawLine(graphics, mc, DungeonTrackerState.x + 6, y, DungeonTrackerState.bossName, bossStr, color, valueColor); y += 11;
        drawLine(graphics, mc, DungeonTrackerState.x + 6, y, "Treasure Chest", chestStr, color, valueColor);
    }

    private static void drawArmor(net.minecraft.client.gui.GuiGraphicsExtractor graphics, Minecraft mc) {
        EquipmentSlot[] slots = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
        int x = ArmorHudState.x;
        int y = ArmorHudState.y;
        int w = "Vertical".equals(ArmorHudState.orientation) ? 24 : 84;
        int h = "Vertical".equals(ArmorHudState.orientation) ? 84 : 24;
        drawPanel(graphics, x, y, w, h, 0xAA101216);

        for (int i = 0; i < slots.length; i++) {
            ItemStack stack = mc.player.getItemBySlot(slots[i]);
            int ix = "Vertical".equals(ArmorHudState.orientation) ? x + 4 : x + 4 + i * 20;
            int iy = "Vertical".equals(ArmorHudState.orientation) ? y + 4 + i * 20 : y + 4;
            if (!stack.isEmpty()) {
                graphics.item(stack, ix, iy);
                graphics.itemDecorations(mc.font, stack, ix, iy);
            } else {
                graphics.outline(ix, iy, 16, 16, 0x559D9DA8);
            }
        }
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

    private static void drawLine(net.minecraft.client.gui.GuiGraphicsExtractor graphics, Minecraft mc, int x, int y, String label, String value, int labelColor, int valueColor) {
        graphics.text(mc.font, label + ":", x, y, labelColor, true);
        graphics.text(mc.font, value, x + mc.font.width(label + ": "), y, valueColor, true);
    }

    private static String formatTime(long seconds) {
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long secs = seconds % 60L;
        if ("Long".equals(SessionManagerState.timeFormat)) return hours + "h " + minutes + "m " + secs + "s";
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }
}
