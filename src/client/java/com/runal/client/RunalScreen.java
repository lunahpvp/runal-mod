package com.runal.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
//? if 1.21.4 {
//?} else {
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
//? if 1.21.4 {
//?} else {
import net.minecraft.network.chat.FontDescription;
//?}
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RunalScreen extends Screen {

    private static final Identifier SCEPTER_FONT =
            Identifier.fromNamespaceAndPath("scepterutils", "scepter_font");

    private static final Map<String, int[]> SAVED_POSITIONS = new HashMap<>();
    private static final Set<Module> EXPANDED = new HashSet<>();
    private static final Set<String> COLLAPSED_CATEGORIES = new HashSet<>();

    private static final Map<Module, Float> TOGGLE_ANIM = new HashMap<>();
    private static final Map<Module, Float> HOVER_ANIM = new HashMap<>();
    private static final Map<Module, Float> EXPAND_ANIM = new HashMap<>();
    private static final Map<String, Float> COLLAPSE_ANIM = new HashMap<>();
    private static final Map<ModuleSetting, Float> TOGGLE_KNOB_ANIM = new HashMap<>();
    private static final String[] CATEGORY_ORDER = { "Combat", "Visual", "Tracking", "Misc" };

    // Runal's compact logical dimensions. Melinoe's independent resolution scale
    // is applied to these; Minecraft's configurable GUI scale is not.
    private static final int COLUMN_WIDTH = 180;
    private static final int COLUMN_GAP = 20;
    private static final int ROW_HEIGHT = 29;
    private static final int SUB_ROW_HEIGHT = 22;
    private static final int HEADER_HEIGHT = 32;
    private static final int PANEL_RADIUS = 0;
    private static final int ROW_RADIUS = 6;
    private static final int PANEL_BOTTOM_MARGIN = 44;
    private static final int SCROLL_SPEED = 12;

    private static final float TEXT_SCALE = 0.78f;
    private static final float HEADER_TEXT_SCALE = 1.15f;
    private static final float SETTING_TEXT_SIZE = 12f;
    private static final float MODULE_TEXT_SIZE = 15f;
    private static final float SUB_TEXT_SCALE = 0.66f;

    private static final int TEXT_LEFT_PADDING = 14;
    private static final int SUB_TEXT_LEFT_PADDING = 17;

    private static final int COLOR_HEADER_BG = 0xF21A1B20;
    private static final int COLOR_HEADER_BG_HOVER = 0xF224252C;
    private static final int COLOR_PANEL_BG = 0xE70D0E12;
    private static final int COLOR_PANEL_BORDER = 0x703A3C44;
    private static final int COLOR_PANEL_BORDER_HOT = 0xAA35D77A;
    private static final int COLOR_ROW_OFF = 0xEA17181D;
    private static final int COLOR_ROW_HOVER = 0xF022242A;
    private static final int COLOR_SUB_ROW = 0xE8121317;
    private static final int COLOR_SUB_ROW_HOVER = 0xEA1C1E24;
    private static final int COLOR_TEXT = 0xFFEDEDF2;
    private static final int COLOR_DIM_TEXT = 0xFFA7A8B2;
    private static final int COLOR_HEADER_TEXT = 0xFFFFFFFF;
    private static final int COLOR_BACKDROP_TOP = 0x60000000;
    private static final int COLOR_BACKDROP_BOTTOM = 0x85000000;
    private static final int COLOR_ACCENT = 0xFF35D77A;
    private static final int COLOR_ACCENT_ROW = 0xF0183224;
    private static final int COLOR_SEARCH_BG = 0xE8111216;
    /** Melinoe's exact ClickGUI.gray26 - flat panel/header/off-row fill for the NVG chrome. */
    private static final int COLOR_GRAY26 = 0xFF1A1A1A;

    private static final long OPEN_ANIM_DURATION_MS = 480L;
    private long openTimeMs = 0L;

    private EditBox searchBox;
    private SliderModuleSetting draggingSlider;
    private int draggingSliderPanelX;
    private TextModuleSetting editingText;
    private ColorModuleSetting draggingColor;
    private int draggingColorPanelX;
    private int draggingColorRowY;
    private ColorModuleSetting editingColor;

    private static class Panel {
        final String category;
        final List<Module> modules;
        int x, y;
        int scroll = 0;
        boolean dragging = false;
        int dragOffsetX, dragOffsetY;

        Panel(String category, List<Module> modules, int x, int y) {
            this.category = category;
            this.modules = modules;
            this.x = x;
            this.y = y;
        }
    }

    private final List<Panel> panels = new ArrayList<>();

    public RunalScreen() {
        super(Component.literal("Runal"));
    }

    @Override
    protected void init() {
        panels.clear();

        Map<String, List<Module>> grouped = new LinkedHashMap<>();
        for (String cat : CATEGORY_ORDER) grouped.put(cat, new ArrayList<>());

        for (Module module : ModuleManager.getModules()) {
            grouped.computeIfAbsent(module.getCategory(), k -> new ArrayList<>()).add(module);
        }

        int startX = 10;
        int startY = 10;
        int columnIndex = 0;

        for (String category : grouped.keySet()) {
            List<Module> modules = grouped.get(category);
            if (modules.isEmpty()) continue;

            int defaultX = startX + columnIndex * (COLUMN_WIDTH + COLUMN_GAP);
            int defaultY = startY;

            int[] saved = SAVED_POSITIONS.get(category);
            int x = saved != null ? saved[0] : defaultX;
            int y = saved != null ? saved[1] : defaultY;

            panels.add(new Panel(category, modules, x, y));
            COLLAPSE_ANIM.putIfAbsent(category, COLLAPSED_CATEGORIES.contains(category) ? 0f : 1f);
            columnIndex++;
        }

        searchBox = new EditBox(
                this.font, (this.width - 220) / 2, this.height - 32, 220, 20,
                Component.literal("Search")
        );
        searchBox.setBordered(false);
        //? if 1.21.4 {
        //?} else {
        searchBox.setCentered(true);
        //?}
        this.addRenderableWidget(searchBox);

        openTimeMs = System.currentTimeMillis();
    }

    private boolean matchesSearch(Module module) {
        String query = searchBox.getValue().trim().toLowerCase();
        return query.isEmpty() || module.getName().toLowerCase().contains(query);
    }

    private List<Module> visibleModules(Panel panel) {
        List<Module> result = new ArrayList<>();
        for (Module module : panel.modules) {
            if (matchesSearch(module)) result.add(module);
        }
        return result;
    }

    private Component styled(String text) {
        //? if 1.21.4 {
        /*return Component.literal(text).withStyle(Style.EMPTY.withFont(SCEPTER_FONT));
        *///?} else {
        return Component.literal(text).withStyle(
                Style.EMPTY.withFont(new FontDescription.Resource(SCEPTER_FONT))
        );
        //?}
    }

    private float easeOutCubic(float t) {
        float f = t - 1f;
        return f * f * f + 1f;
    }

    /**
     * Melinoe's ClickGUI scale. It is intentionally independent from Minecraft's
     * configurable GUI Scale option, so that option cannot inflate panel geometry.
     */
    private float getStandardGuiScale() {
        var window = Minecraft.getInstance().getWindow();
        float verticalScale = (window.getScreenHeight() / 1080f) / NVGRenderer.devicePixelRatio();
        float horizontalScale = (window.getScreenWidth() / 1920f) / NVGRenderer.devicePixelRatio();
        float scale = Math.max(verticalScale, horizontalScale);
        scale = Math.max(1f, Math.min(3f, scale));
        return Math.round(scale * 10f) / 10f;
    }

    private int scaledNvgMouseX() {
        return (int) (Minecraft.getInstance().mouseHandler.xpos() / getStandardGuiScale());
    }

    private int scaledNvgMouseY() {
        return (int) (Minecraft.getInstance().mouseHandler.ypos() / getStandardGuiScale());
    }

    private int clickGuiViewportWidth() {
        //? if 26.1.2 {
        return (int) (Minecraft.getInstance().getWindow().getScreenWidth() / getStandardGuiScale());
        //?} else {
        /*return this.width;
        *///?}
    }

    private int clickGuiViewportHeight() {
        //? if 26.1.2 {
        return (int) (Minecraft.getInstance().getWindow().getScreenHeight() / getStandardGuiScale());
        //?} else {
        /*return this.height;
        *///?}
    }

    private int interactionMouseX(double eventX) {
        //? if 26.1.2 {
        return scaledNvgMouseX();
        //?} else {
        /*return (int) eventX;
        *///?}
    }

    private int interactionMouseY(double eventY) {
        //? if 26.1.2 {
        return scaledNvgMouseY();
        //?} else {
        /*return (int) eventY;
        *///?}
    }

    private float easeOutQuart(float t) {
        float f = 1f - t;
        return 1f - f * f * f * f;
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private float anim(Map<Module, Float> map, Module module) {
        return map.getOrDefault(module, 0f);
    }

    private float animCategory(Map<String, Float> map, String category) {
        return map.getOrDefault(category, 0f);
    }

    private void animate(Map<Module, Float> map, Module module, float target, float speed) {
        float current = anim(map, module);
        if (Math.abs(current - target) < 0.002f) {
            map.put(module, target);
            return;
        }
        map.put(module, lerp(current, target, speed));
    }

    private float animSetting(Map<ModuleSetting, Float> map, ModuleSetting setting) {
        return map.getOrDefault(setting, 0f);
    }

    private void animateSetting(Map<ModuleSetting, Float> map, ModuleSetting setting, float target, float speed) {
        float current = animSetting(map, setting);
        if (Math.abs(current - target) < 0.002f) {
            map.put(setting, target);
            return;
        }
        map.put(setting, lerp(current, target, speed));
    }

    private void animateCategory(Map<String, Float> map, String category, float target, float speed) {
        float current = animCategory(map, category);
        if (Math.abs(current - target) < 0.002f) {
            map.put(category, target);
            return;
        }
        map.put(category, lerp(current, target, speed));
    }

    private int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private int alpha(int color, float alpha) {
        int a = clamp255((int) (((color >>> 24) & 255) * alpha));
        return (color & 0x00FFFFFF) | (a << 24);
    }

    private int accentColor() {
        return RunalSettings.accentColor;
    }

    private int accentRowColor() {
        return mixColor(0xF0183224, accentColor(), 0.18f);
    }

    private int mixColor(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));

        int a1 = (c1 >>> 24) & 255;
        int r1 = (c1 >>> 16) & 255;
        int g1 = (c1 >>> 8) & 255;
        int b1 = c1 & 255;
        int a2 = (c2 >>> 24) & 255;
        int r2 = (c2 >>> 16) & 255;
        int g2 = (c2 >>> 8) & 255;
        int b2 = c2 & 255;

        int a = (int) lerp(a1, a2, t);
        int r = (int) lerp(r1, r2, t);
        int g = (int) lerp(g1, g2, t);
        int b = (int) lerp(b1, b2, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int brighten(int color, int amount) {
        int a = (color >>> 24) & 255;
        int r = clamp255(((color >>> 16) & 255) + amount);
        int g = clamp255(((color >>> 8) & 255) + amount);
        int b = clamp255((color & 255) + amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void drawRoundedRect(GuiGraphicsExtractor context, int x, int y, int width, int height, int color) {
        drawRoundedRect(context, x, y, width, height, color, PANEL_RADIUS);
    }

    private void drawRoundedRect(GuiGraphicsExtractor context, int x, int y, int width, int height, int color, int radius) {
        if (width <= 0 || height <= 0) return;

        int r = Math.max(0, Math.min(radius, Math.min(width / 2, height / 2)));
        if (r <= 0) {
            context.fill(x, y, x + width, y + height, color);
            return;
        }

        context.fill(x, y + r, x + width, y + height - r, color);
        int rr = r * r;
        for (int py = 0; py < r; py++) {
            double dy = r - py - 0.5;
            int inset = (int) Math.ceil(r - Math.sqrt(Math.max(0, rr - dy * dy)));
            context.fill(x + inset, y + py, x + width - inset, y + py + 1, color);
            context.fill(x + inset, y + height - py - 1, x + width - inset, y + height - py, color);
        }
    }

    private void drawCorner(GuiGraphicsExtractor context, int cx, int cy, int radius, int color, boolean left, boolean top) {
        int samples = 4;

        for (int py = 0; py < radius; py++) {
            for (int px = 0; px < radius; px++) {
                int inside = 0;

                for (int sy = 0; sy < samples; sy++) {
                    for (int sx = 0; sx < samples; sx++) {
                        double sampleX = px + (sx + 0.5) / samples;
                        double sampleY = py + (sy + 0.5) / samples;
                        double dx = radius - sampleX;
                        double dy = radius - sampleY;

                        if (dx * dx + dy * dy <= radius * radius) inside++;
                    }
                }

                if (inside == 0) continue;
                float coverage = inside / (float) (samples * samples);
                int drawX = left ? cx - radius + px : cx + radius - px - 1;
                int drawY = top ? cy - radius + py : cy + radius - py - 1;
                context.fill(drawX, drawY, drawX + 1, drawY + 1, alpha(color, coverage));
            }
        }
    }

    private void drawRoundedOutline(GuiGraphicsExtractor context, int x, int y, int width, int height, int radius, int color) {
        drawRoundedRect(context, x, y, width, height, color, radius);
        drawRoundedRect(context, x + 1, y + 1, width - 2, height - 2, COLOR_PANEL_BG, Math.max(0, radius - 1));
    }

    private void drawPanelShadow(GuiGraphicsExtractor context, int x, int y, int w, int h) {
        drawRoundedRect(context, x + 3, y + 5, w, h, 0x36000000, PANEL_RADIUS + 1);
        drawRoundedRect(context, x + 1, y + 2, w, h, 0x26000000, PANEL_RADIUS);
    }

    private void drawPanelChrome(GuiGraphicsExtractor context, int x, int y, int w, int h) {
        drawPanelShadow(context, x, y, w, h);
        drawRoundedRect(context, x, y, w, h, COLOR_PANEL_BG, PANEL_RADIUS);
        drawRoundedOutline(context, x, y, w, h, PANEL_RADIUS, COLOR_PANEL_BORDER);
        if (!RunalSettings.roundedPanelBottoms) {
            int bottomY = y + h - PANEL_RADIUS - 1;
            context.fill(x, bottomY, x + w, y + h, COLOR_PANEL_BG);
            context.fill(x, y + h - 1, x + w, y + h, COLOR_PANEL_BORDER);
            context.fill(x, bottomY, x + 1, y + h, COLOR_PANEL_BORDER);
            context.fill(x + w - 1, bottomY, x + w, y + h, COLOR_PANEL_BORDER);
        }
    }

    private void drawHeader(GuiGraphicsExtractor context, int x, int y, int w, int h, boolean hovered, boolean collapsed) {
        int color = hovered ? COLOR_HEADER_BG_HOVER : COLOR_HEADER_BG;
        drawRoundedRect(context, x + 4, y + 4, w - 8, h - 7, color, ROW_RADIUS);
        context.fill(x + 12, y + h - 2, x + w - 12, y + h - 1, alpha(accentColor(), collapsed ? 0.28f : 0.60f));
        drawScaledLeftText(context, collapsed ? "+" : "-", x + w - 16, y + 1, h, COLOR_DIM_TEXT, TEXT_SCALE);
    }

    private void drawModuleRow(GuiGraphicsExtractor context, int x, int y, int w, int h, int color, float toggle) {
        drawRoundedRect(context, x + 4, y + 2, w - 8, h - 3, color, ROW_RADIUS);

        if (toggle > 0.02f) {
            int accent = alpha(accentColor(), 0.90f * toggle);
            int soft = alpha(accentColor(), 0.16f * toggle);
            drawRoundedRect(context, x + 3, y + 1, w - 6, h - 1, soft, ROW_RADIUS + 1);
            drawRoundedRect(context, x + 7, y + 5, 3, h - 9, accent, 2);
            context.fill(x + 12, y + h - 4, x + w - 12, y + h - 3, alpha(accentColor(), 0.35f * toggle));
        }
    }

    private void drawSubRow(GuiGraphicsExtractor context, int x, int y, int w, int h, int color, float expand) {
        int inset = (int) lerp(10f, 5f, expand);
        drawRoundedRect(context, x + inset, y + 1, w - inset - 4, h - 2, color, ROW_RADIUS - 1);
    }

    private void drawSlider(GuiGraphicsExtractor context, SliderModuleSetting slider, int x, int y, int h) {
        int trackX = x + 62;
        int trackY = y + h - 6;
        int trackW = COLUMN_WIDTH - 80;
        float value = Math.max(0f, Math.min(1f, slider.getNormalizedValue()));
        int fillW = (int) (trackW * value);
        drawRoundedRect(context, trackX, trackY, trackW, 3, 0xFF2A2D34, 2);
        drawRoundedRect(context, trackX, trackY, fillW, 3, accentColor(), 2);
        drawRoundedRect(context, trackX + fillW - 2, trackY - 2, 5, 7, 0xFFEDEDF2, 3);
    }

    private void drawScaledCenteredText(GuiGraphicsExtractor context, String text, int boxX, int boxY, int boxWidth, int boxHeight, int color, float scale) {
        Component comp = styled(text);
        int rawWidth = font.width(comp);
        float scaledWidth = rawWidth * scale;
        float scaledHeight = font.lineHeight * scale;
        float drawX = boxX + (boxWidth - scaledWidth) / 2f;
        float drawY = boxY + (boxHeight - scaledHeight) / 2f + 0.5f;

        //? if 1.21.4 {
        /*context.pose().pushPose();
        context.pose().translate(drawX, drawY, 0f);
        context.pose().scale(scale, scale, 1f);
        *///?} else {
        context.pose().pushMatrix();
        context.pose().translate(drawX, drawY);
        context.pose().scale(scale, scale);
        //?}
        context.text(font, comp, 0, 0, color, false);
        //? if 1.21.4 {
        /*context.pose().popPose();
        *///?} else {
        context.pose().popMatrix();
        //?}
    }

    private void drawScaledLeftText(GuiGraphicsExtractor context, String text, int x, int boxY, int boxHeight, int color, float scale) {
        Component comp = styled(text);
        float scaledHeight = font.lineHeight * scale;
        float drawY = boxY + (boxHeight - scaledHeight) / 2f + 0.5f;

        //? if 1.21.4 {
        /*context.pose().pushPose();
        context.pose().translate(x, drawY, 0f);
        context.pose().scale(scale, scale, 1f);
        *///?} else {
        context.pose().pushMatrix();
        context.pose().translate(x, drawY);
        context.pose().scale(scale, scale);
        //?}
        context.text(font, comp, 0, 0, color, false);
        //? if 1.21.4 {
        /*context.pose().popPose();
        *///?} else {
        context.pose().popMatrix();
        //?}
    }

    /** Trims text with a trailing "..." if it would render wider than maxWidth. */
    private String truncateToFit(String text, float maxWidth, float scale) {
        if (maxWidth <= 0) return "";
        if (font.width(styled(text)) * scale <= maxWidth) return text;
        String ellipsis = "...";
        for (int len = text.length() - 1; len > 0; len--) {
            String candidate = text.substring(0, len) + ellipsis;
            if (font.width(styled(candidate)) * scale <= maxWidth) return candidate;
        }
        return ellipsis;
    }

    /**
     * NanoVG's font functions (nvgFontFaceId etc.) are only safe to call from inside the
     * active deferred NVG frame (the NVGPIPRenderer callback) - calling them synchronously,
     * like from this method during layout, segfaults the JVM (confirmed via crash log: a
     * native access violation inside lwjgl_nanovg.dll from nvgFontFaceId). Layout math that
     * needs a width *before* queuing the actual (correctly-deferred) NVG draw call has to
     * estimate it some other way - vanilla font width scaled to the target NVG size is a
     * close enough proxy, since both fonts are normal-proportioned sans-serif.
     */
    private float estimateNvgTextWidth(String text, float nvgSize) {
        return font.width(styled(text)) * (nvgSize / 9f);
    }

    private String truncateToFitNVG(String text, float maxWidth, float size) {
        if (maxWidth <= 0) return "";
        if (estimateNvgTextWidth(text, size) <= maxWidth) return text;
        String ellipsis = "...";
        for (int len = text.length() - 1; len > 0; len--) {
            String candidate = text.substring(0, len) + ellipsis;
            if (estimateNvgTextWidth(candidate, size) <= maxWidth) return candidate;
        }
        return ellipsis;
    }

    private void toggleCategory(String category) {
        if (COLLAPSED_CATEGORIES.contains(category)) {
            COLLAPSED_CATEGORIES.remove(category);
        } else {
            COLLAPSED_CATEGORIES.add(category);
        }
    }

    private void drawSearchChrome(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        if (searchBox == null) return;

        int x = (this.width - 220) / 2;
        int y = this.height - 32;
        int w = 220;
        int h = 20;
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        boolean focused = searchBox.isFocused();
        int border = focused ? accentColor() : (hovered ? 0x90686B76 : COLOR_PANEL_BORDER);

        drawRoundedRect(context, x - 2, y - 2, w + 4, h + 4, alpha(0xFF000000, 0.24f), PANEL_RADIUS);
        drawRoundedRect(context, x - 1, y - 1, w + 2, h + 2, border, PANEL_RADIUS);
        drawRoundedRect(context, x, y, w, h, COLOR_SEARCH_BG, PANEL_RADIUS - 1);
        if (focused || hovered) context.fill(x + 12, y + h - 2, x + w - 12, y + h - 1, alpha(accentColor(), focused ? 0.75f : 0.35f));
        if (searchBox.getValue().isEmpty()) {
            drawScaledCenteredText(context, "Search mods...", x, y, w, h, COLOR_DIM_TEXT, SUB_TEXT_SCALE);
        }
    }

    private List<ModuleSetting> visibleSettings(Module module) {
        List<ModuleSetting> result = new ArrayList<>();
        addVisibleSettings(result, module.getSettings());
        return result;
    }

    private void addVisibleSettings(List<ModuleSetting> result, List<ModuleSetting> settings) {
        for (ModuleSetting setting : settings) {
            result.add(setting);
            if (setting instanceof SettingGroup group && group.isExpanded()) {
                addVisibleSettings(result, group.getSettings());
            }
        }
    }

    // Layout of the expanded HSB color picker (top label/swatch row, then square, hue strip,
    // hex box below it) - shared by rendering and click hit-testing so they can never drift.
    private static final int PICKER_TOP_ROW = SUB_ROW_HEIGHT;
    private static final int PICKER_GAP = 4;
    private static final int PICKER_SQUARE_H = 60;
    private static final int PICKER_HUE_H = 10;
    private static final int PICKER_HEX_H = 18;
    private static final int PICKER_BOTTOM_MARGIN = 6;
    private static final int PICKER_EXTENDED_HEIGHT = PICKER_TOP_ROW + PICKER_GAP + PICKER_SQUARE_H
            + PICKER_GAP + PICKER_HUE_H + PICKER_GAP + PICKER_HEX_H + PICKER_BOTTOM_MARGIN;

    private int settingRowHeight(ModuleSetting setting) {
        if (setting instanceof ColorModuleSetting color) return color.isExtended() ? PICKER_EXTENDED_HEIGHT : SUB_ROW_HEIGHT;
        if (setting instanceof SliderModuleSetting) return SUB_ROW_HEIGHT + 6;
        return SUB_ROW_HEIGHT;
    }

    private int totalSettingsHeight(List<ModuleSetting> settings) {
        int total = 0;
        for (ModuleSetting setting : settings) total += settingRowHeight(setting);
        return total;
    }

    private void drawSettingControl(GuiGraphicsExtractor context, ModuleSetting setting, int x, int y, int h, boolean hovered) {
        int controlX = x + COLUMN_WIDTH - 48;
        int controlY = y + 4;

        if (setting instanceof ToggleModuleSetting toggle) {
            int bg = toggle.getValue() ? alpha(accentColor(), 0.75f) : 0xFF2A2D34;
            drawRoundedRect(context, controlX + 18, controlY, 20, 8, bg, 4);
            drawRoundedRect(context, controlX + (toggle.getValue() ? 30 : 20), controlY + 1, 6, 6, COLOR_TEXT, 3);
        } else if (setting instanceof ColorModuleSetting color) {
            drawRoundedRect(context, x + COLUMN_WIDTH - 25, controlY - 1, 14, 10, COLOR_PANEL_BORDER, 4);
            drawRoundedRect(context, x + COLUMN_WIDTH - 24, controlY, 12, 8, color.getColor(), 3);
        } else if (setting instanceof SettingGroup group) {
            context.fill(x + 12, y + h - 2, x + COLUMN_WIDTH - 12, y + h - 1, alpha(accentColor(), group.isExpanded() ? 0.55f : 0.22f));
        }
    }

    private int panelMaxHeight(Panel panel) {
        return Math.max(
                HEADER_HEIGHT + ROW_HEIGHT + SUB_ROW_HEIGHT,
                clickGuiViewportHeight() - panel.y - PANEL_BOTTOM_MARGIN
        );
    }

    private void drawScrollbar(GuiGraphicsExtractor context, Panel panel, int panelHeight, int fullHeight) {
        int trackTop = panel.y + HEADER_HEIGHT + 2;
        int trackBottom = panel.y + panelHeight - 6;
        int trackHeight = trackBottom - trackTop;
        if (trackHeight <= 4) return;

        int maxScroll = Math.max(1, fullHeight - panelHeight);
        int thumbHeight = Math.min(trackHeight, Math.max(14, (int) (trackHeight * (panelHeight / (float) fullHeight))));
        float progress = maxScroll <= 0 ? 0f : panel.scroll / (float) maxScroll;
        int thumbY = trackTop + (int) ((trackHeight - thumbHeight) * progress);
        int barX = panel.x + COLUMN_WIDTH - 5;

        context.fill(barX, trackTop, barX + 2, trackBottom, alpha(0xFFFFFFFF, 0.08f));
        context.fill(barX, thumbY, barX + 2, thumbY + thumbHeight, alpha(accentColor(), 0.55f));
    }

    private void drawResetButton(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        int[] bounds = resetButtonBounds();
        int x = bounds[0], y = bounds[1], w = bounds[2], h = bounds[3];
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;

        drawRoundedRect(context, x - 2, y - 2, w + 4, h + 4, alpha(0xFF000000, 0.24f), PANEL_RADIUS);
        drawRoundedRect(context, x - 1, y - 1, w + 2, h + 2, hovered ? accentColor() : COLOR_PANEL_BORDER, PANEL_RADIUS);
        drawRoundedRect(context, x, y, w, h, hovered ? COLOR_ROW_HOVER : COLOR_SEARCH_BG, PANEL_RADIUS - 1);
        drawScaledCenteredText(context, "Reset All Settings", x, y, w, h, hovered ? COLOR_TEXT : COLOR_DIM_TEXT, SUB_TEXT_SCALE);
    }

    private int[] resetButtonBounds() {
        int w = 118;
        int h = 20;
        int x = (this.width + 220) / 2 + 10;
        int y = this.height - 32;
        return new int[]{x, y, w, h};
    }

    private void resetAllSettings() {
        for (Module module : ModuleManager.getModules()) {
            module.resetSettings();
        }
        ModuleConfig.save();
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F)
        );
    }

    private int animatedPanelHeight(Panel panel, List<Module> visible) {
        int h = HEADER_HEIGHT + 5;
        float categoryOpen = COLLAPSED_CATEGORIES.contains(panel.category) ? 0f : 1f;

        for (Module module : visible) {
            h += (int) (ROW_HEIGHT * categoryOpen);
            float expand = anim(EXPAND_ANIM, module) * categoryOpen;
            if (!module.getSettings().isEmpty()) h += (int) (totalSettingsHeight(visibleSettings(module)) * expand);
        }

        return h + 4;
    }

    private void setSliderFromMouse(SliderModuleSetting slider, int panelX, int mouseX) {
        int trackX = panelX + 62;
        int trackW = COLUMN_WIDTH - 80;
        slider.setNormalizedValue((mouseX - trackX) / (float) trackW);
    }

    /** rowY here is the setting's own row start, matching settingRowY used during rendering. */
    private void handleColorPickerClick(ColorModuleSetting colorSetting, int panelX, int rowY, int mouseX, int mouseY) {
        if (!colorSetting.isExtended()) {
            colorSetting.onClick();
            return;
        }

        int localY = mouseY - rowY;
        if (localY < PICKER_TOP_ROW) {
            colorSetting.onClick(); // clicked the label/swatch row again - collapse
            return;
        }

        int pickerY = rowY + PICKER_TOP_ROW + PICKER_GAP;
        float[] square = colorPickerSquareBounds(panelX, pickerY, COLUMN_WIDTH);
        float[] hue = colorPickerHueBounds(panelX, pickerY, COLUMN_WIDTH);
        float hexY = hue[1] + hue[3] + PICKER_GAP;

        if (mouseX >= square[0] && mouseX <= square[0] + square[2] && mouseY >= square[1] && mouseY <= square[1] + square[3]) {
            colorSetting.setDragSection(0);
            draggingColor = colorSetting;
            draggingColorPanelX = panelX;
            draggingColorRowY = rowY;
            updateColorFromSquareDrag(colorSetting, square, mouseX, mouseY);
        } else if (mouseX >= hue[0] && mouseX <= hue[0] + hue[2] && mouseY >= hue[1] - 4 && mouseY <= hue[1] + hue[3] + 4) {
            colorSetting.setDragSection(1);
            draggingColor = colorSetting;
            draggingColorPanelX = panelX;
            draggingColorRowY = rowY;
            updateColorFromHueDrag(colorSetting, hue, mouseX);
        } else if (mouseY >= hexY && mouseY <= hexY + PICKER_HEX_H) {
            colorSetting.startHexEditing();
            editingColor = colorSetting;
        }
    }

    //? if 1.21.4 || 1.21.11 {
    /*@Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        renderContentLegacy(context, mouseX, mouseY, deltaTicks);
        super.render(context, mouseX, mouseY, deltaTicks);
    }
    *///?} else {
    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        renderContentNVG(context, mouseX, mouseY, deltaTicks);
        super.extractRenderState(context, mouseX, mouseY, deltaTicks);
    }
    //?}

    // Both renderContentLegacy and renderContentNVG are compiled for every Stonecutter target
    // (Stonecutter's //? preprocessor doesn't compose when a conditional block is nested inside
    // another one's commented-out branch), but only one of them is ever called per version - see
    // the render()/extractRenderState() dispatch above.
    private void renderContentLegacy(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        context.fillGradient(0, 0, this.width, this.height, COLOR_BACKDROP_TOP, COLOR_BACKDROP_BOTTOM);

        long elapsed = System.currentTimeMillis() - openTimeMs;
        float openProgress = Math.min(1f, elapsed / (float) OPEN_ANIM_DURATION_MS);
        float easedOpen = easeOutCubic(openProgress);
        float scale = 0.88f + 0.12f * easedOpen;

        //? if 1.21.4 {
        /*context.pose().pushPose();
        context.pose().translate(this.width / 2f, this.height / 2f, 0f);
        context.pose().scale(scale, scale, 1f);
        context.pose().translate(-this.width / 2f, -this.height / 2f, 0f);
        *///?} else {
        context.pose().pushMatrix();
        context.pose().translate(this.width / 2f, this.height / 2f);
        context.pose().scale(scale, scale);
        context.pose().translate(-this.width / 2f, -this.height / 2f);
        //?}

        for (Panel panel : panels) {
            List<Module> visible = visibleModules(panel);
            if (visible.isEmpty()) continue;

            COLLAPSE_ANIM.put(panel.category, COLLAPSED_CATEGORIES.contains(panel.category) ? 0f : 1f);
            boolean collapsed = COLLAPSED_CATEGORIES.contains(panel.category);
            float categoryOpen = COLLAPSED_CATEGORIES.contains(panel.category) ? 0f : 1f;
            int fullHeight = animatedPanelHeight(panel, visible);
            int panelHeight = Math.min(fullHeight, panelMaxHeight(panel));
            int maxScroll = Math.max(0, fullHeight - panelHeight);
            panel.scroll = Math.max(0, Math.min(panel.scroll, maxScroll));
            boolean headerHovered = mouseX >= panel.x
                    && mouseX <= panel.x + COLUMN_WIDTH
                    && mouseY >= panel.y
                    && mouseY <= panel.y + HEADER_HEIGHT;

            drawPanelChrome(context, panel.x, panel.y, COLUMN_WIDTH, panelHeight);
            drawHeader(context, panel.x, panel.y, COLUMN_WIDTH, HEADER_HEIGHT, headerHovered, collapsed);
            drawScaledCenteredText(context, panel.category, panel.x, panel.y + 1, COLUMN_WIDTH, HEADER_HEIGHT, COLOR_HEADER_TEXT, TEXT_SCALE);

            if (categoryOpen <= 0.025f) continue;

            int contentTop = panel.y + HEADER_HEIGHT;
            int contentBottom = panel.y + panelHeight - 4;
            context.enableScissor(panel.x, contentTop, panel.x + COLUMN_WIDTH, contentBottom);

            int rowY = contentTop + 3 - panel.scroll;
            for (Module module : visible) {
                boolean hovered = mouseY >= contentTop && mouseY <= contentBottom
                        && mouseX >= panel.x + 4
                        && mouseX <= panel.x + COLUMN_WIDTH - 4
                        && mouseY >= rowY + 2
                        && mouseY <= rowY + ROW_HEIGHT - 1;

                TOGGLE_ANIM.put(module, module.isEnabled() ? 1f : 0f);
                animate(HOVER_ANIM, module, hovered ? 1f : 0f, 0.22f);
                EXPAND_ANIM.put(module, EXPANDED.contains(module) ? 1f : 0f);

                float toggle = module.isEnabled() ? 1f : 0f;
                float hover = easeOutQuart(anim(HOVER_ANIM, module));
                float expand = EXPANDED.contains(module) ? 1f : 0f;
                int rowColor = mixColor(COLOR_ROW_OFF, accentRowColor(), toggle);
                rowColor = mixColor(rowColor, mixColor(COLOR_ROW_HOVER, brighten(accentRowColor(), 12), toggle), hover);

                drawModuleRow(context, panel.x, rowY, COLUMN_WIDTH, ROW_HEIGHT, rowColor, toggle);
                if (!module.getSettings().isEmpty()) {
                    drawScaledLeftText(context, EXPANDED.contains(module) ? "-" : "+", panel.x + COLUMN_WIDTH - 15, rowY, ROW_HEIGHT, COLOR_DIM_TEXT, TEXT_SCALE);
                }
                int textColor = mixColor(COLOR_TEXT, 0xFFFFFFFF, Math.max(toggle, hover * 0.45f));
                drawScaledCenteredText(context, module.getName(), panel.x, rowY, COLUMN_WIDTH, ROW_HEIGHT, textColor, TEXT_SCALE);

                rowY += ROW_HEIGHT;
                if (expand > 0.025f && !module.getSettings().isEmpty()) {
                    List<ModuleSetting> settings = visibleSettings(module);
                    int visibleHeight = (int) (totalSettingsHeight(settings) * expand);
                    int subStart = rowY;
                    int drawn = 0;

                    for (ModuleSetting setting : settings) {
                        if (drawn >= visibleHeight) break;
                        int allowedHeight = Math.min(settingRowHeight(setting), visibleHeight - drawn);
                        boolean subHovered = mouseY >= contentTop && mouseY <= contentBottom
                                && mouseX >= panel.x + 7
                                && mouseX <= panel.x + COLUMN_WIDTH - 5
                                && mouseY >= rowY + 1
                                && mouseY <= rowY + allowedHeight - 1;
                        boolean listening = setting instanceof KeybindModuleSetting keybind && keybind.isListening();
                        int subColor = listening ? alpha(accentColor(), 0.25f) : (subHovered ? COLOR_SUB_ROW_HOVER : COLOR_SUB_ROW);
                        drawSubRow(context, panel.x, rowY, COLUMN_WIDTH, allowedHeight, subColor, expand);

                        if (allowedHeight >= 9) {
                            if (setting instanceof ColorModuleSetting colorSetting && allowedHeight >= SUB_ROW_HEIGHT * 2) {
                                int lineH = allowedHeight / 2;
                                drawScaledLeftText(context, colorSetting.getLabel(), panel.x + SUB_TEXT_LEFT_PADDING, rowY, lineH, COLOR_DIM_TEXT, SUB_TEXT_SCALE);
                                String valueText = colorSetting.getDisplayValue();
                                int valueWidth = (int) (font.width(styled(valueText)) * SUB_TEXT_SCALE);
                                drawScaledLeftText(context, valueText, panel.x + COLUMN_WIDTH - valueWidth - 30, rowY + lineH, lineH, listening ? accentColor() : COLOR_TEXT, SUB_TEXT_SCALE);
                                drawSettingControl(context, colorSetting, panel.x, rowY + lineH, lineH, subHovered);
                            } else {
                                drawScaledLeftText(context, setting.getLabel(), panel.x + SUB_TEXT_LEFT_PADDING, rowY, allowedHeight, COLOR_DIM_TEXT, SUB_TEXT_SCALE);
                                String valueText = setting.getDisplayValue();
                                int valueWidth = (int) (font.width(styled(valueText)) * SUB_TEXT_SCALE);
                                boolean textEditing = setting instanceof TextModuleSetting text && text.isEditing();
                                drawScaledLeftText(context, valueText, panel.x + COLUMN_WIDTH - valueWidth - 12, rowY, allowedHeight, listening || textEditing ? accentColor() : COLOR_TEXT, SUB_TEXT_SCALE);
                                drawSettingControl(context, setting, panel.x, rowY, allowedHeight, subHovered);
                                if (setting instanceof SliderModuleSetting slider) drawSlider(context, slider, panel.x, rowY, allowedHeight);
                            }
                        }

                        rowY += allowedHeight;
                        drawn += allowedHeight;
                    }

                    rowY = subStart + visibleHeight;
                }
            }

            context.disableScissor();
            if (maxScroll > 0) drawScrollbar(context, panel, panelHeight, fullHeight);
        }

        //? if 1.21.4 {
        /*context.pose().popPose();
        *///?} else {
        context.pose().popMatrix();
        //?}
        drawSearchChrome(context, mouseX, mouseY);
        drawResetButton(context, mouseX, mouseY);
    }

    // NanoVG-based chrome. Layout/animation-state math is identical to renderContentLegacy
    // above; only the shape drawing backend changed (real anti-aliased vector shapes,
    // drop shadows and gradients instead of per-pixel rounded-rect approximation).
    // Vector chrome is recorded into `nvg` and executed later, inside the deferred
    // NVGPIPRenderer callback; text still goes through Minecraft's font renderer,
    // drawn immediately here (via `context`) so it lands on top of the vector layer.
    private void renderContentNVG(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        //? if 26.2 {
        /*renderContentLegacy(context, mouseX, mouseY, deltaTicks);
        *///?}
        //? if 1.21.4 || 1.21.11 || 26.1.2 {
        context.fillGradient(0, 0, this.width, this.height, COLOR_BACKDROP_TOP, COLOR_BACKDROP_BOTTOM);
        if (!NVGRenderer.hasDrawableSize()) return;

        int minecraftMouseX = mouseX;
        int minecraftMouseY = mouseY;

        // Melinoe does not use Screen's GUI-scaled mouse arguments. Its panels live in
        // the independent ClickGUI coordinate space, so use the raw cursor divided by
        // that same scale for rendering, hovering, dragging, and hit testing.
        mouseX = scaledNvgMouseX();
        mouseY = scaledNvgMouseY();

        List<Runnable> nvg = new ArrayList<>();

        long elapsed = System.currentTimeMillis() - openTimeMs;
        float openProgress = Math.min(1f, elapsed / (float) OPEN_ANIM_DURATION_MS);
        float easedOpen = easeOutCubic(openProgress);
        float scale = 0.20f + 0.80f * easedOpen;

        //? if 1.21.4 {
        /*context.pose().pushPose();
        context.pose().translate(this.width / 2f, this.height / 2f, 0f);
        context.pose().scale(scale, scale, 1f);
        context.pose().translate(-this.width / 2f, -this.height / 2f, 0f);
        *///?} else {
        context.pose().pushMatrix();
        context.pose().translate(this.width / 2f, this.height / 2f);
        context.pose().scale(scale, scale);
        context.pose().translate(-this.width / 2f, -this.height / 2f);
        //?}

        String tooltipText = null;
        int tooltipX = 0, tooltipY = 0;

        for (Panel panel : panels) {
            List<Module> visible = visibleModules(panel);
            if (visible.isEmpty()) continue;

            COLLAPSE_ANIM.put(panel.category, COLLAPSED_CATEGORIES.contains(panel.category) ? 0f : 1f);
            boolean collapsed = COLLAPSED_CATEGORIES.contains(panel.category);
            float categoryOpen = COLLAPSED_CATEGORIES.contains(panel.category) ? 0f : 1f;
            int fullHeight = animatedPanelHeight(panel, visible);
            int panelHeight = Math.min(fullHeight, panelMaxHeight(panel));
            int maxScroll = Math.max(0, fullHeight - panelHeight);
            panel.scroll = Math.max(0, Math.min(panel.scroll, maxScroll));

            int px = panel.x, py = panel.y, ph = panelHeight;
            int shadowHeight = collapsed ? HEADER_HEIGHT + 10 : ph;
            nvg.add(() -> drawPanelShadowNVG(px, py, COLUMN_WIDTH, shadowHeight));
            nvg.add(() -> drawPanelHeaderCapNVG(px, py, COLUMN_WIDTH, HEADER_HEIGHT));
            String categoryName = panel.category;
            // Real vector text via NVG - no bitmap-atlas rescaling, so it stays crisp at any
            // size instead of the uneven stroke-thickness artifact the old scaled bitmap font had.
            nvg.add(() -> NVGRenderer.textCentered(categoryName, px, py, COLUMN_WIDTH, HEADER_HEIGHT, 22f, COLOR_HEADER_TEXT));

            boolean lastVisibleEnabled = !visible.isEmpty() && visible.get(visible.size() - 1).isEnabled();
            // If anything in this panel is expanded, its settings (transparent, showing the
            // panel's own gray26) may be what's actually sitting right above the cap - not
            // necessarily the last module in the list - so an accent-colored cap could mismatch
            // what's directly above it. Only tint the cap when the panel is a plain flat list.
            boolean anyExpanded = visible.stream().anyMatch(EXPANDED::contains);
            int capColor = (!anyExpanded && lastVisibleEnabled) ? accentColor() : COLOR_GRAY26;

            if (categoryOpen <= 0.025f) {
                int capY = py + HEADER_HEIGHT;
                nvg.add(() -> drawPanelBottomCapNVG(px, capY, COLUMN_WIDTH, 10, capColor));
                continue;
            }

            int contentTop = panel.y + HEADER_HEIGHT;
            int contentBottom = panel.y + panelHeight;
            nvg.add(() -> NVGRenderer.pushScissor(px, contentTop, COLUMN_WIDTH, contentBottom - contentTop));

            int rowY = contentTop - panel.scroll;
            for (Module module : visible) {
                boolean hovered = mouseY >= contentTop && mouseY <= contentBottom
                        && mouseX >= panel.x + 4
                        && mouseX <= panel.x + COLUMN_WIDTH - 4
                        && mouseY >= rowY + 2
                        && mouseY <= rowY + ROW_HEIGHT - 1;

                TOGGLE_ANIM.put(module, module.isEnabled() ? 1f : 0f);
                animate(HOVER_ANIM, module, hovered ? 1f : 0f, 0.22f);
                // Was previously just written and never read back, so settings popped open
                // instantly - actually reading the lerped value here is what makes it slide,
                // matching Melinoe's ModuleButton.kt EaseInOutAnimation on extendAnim.
                animate(EXPAND_ANIM, module, EXPANDED.contains(module) ? 1f : 0f, 0.18f);

                float toggle = module.isEnabled() ? 1f : 0f;
                float hover = easeOutQuart(anim(HOVER_ANIM, module));
                // No easeOutQuart wrapper here (unlike hover) - animate()'s own lerp convergence
                // already decelerates near its target in both directions. Stacking easeOutQuart
                // on top biased heavily toward "already at 1" while opening (looked instant) and
                // "still at 1" while closing (looked like nothing happened, then a sudden snap).
                float expand = anim(EXPAND_ANIM, module);
                // Flat fill matching Melinoe's ModuleButton.kt: solid accent when on, solid
                // gray26 when off, with only a subtle brighten on hover - no gradient/blend.
                int rowColor = brighten(module.isEnabled() ? accentColor() : COLOR_GRAY26, (int) (hover * 10f));

                // Melinoe shows a description tooltip beside a module/setting once hover has
                // been sustained long enough (HoverHandler reaching 100%) - HOVER_ANIM converges
                // toward 1 the same way, so reusing it gives the same "wait a beat" behavior.
                if (hover > 0.97f && !module.getDescription().isEmpty()) {
                    tooltipText = module.getDescription();
                    tooltipX = panel.x + COLUMN_WIDTH + 10;
                    tooltipY = rowY;
                }

                int rowYCapture = rowY;
                int rowColorCapture = rowColor;
                nvg.add(() -> drawModuleRowNVG(px, rowYCapture, COLUMN_WIDTH, ROW_HEIGHT, rowColorCapture));
                // Melinoe's 18f module-name size assumes its 240px-wide panel - Runal's is 124px,
                // so copying that size verbatim overflowed/clipped long names ("Accessory Cooldown"
                // etc). Sized to actually fit the column, with truncation as a hard safety net.
                String moduleName = truncateToFitNVG(module.getName(), COLUMN_WIDTH - 12, MODULE_TEXT_SIZE);
                int textColor = mixColor(COLOR_TEXT, 0xFFFFFFFF, Math.max(toggle, hover * 0.45f));
                nvg.add(() -> NVGRenderer.textCentered(moduleName, px, rowYCapture, COLUMN_WIDTH, ROW_HEIGHT, MODULE_TEXT_SIZE, textColor));

                rowY += ROW_HEIGHT;
                if (expand > 0.025f && !module.getSettings().isEmpty()) {
                    List<ModuleSetting> settings = visibleSettings(module);
                    int visibleHeight = (int) (totalSettingsHeight(settings) * expand);
                    int subStart = rowY;
                    int drawn = 0;

                    for (ModuleSetting setting : settings) {
                        if (drawn >= visibleHeight) break;
                        int allowedHeight = Math.min(settingRowHeight(setting), visibleHeight - drawn);
                        boolean subHovered = mouseY >= contentTop && mouseY <= contentBottom
                                && mouseX >= panel.x + 7
                                && mouseX <= panel.x + COLUMN_WIDTH - 5
                                && mouseY >= rowY + 1
                                && mouseY <= rowY + allowedHeight - 1;
                        boolean listening = setting instanceof KeybindModuleSetting keybind && keybind.isListening();

                        if (subHovered && !setting.getDescription().isEmpty()) {
                            tooltipText = setting.getDescription();
                            tooltipX = px + COLUMN_WIDTH + 10;
                            tooltipY = rowY;
                        }

                        if (allowedHeight >= 9) {
                            int settingRowY = rowY;
                            if (setting instanceof ColorModuleSetting colorSetting) {
                                // No hex readout on the collapsed row - matches Melinoe's ColorSetting.kt,
                                // which only shows label + swatch until the picker itself is opened.
                                nvg.add(() -> NVGRenderer.textLeft(colorSetting.getLabel(), px + SUB_TEXT_LEFT_PADDING, settingRowY, PICKER_TOP_ROW, SETTING_TEXT_SIZE, 0xFFFFFFFF));
                                nvg.add(() -> drawSettingControlNVG(colorSetting, px, settingRowY, PICKER_TOP_ROW, subHovered));
                                if (colorSetting.isExtended()) {
                                    int pickerY = settingRowY + PICKER_TOP_ROW + PICKER_GAP;
                                    nvg.add(() -> drawColorPickerNVG(colorSetting, px, pickerY, COLUMN_WIDTH));
                                    nvg.add(() -> drawColorPickerHexText(colorSetting, px, pickerY, COLUMN_WIDTH));
                                }
                            } else {
                                // Toggles show state via the switch itself, not text - matches
                                // Melinoe's BooleanSetting.kt, which never draws "On"/"Off".
                                boolean isToggle = setting instanceof ToggleModuleSetting;
                                // Enum/Keybind values sit centered inside the chip drawn by
                                // drawSettingControlNVG, in pure white - matches Melinoe's
                                // SelectorSetting.kt/KeybindSetting.kt exactly.
                                boolean isChip = setting instanceof EnumModuleSetting || setting instanceof KeybindModuleSetting;
                                String valueText = isToggle ? "" : setting.getDisplayValue();
                                int valueWidth = (int) estimateNvgTextWidth(valueText, SETTING_TEXT_SIZE);
                                boolean textEditing = setting instanceof TextModuleSetting textSetting && textSetting.isEditing();
                                int valueColor = isChip ? (listening ? 0xFFFFD966 : 0xFFFFFFFF) : (listening || textEditing ? accentColor() : 0xFFFFFFFF);
                                int valueX = isChip ? px + COLUMN_WIDTH - valueWidth - 15 : px + COLUMN_WIDTH - valueWidth - 12;
                                // Truncate the label if it would otherwise run under the value/chip -
                                // COLUMN_WIDTH is narrow enough that long labels + long values collide.
                                int labelMaxWidth = isToggle ? COLUMN_WIDTH - SUB_TEXT_LEFT_PADDING - 40 : valueX - SUB_TEXT_LEFT_PADDING - px - 10;
                                String labelText = truncateToFitNVG(setting.getLabel(), labelMaxWidth, SETTING_TEXT_SIZE);
                                nvg.add(() -> {
                                    NVGRenderer.textLeft(labelText, px + SUB_TEXT_LEFT_PADDING, settingRowY, allowedHeight, SETTING_TEXT_SIZE, 0xFFFFFFFF);
                                    if (!isToggle) {
                                        NVGRenderer.textLeft(valueText, valueX, settingRowY, allowedHeight, SETTING_TEXT_SIZE, valueColor);
                                    }
                                });
                                nvg.add(() -> drawSettingControlNVG(setting, px, settingRowY, allowedHeight, subHovered));
                                if (setting instanceof SliderModuleSetting slider) {
                                    int sliderRowY = rowY;
                                    int sliderHeight = allowedHeight;
                                    nvg.add(() -> drawSliderNVG(slider, px, sliderRowY, sliderHeight));
                                }
                            }
                        }

                        rowY += allowedHeight;
                        drawn += allowedHeight;
                    }

                    rowY = subStart + visibleHeight;
                }
            }

            nvg.add(NVGRenderer::popScissor);
            // Use the actual accumulated row position, not panelHeight - animatedPanelHeight()
            // still carries +5/+4 padding left over from the old inset row style, which would
            // otherwise leave a gap between the last row and this cap.
            int capY = Math.min(rowY, contentBottom);
            nvg.add(() -> drawPanelBottomCapNVG(px, capY, COLUMN_WIDTH, 10, capColor));
            if (maxScroll > 0) {
                int fh = fullHeight;
                nvg.add(() -> drawScrollbarNVG(panel, ph, fh));
            }
        }

        // Built here (coordinates already known) but drawn in a separate, later PiP pass below -
        // queuing it into the same `nvg` list as the panels wasn't enough to guarantee it landed
        // on top of *other* panels next to the one it's for.
        Runnable tooltipDraw = null;
        if (tooltipText != null) {
            String finalTooltip = tooltipText;
            int estWidth = (int) estimateNvgTextWidth(finalTooltip, SETTING_TEXT_SIZE) + 16;
            int tooltipH = SUB_ROW_HEIGHT + 6;
            int boxX = tooltipX;
            if (boxX + estWidth > clickGuiViewportWidth()) {
                boxX = tooltipX - COLUMN_WIDTH - estWidth - 20;
            }
            int finalBoxX = boxX;
            int finalTooltipY = tooltipY;
            tooltipDraw = () -> {
                NVGRenderer.dropShadow(finalBoxX, finalTooltipY, estWidth, tooltipH, 8f, 1f, 3f);
                NVGRenderer.rect(finalBoxX, finalTooltipY, estWidth, tooltipH, COLOR_GRAY26, 3f);
                NVGRenderer.hollowRect(finalBoxX, finalTooltipY, estWidth, tooltipH, 1f, accentColor(), 3f);
                NVGRenderer.textCentered(finalTooltip, finalBoxX, finalTooltipY, estWidth, tooltipH, SETTING_TEXT_SIZE, 0xFFFFFFFF);
            };
        }
        Runnable finalTooltipDraw = tooltipDraw;

        //? if 1.21.4 {
        /*context.pose().popPose();
        *///?} else {
        context.pose().popMatrix();
        //?}

        // Copy Melinoe's independent resolution scale exactly. Minecraft's GUI Scale
        // may resize vanilla widgets/text, but it must never inflate these panel boxes.
        float guiScale = getStandardGuiScale();
        int screenW = this.width;
        int screenH = this.height;
        NVGPIPRenderer.draw(context, 0, 0, context.guiWidth(), context.guiHeight(), () -> {
            NVGRenderer.scale(guiScale, guiScale);
            NVGRenderer.push();
            NVGRenderer.translate(screenW / 2f, screenH / 2f);
            NVGRenderer.scale(scale, scale);
            NVGRenderer.translate(-screenW / 2f, -screenH / 2f);
            for (Runnable r : nvg) r.run();
            NVGRenderer.pop();
        });

        // These two controls are still vanilla GUI elements, so they intentionally
        // keep Minecraft's own GUI-scaled coordinates.
        drawSearchChrome(context, minecraftMouseX, minecraftMouseY);
        drawResetButton(context, minecraftMouseX, minecraftMouseY);

        // Separate, later PiP call: NVGPIPRenderer.draw(...) calls composite in the order
        // they're issued, so a second call made strictly after the panels' call is guaranteed
        // to land on top of all of it, including neighboring panels the tooltip overlaps.
        if (finalTooltipDraw != null) {
            NVGPIPRenderer.draw(context, 0, 0, context.guiWidth(), context.guiHeight(), () -> {
                NVGRenderer.scale(guiScale, guiScale);
                finalTooltipDraw.run();
            });
        }
        //?}
    }

    /** Header cap: rounded top corners only, flat fill - matches Melinoe's Panel.kt exactly. */
    private void drawPanelHeaderCapNVG(int x, int y, int w, int h) {
        NVGRenderer.halfRoundedRect(x, y, w, h, COLOR_GRAY26, PANEL_RADIUS, true);
    }

    /** Bottom cap: rounded bottom corners only, colored by whether the last module is on. */
    private void drawPanelBottomCapNVG(int x, int y, int w, int h, int color) {
        NVGRenderer.halfRoundedRect(x, y, w, h, color, PANEL_RADIUS, false);
    }

    private void drawPanelShadowNVG(int x, int y, int w, int h) {
        NVGRenderer.dropShadow(x, y, w, h, 10f, 3f, PANEL_RADIUS);
    }

    /** Flat, seamless module row - no radius, no border, no glow. Matches ModuleButton.kt. */
    private void drawModuleRowNVG(int x, int y, int w, int h, int color) {
        NVGRenderer.rect(x, y, w, h, color);
    }

    /** Toggle/color/group controls as flat square vector shapes - no radius. */
    private void drawSettingControlNVG(ModuleSetting setting, int x, int y, int h, boolean hovered) {
        int controlY = y + 4;

        if (setting instanceof ToggleModuleSetting toggle) {
            boolean on = toggle.getValue();
            int bg = on ? accentColor() : 0xFF2A2D34;
            // Sized to fit inside a row (h is as small as SUB_ROW_HEIGHT=16) so adjacent
            // toggles never overflow into each other - the old fixed 34x20 track was taller
            // than the row itself and visibly bled into neighboring rows.
            float trackW = 20f, trackH = Math.min(11f, h - 4f);
            float trackX = x + COLUMN_WIDTH - 30;
            float trackY = y + (h - trackH) / 2f;
            NVGRenderer.rect(trackX, trackY, trackW, trackH, bg, trackH / 2f);
            float knobR = trackH / 2f - 1.5f;
            // Knob slides between off/on positions instead of snapping, matching Melinoe's
            // BooleanSetting.kt (LinearAnimation over the knob's x position).
            animateSetting(TOGGLE_KNOB_ANIM, toggle, on ? 1f : 0f, 0.3f);
            float knobT = animSetting(TOGGLE_KNOB_ANIM, toggle);
            float knobX = lerp(trackX + knobR, trackX + trackW - knobR, knobT);
            NVGRenderer.circle(knobX, trackY + trackH / 2f, knobR, 0xFFFFFFFF);
        } else if (setting instanceof ColorModuleSetting color) {
            NVGRenderer.rect(x + COLUMN_WIDTH - 25, controlY - 1, 14, 10, COLOR_GRAY26, 2f);
            NVGRenderer.rect(x + COLUMN_WIDTH - 24, controlY, 12, 8, color.getColor(), 1.5f);
        } else if (setting instanceof SettingGroup group) {
            // Down when collapsed, sideways (rotated 90 degrees) when expanded - matches
            // Melinoe's DropdownSetting.kt chevron rotation exactly.
            drawChevronNVG(x + COLUMN_WIDTH - 16, y + h / 2f, 5f, group.isExpanded(), 0xFFFFFFFF);
        } else if (setting instanceof EnumModuleSetting || setting instanceof KeybindModuleSetting) {
            // Bordered chip matching Melinoe's SelectorSetting.kt/KeybindSetting.kt: gray38 fill,
            // accent-colored hollow outline, pure white text (drawn separately via overlay).
            // Must measure with the SAME renderer/size the text itself draws with (NVG at
            // SETTING_TEXT_SIZE) - measuring via the old vanilla font gave a mismatched width,
            // which is why the text looked off-center inside the chip.
            String value = setting.getDisplayValue();
            int valueWidth = (int) NVGRenderer.textWidth(value, SETTING_TEXT_SIZE);
            float chipW = valueWidth + 10f;
            float chipH = Math.min(15f, h - 2f);
            float chipX = x + COLUMN_WIDTH - 10 - chipW;
            float chipY = y + (h - chipH) / 2f;
            boolean listening = setting instanceof KeybindModuleSetting keybind && keybind.isListening();
            NVGRenderer.rect(chipX, chipY, chipW, chipH, 0xFF2A2D34, 4f);
            NVGRenderer.hollowRect(chipX, chipY, chipW, chipH, 1.3f, listening ? 0xFFFFD966 : accentColor(), 4f);
        }
    }

    /** Chevron pointing down (collapsed) or sideways (expanded) - replaces the old +/- text glyph. */
    private void drawChevronNVG(float centerX, float centerY, float armLength, boolean expanded, int color) {
        if (expanded) {
            // Points right: apex at (centerX + armLength, centerY)
            NVGRenderer.line(centerX - armLength * 0.4f, centerY - armLength, centerX + armLength * 0.4f, centerY, 1.6f, color);
            NVGRenderer.line(centerX + armLength * 0.4f, centerY, centerX - armLength * 0.4f, centerY + armLength, 1.6f, color);
        } else {
            // Points down: apex at (centerX, centerY + armLength)
            NVGRenderer.line(centerX - armLength, centerY - armLength * 0.4f, centerX, centerY + armLength * 0.4f, 1.6f, color);
            NVGRenderer.line(centerX, centerY + armLength * 0.4f, centerX + armLength, centerY - armLength * 0.4f, 1.6f, color);
        }
    }

    /** Bounds of the picker's saturation/brightness square, in the same space used to draw it. */
    private float[] colorPickerSquareBounds(int x, int y, int w) {
        int margin = 8;
        return new float[]{x + margin, y, w - margin * 2f, PICKER_SQUARE_H};
    }

    private float[] colorPickerHueBounds(int x, int y, int w) {
        float[] square = colorPickerSquareBounds(x, y, w);
        return new float[]{square[0], square[1] + square[3] + PICKER_GAP, square[2], PICKER_HUE_H};
    }

    /** Melinoe-style HSB color picker: draggable saturation/brightness square + hue strip. */
    private void drawColorPickerNVG(ColorModuleSetting colorSetting, int x, int y, int w) {
        float[] hsb = colorSetting.getHSB();
        float[] square = colorPickerSquareBounds(x, y, w);
        float squareX = square[0], squareY = square[1], squareW = square[2], squareH = square[3];

        int hueColor = 0xFF000000 | (java.awt.Color.HSBtoRGB(hsb[0], 1f, 1f) & 0xFFFFFF);
        NVGRenderer.gradientRect(squareX, squareY, squareW, squareH, 0xFFFFFFFF, hueColor, NVGRenderer.Gradient.LEFT_TO_RIGHT, 3f);
        NVGRenderer.gradientRect(squareX, squareY, squareW, squareH, 0x00000000, 0xFF000000, NVGRenderer.Gradient.TOP_TO_BOTTOM, 3f);

        float pointerX = squareX + hsb[1] * squareW;
        float pointerY = squareY + (1f - hsb[2]) * squareH;
        NVGRenderer.circle(pointerX, pointerY, 5f, 0xFFFFFFFF);
        NVGRenderer.circle(pointerX, pointerY, 4f, 0xFF000000 | (colorSetting.getColor() & 0xFFFFFF));

        float[] hue = colorPickerHueBounds(x, y, w);
        // Six real 2-color gradients back to back (red->yellow->green->cyan->blue->magenta->red)
        // instead of many flat color chips - each segment's end color exactly matches the next
        // segment's start color, so there's no visible seam anywhere, just a continuous rainbow.
        int stops = 6;
        float segW = hue[2] / stops;
        for (int i = 0; i < stops; i++) {
            int startColor = 0xFF000000 | (java.awt.Color.HSBtoRGB(i / (float) stops, 1f, 1f) & 0xFFFFFF);
            int endColor = 0xFF000000 | (java.awt.Color.HSBtoRGB((i + 1) / (float) stops, 1f, 1f) & 0xFFFFFF);
            NVGRenderer.gradientRect(hue[0] + i * segW, hue[1], segW + 0.5f, hue[3], startColor, endColor, NVGRenderer.Gradient.LEFT_TO_RIGHT, 0f);
        }
        float huePointerX = hue[0] + hsb[0] * hue[2];
        NVGRenderer.circle(huePointerX, hue[1] + hue[3] / 2f, 5f, 0xFFFFFFFF);
        NVGRenderer.circle(huePointerX, hue[1] + hue[3] / 2f, 4f, hueColor);

        float hexY = hue[1] + hue[3] + PICKER_GAP;
        NVGRenderer.rect(squareX, hexY, squareW, PICKER_HEX_H, 0xFF2A2D34, 3f);
        NVGRenderer.hollowRect(squareX, hexY, squareW, PICKER_HEX_H, 1f, colorSetting.isHexEditing() ? accentColor() : 0xFF3A3C44, 3f);
    }

    private void drawColorPickerHexText(ColorModuleSetting colorSetting, int x, int y, int w) {
        float[] hue = colorPickerHueBounds(x, y, w);
        float[] square = colorPickerSquareBounds(x, y, w);
        float hexY = hue[1] + hue[3] + PICKER_GAP;
        String text = "#" + colorSetting.getHexInput();
        int color = colorSetting.isHexEditing() ? accentColor() : 0xFFFFFFFF;
        NVGRenderer.textCentered(text, square[0], hexY, square[2], PICKER_HEX_H, SETTING_TEXT_SIZE, color);
    }

    private void updateColorFromSquareDrag(ColorModuleSetting colorSetting, float[] square, int mouseX, int mouseY) {
        float sat = Math.max(0f, Math.min(1f, (mouseX - square[0]) / square[2]));
        float bright = Math.max(0f, Math.min(1f, 1f - (mouseY - square[1]) / square[3]));
        float[] hsb = colorSetting.getHSB();
        colorSetting.setHSB(hsb[0], sat, bright);
    }

    private void updateColorFromHueDrag(ColorModuleSetting colorSetting, float[] hue, int mouseX) {
        float h = Math.max(0f, Math.min(1f, (mouseX - hue[0]) / hue[2]));
        float[] hsb = colorSetting.getHSB();
        colorSetting.setHSB(h, hsb[1], hsb[2]);
    }

    private void drawSliderNVG(SliderModuleSetting slider, int x, int y, int h) {
        int trackX = x + 62;
        float trackY = y + h - 4.5f;
        int trackW = COLUMN_WIDTH - 80;
        float value = Math.max(0f, Math.min(1f, slider.getNormalizedValue()));
        int fillW = (int) (trackW * value);
        NVGRenderer.rect(trackX, trackY, trackW, 3, 0xFF2A2D34, 1.5f);
        NVGRenderer.rect(trackX, trackY, fillW, 3, accentColor(), 1.5f);
        NVGRenderer.circle(trackX + fillW, trackY + 1.5f, 3.5f, 0xFFEDEDF2);
    }

    private void drawScrollbarNVG(Panel panel, int panelHeight, int fullHeight) {
        int trackTop = panel.y + HEADER_HEIGHT + 2;
        int trackBottom = panel.y + panelHeight - 6;
        int trackHeight = trackBottom - trackTop;
        if (trackHeight <= 4) return;

        int maxScroll = Math.max(1, fullHeight - panelHeight);
        int thumbHeight = Math.min(trackHeight, Math.max(14, (int) (trackHeight * (panelHeight / (float) fullHeight))));
        float progress = maxScroll <= 0 ? 0f : panel.scroll / (float) maxScroll;
        int thumbY = trackTop + (int) ((trackHeight - thumbHeight) * progress);
        int barX = panel.x + COLUMN_WIDTH - 5;

        NVGRenderer.rect(barX, trackTop, 2, trackBottom - trackTop, alpha(0xFFFFFFFF, 0.08f), 1);
        NVGRenderer.rect(barX, thumbY, 2, thumbHeight, alpha(accentColor(), 0.55f), 1);
    }

    //? if 1.21.4 {
    /*@Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (handleResetClick((int) mouseX, (int) mouseY, button)) return true;
        if (handleMouseClicked(interactionMouseX(mouseX), interactionMouseY(mouseY), button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }
    *///?} else {
    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubled) {
        if (handleResetClick((int) mouseButtonEvent.x(), (int) mouseButtonEvent.y(), mouseButtonEvent.button())) {
            return true;
        }
        if (handleMouseClicked(
                interactionMouseX(mouseButtonEvent.x()),
                interactionMouseY(mouseButtonEvent.y()),
                mouseButtonEvent.button()
        )) return true;
        return super.mouseClicked(mouseButtonEvent, doubled);
    }
    //?}

    private boolean handleResetClick(int mouseX, int mouseY, int button) {
        if (button == 0) {
            int[] resetBounds = resetButtonBounds();
            if (mouseX >= resetBounds[0] && mouseX <= resetBounds[0] + resetBounds[2]
                    && mouseY >= resetBounds[1] && mouseY <= resetBounds[1] + resetBounds[3]) {
                resetAllSettings();
                return true;
            }
        }
        return false;
    }

    private boolean handleMouseClicked(int mouseX, int mouseY, int button) {
        for (Panel panel : panels) {
            if (mouseX >= panel.x
                    && mouseX <= panel.x + COLUMN_WIDTH
                    && mouseY >= panel.y
                    && mouseY <= panel.y + HEADER_HEIGHT) {
                if (button == 1 || mouseX >= panel.x + COLUMN_WIDTH - 24) {
                    toggleCategory(panel.category);
                    return true;
                }

                panel.dragging = true;
                panel.dragOffsetX = mouseX - panel.x;
                panel.dragOffsetY = mouseY - panel.y;
                return true;
            }

            if (COLLAPSED_CATEGORIES.contains(panel.category)) continue;

            List<Module> visible = visibleModules(panel);
            if (visible.isEmpty()) continue;

            int fullHeight = animatedPanelHeight(panel, visible);
            int panelHeight = Math.min(fullHeight, panelMaxHeight(panel));
            int contentTop = panel.y + HEADER_HEIGHT;
            int contentBottom = panel.y + panelHeight - 4;
            if (mouseY < contentTop || mouseY > contentBottom) continue;

            int rowY = contentTop + 3 - panel.scroll;

            for (Module module : visible) {
                boolean inRowBounds = mouseX >= panel.x + 4
                        && mouseX <= panel.x + COLUMN_WIDTH - 4
                        && mouseY >= rowY + 2
                        && mouseY <= rowY + ROW_HEIGHT - 1;

                if (inRowBounds) {
                    if (button == 1 || mouseX >= panel.x + COLUMN_WIDTH - 24) {
                        if (EXPANDED.contains(module)) EXPANDED.remove(module);
                        else if (!module.getSettings().isEmpty()) EXPANDED.add(module);
                    } else {
                        module.toggle();
                        ModuleConfig.save();
                    }
                    return true;
                }

                rowY += ROW_HEIGHT;
                if (EXPANDED.contains(module)) {
                    for (ModuleSetting setting : visibleSettings(module)) {
                        int rowHeight = settingRowHeight(setting);
                        boolean inSubBounds = mouseX >= panel.x + 7
                                && mouseX <= panel.x + COLUMN_WIDTH - 5
                                && mouseY >= rowY + 1
                                && mouseY <= rowY + rowHeight - 1;

                        if (inSubBounds) {
                            if (button == 1 && setting instanceof KeybindModuleSetting keybind) {
                                keybind.clear();
                                return true;
                            }

                            if (button == 0) {
                                if (setting instanceof SliderModuleSetting slider) {
                                    draggingSlider = slider;
                                    draggingSliderPanelX = panel.x;
                                    setSliderFromMouse(slider, panel.x, mouseX);
                                } else if (setting instanceof ColorModuleSetting colorSetting) {
                                    handleColorPickerClick(colorSetting, panel.x, rowY, mouseX, mouseY);
                                } else {
                                    setting.onClick();
                                    if (setting instanceof TextModuleSetting text) editingText = text;
                                }
                                return true;
                            }
                        }

                        rowY += rowHeight;
                    }
                }
            }
        }

        return false;
    }

    //? if 1.21.4 {
    /*@Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (handleMouseDragged(interactionMouseX(mouseX), interactionMouseY(mouseY))) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    *///?} else {
    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double dragX, double dragY) {
        if (handleMouseDragged(
                interactionMouseX(mouseButtonEvent.x()),
                interactionMouseY(mouseButtonEvent.y())
        )) return true;
        return super.mouseDragged(mouseButtonEvent, dragX, dragY);
    }
    //?}

    private boolean handleMouseDragged(int mouseX, int mouseY) {
        if (draggingSlider != null) {
            setSliderFromMouse(draggingSlider, draggingSliderPanelX, mouseX);
            return true;
        }

        if (draggingColor != null) {
            int pickerY = draggingColorRowY + PICKER_TOP_ROW + PICKER_GAP;
            Integer section = draggingColor.getDragSection();
            if (section != null && section == 0) {
                updateColorFromSquareDrag(draggingColor, colorPickerSquareBounds(draggingColorPanelX, pickerY, COLUMN_WIDTH), mouseX, mouseY);
            } else if (section != null && section == 1) {
                updateColorFromHueDrag(draggingColor, colorPickerHueBounds(draggingColorPanelX, pickerY, COLUMN_WIDTH), mouseX);
            }
            return true;
        }

        for (Panel panel : panels) {
            if (panel.dragging) {
                panel.x = mouseX - panel.dragOffsetX;
                panel.y = mouseY - panel.dragOffsetY;
                SAVED_POSITIONS.put(panel.category, new int[]{panel.x, panel.y});
                return true;
            }
        }

        return false;
    }

    //? if 1.21.4 {
    /*@Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        handleMouseReleased();
        return super.mouseReleased(mouseX, mouseY, button);
    }
    *///?} else {
    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        handleMouseReleased();
        return super.mouseReleased(mouseButtonEvent);
    }
    //?}

    private void handleMouseReleased() {
        draggingSlider = null;
        if (draggingColor != null) {
            draggingColor.setDragSection(null);
            draggingColor = null;
        }
        for (Panel panel : panels) {
            panel.dragging = false;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int clickGuiMouseX = interactionMouseX(mouseX);
        int clickGuiMouseY = interactionMouseY(mouseY);
        for (Panel panel : panels) {
            if (COLLAPSED_CATEGORIES.contains(panel.category)) continue;

            List<Module> visible = visibleModules(panel);
            if (visible.isEmpty()) continue;

            int fullHeight = animatedPanelHeight(panel, visible);
            int panelHeight = Math.min(fullHeight, panelMaxHeight(panel));
            int maxScroll = Math.max(0, fullHeight - panelHeight);
            if (maxScroll <= 0) continue;

            boolean inside = clickGuiMouseX >= panel.x && clickGuiMouseX <= panel.x + COLUMN_WIDTH
                    && clickGuiMouseY >= panel.y && clickGuiMouseY <= panel.y + panelHeight;
            if (inside) {
                panel.scroll = Math.max(0, Math.min(maxScroll, panel.scroll - (int) (scrollY * SCROLL_SPEED)));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    //? if 1.21.4 {
    /*@Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (handleKeyPressed(keyCode, scanCode)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    *///?} else {
    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent keyEvent) {
        if (handleKeyPressed(keyEvent.key(), keyEvent.scancode())) return true;
        return super.keyPressed(keyEvent);
    }
    //?}

    private boolean handleKeyPressed(int keyCode, int scanCode) {
        if (editingText != null && editingText.isEditing()) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER) {
                editingText.stopEditing();
                editingText = null;
                return true;
            }
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
                editingText.backspace();
                return true;
            }
        }

        if (editingColor != null && editingColor.isHexEditing()) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER) {
                editingColor.stopHexEditing();
                editingColor = null;
                return true;
            }
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
                editingColor.backspaceHex();
                return true;
            }
        }

        for (Module module : ModuleManager.getModules()) {
            for (ModuleSetting setting : visibleSettings(module)) {
                if (setting instanceof KeybindModuleSetting keybind && keybind.isListening()) {
                    if (keybind.handleKeyPress(keyCode, scanCode)) return true;
                }
            }
        }
        return false;
    }

    //? if 1.21.4 {
    /*@Override
    public boolean charTyped(char chr, int modifiers) {
        if (handleCharTyped(chr)) return true;
        return super.charTyped(chr, modifiers);
    }
    *///?} else {
    @Override
    public boolean charTyped(CharacterEvent event) {
        if (handleCharTyped((char) event.codepoint())) return true;
        return super.charTyped(event);
    }
    //?}

    private boolean handleCharTyped(char chr) {
        if (editingText != null && editingText.isEditing()) {
            editingText.append(chr);
            return true;
        }
        if (editingColor != null && editingColor.isHexEditing()) {
            editingColor.appendHex(chr);
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        ModuleConfig.save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
