package com.runal.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL3.*;
import static org.lwjgl.system.MemoryUtil.memAlloc;

public final class NVGRenderer {

    public enum Gradient { LEFT_TO_RIGHT, TOP_TO_BOTTOM }

    private static final NVGPaint NVG_PAINT = NVGPaint.malloc();
    private static final NVGColor NVG_COLOR = NVGColor.malloc();
    private static final NVGColor NVG_COLOR_2 = NVGColor.malloc();
    private static final float[] TEXT_BOUNDS = new float[4];

    private static long vg = -1L;
    private static boolean drawing = false;
    private static Scissor scissor;
    private static int fontId = -1;

    private NVGRenderer() {
    }

    public static void init() {
        if (vg != -1L) return;
        vg = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
        if (vg == -1L) throw new IllegalStateException("[NVGRenderer] Failed to initialize NanoVG");
        loadFont();
    }

    private static void loadFont() {
        try (InputStream stream = Minecraft.getInstance().getResourceManager()
                .getResource(Identifier.fromNamespaceAndPath("scepterutils", "font/inter-28pt-medium.ttf"))
                .orElseThrow().open()) {
            byte[] bytes = stream.readAllBytes();
            ByteBuffer buffer = memAlloc(bytes.length);
            buffer.put(bytes).flip();
            fontId = nvgCreateFontMem(vg, "inter", buffer, true);
            if (fontId == -1) throw new IllegalStateException("nvgCreateFontMem failed");
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("[NVGRenderer] Failed to load inter-28pt-medium.ttf", e);
        }
    }

    /**
     * GLFW reports a 0x0 window while minimized/alt-tabbed out of fullscreen, but Minecraft
     * keeps the last GUI-scaled dimensions. Mixing the two produces divide-by-zero layout,
     * so callers should skip the frame entirely when this returns false.
     */
    public static boolean hasDrawableSize() {
        Minecraft mc = Minecraft.getInstance();
        var window = mc.getWindow();
        if (window == null) return false;
        return window.getScreenWidth() > 0 && window.getScreenHeight() > 0
                && window.getGuiScaledWidth() > 0 && window.getGuiScaledHeight() > 0;
    }

    public static float devicePixelRatio() {
        try {
            var window = Minecraft.getInstance().getWindow();
            int fbw = window.getWidth();
            int ww = window.getScreenWidth();
            return ww == 0 ? 1f : (float) fbw / (float) ww;
        } catch (Throwable t) {
            return 1f;
        }
    }

    public static void beginFrame(float width, float height) {
        if (drawing) throw new IllegalStateException("[NVGRenderer] Already drawing, but called beginFrame");
        init();
        float dpr = devicePixelRatio();
        nvgBeginFrame(vg, width / dpr, height / dpr, dpr);
        drawing = true;
    }

    public static void endFrame() {
        if (!drawing) throw new IllegalStateException("[NVGRenderer] Not drawing, but called endFrame");
        nvgEndFrame(vg);
        drawing = false;
    }

    public static void push() {
        nvgSave(vg);
    }

    public static void pop() {
        nvgRestore(vg);
    }

    public static void scale(float x, float y) {
        nvgScale(vg, x, y);
    }

    public static void translate(float x, float y) {
        nvgTranslate(vg, x, y);
    }

    public static void globalAlpha(float amount) {
        nvgGlobalAlpha(vg, Math.max(0f, Math.min(1f, amount)));
    }

    public static void pushScissor(float x, float y, float w, float h) {
        scissor = new Scissor(scissor, x, y, w + x, h + y);
        scissor.apply();
    }

    public static void popScissor() {
        nvgResetScissor(vg);
        scissor = scissor == null ? null : scissor.previous;
        if (scissor != null) scissor.apply();
    }

    public static void line(float x1, float y1, float x2, float y2, float thickness, int color) {
        nvgBeginPath(vg);
        nvgMoveTo(vg, x1, y1);
        nvgLineTo(vg, x2, y2);
        nvgStrokeWidth(vg, thickness);
        color(color, NVG_COLOR);
        nvgStrokeColor(vg, NVG_COLOR);
        nvgStroke(vg);
    }

    public static void rect(float x, float y, float w, float h, int color) {
        rect(x, y, w, h, color, 0f);
    }

    public static void rect(float x, float y, float w, float h, int color, float radius) {
        nvgBeginPath(vg);
        if (radius > 0f) nvgRoundedRect(vg, x, y, w, h + 0.5f, radius);
        else nvgRect(vg, x, y, w, h + 0.5f);
        color(color, NVG_COLOR);
        nvgFillColor(vg, NVG_COLOR);
        nvgFill(vg);
    }

    public static void halfRoundedRect(float x, float y, float w, float h, int color, float radius, boolean roundTop) {
        // nvgArcTo degenerates badly at radius 0, so just draw a plain rect instead.
        if (radius <= 0.01f) {
            rect(x, y, w, h, color);
            return;
        }
        nvgBeginPath(vg);
        if (roundTop) {
            nvgMoveTo(vg, x, y + h);
            nvgLineTo(vg, x + w, y + h);
            nvgLineTo(vg, x + w, y + radius);
            nvgArcTo(vg, x + w, y, x + w - radius, y, radius);
            nvgLineTo(vg, x + radius, y);
            nvgArcTo(vg, x, y, x, y + radius, radius);
            nvgLineTo(vg, x, y + h);
        } else {
            nvgMoveTo(vg, x, y);
            nvgLineTo(vg, x + w, y);
            nvgLineTo(vg, x + w, y + h - radius);
            nvgArcTo(vg, x + w, y + h, x + w - radius, y + h, radius);
            nvgLineTo(vg, x + radius, y + h);
            nvgArcTo(vg, x, y + h, x, y + h - radius, radius);
            nvgLineTo(vg, x, y);
        }
        nvgClosePath(vg);
        color(color, NVG_COLOR);
        nvgFillColor(vg, NVG_COLOR);
        nvgFill(vg);
    }

    public static void hollowRect(float x, float y, float w, float h, float thickness, int color, float radius) {
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, w, h, radius);
        nvgStrokeWidth(vg, thickness);
        nvgPathWinding(vg, NVG_HOLE);
        color(color, NVG_COLOR);
        nvgStrokeColor(vg, NVG_COLOR);
        nvgStroke(vg);
    }

    public static void gradientRect(float x, float y, float w, float h, int color1, int color2, Gradient direction, float radius) {
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, w, h, radius);
        gradient(color1, color2, x, y, w, h, direction);
        nvgFillPaint(vg, NVG_PAINT);
        nvgFill(vg);
    }

    public static void dropShadow(float x, float y, float width, float height, float blur, float spread, float radius) {
        nvgRGBA((byte) 0, (byte) 0, (byte) 0, (byte) 125, NVG_COLOR);
        nvgRGBA((byte) 0, (byte) 0, (byte) 0, (byte) 0, NVG_COLOR_2);
        nvgBoxGradient(vg, x - spread, y - spread, width + 2 * spread, height + 2 * spread, radius + spread, blur, NVG_COLOR, NVG_COLOR_2, NVG_PAINT);
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x - spread - blur, y - spread - blur, width + 2 * spread + 2 * blur, height + 2 * spread + 2 * blur, radius + spread);
        nvgRoundedRect(vg, x, y, width, height, radius);
        nvgPathWinding(vg, NVG_HOLE);
        nvgFillPaint(vg, NVG_PAINT);
        nvgFill(vg);
    }

    public static void glow(float x, float y, float width, float height, float blur, float spread, float radius, int color) {
        color(color, NVG_COLOR);
        color(color & 0x00FFFFFF, NVG_COLOR_2);
        nvgBoxGradient(vg, x - spread, y - spread, width + 2 * spread, height + 2 * spread, radius + spread, blur, NVG_COLOR, NVG_COLOR_2, NVG_PAINT);
        nvgBeginPath(vg);
        nvgRect(vg, x - spread - blur, y - spread - blur, width + 2 * (spread + blur), height + 2 * (spread + blur));
        nvgFillPaint(vg, NVG_PAINT);
        nvgFill(vg);
    }

    public static void circle(float x, float y, float radius, int color) {
        nvgBeginPath(vg);
        nvgCircle(vg, x, y, radius);
        color(color, NVG_COLOR);
        nvgFillColor(vg, NVG_COLOR);
        nvgFill(vg);
    }

    public static void text(String text, float x, float y, float size, int color) {
        nvgFontFaceId(vg, fontId);
        nvgFontSize(vg, size);
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
        color(color, NVG_COLOR);
        nvgFillColor(vg, NVG_COLOR);
        nvgText(vg, x, y, text);
    }

    public static void textCentered(String text, float boxX, float boxY, float boxW, float boxH, float size, int color) {
        nvgFontFaceId(vg, fontId);
        nvgFontSize(vg, size);
        nvgTextAlign(vg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
        color(color, NVG_COLOR);
        nvgFillColor(vg, NVG_COLOR);
        nvgText(vg, boxX + boxW / 2f, boxY + boxH / 2f, text);
    }

    public static void textLeft(String text, float x, float boxY, float boxH, float size, int color) {
        nvgFontFaceId(vg, fontId);
        nvgFontSize(vg, size);
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        color(color, NVG_COLOR);
        nvgFillColor(vg, NVG_COLOR);
        nvgText(vg, x, boxY + boxH / 2f, text);
    }

    public static float textWidth(String text, float size) {
        nvgFontFaceId(vg, fontId);
        nvgFontSize(vg, size);
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
        return nvgTextBounds(vg, 0f, 0f, text, TEXT_BOUNDS);
    }

    private static void color(int argb, NVGColor out) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        nvgRGBA((byte) r, (byte) g, (byte) b, (byte) a, out);
    }

    private static void gradient(int color1, int color2, float x, float y, float w, float h, Gradient direction) {
        color(color1, NVG_COLOR);
        color(color2, NVG_COLOR_2);
        if (direction == Gradient.LEFT_TO_RIGHT) nvgLinearGradient(vg, x, y, x + w, y, NVG_COLOR, NVG_COLOR_2, NVG_PAINT);
        else nvgLinearGradient(vg, x, y, x, y + h, NVG_COLOR, NVG_COLOR_2, NVG_PAINT);
    }

    private static final class Scissor {
        final Scissor previous;
        final float x, y, maxX, maxY;

        Scissor(Scissor previous, float x, float y, float maxX, float maxY) {
            this.previous = previous;
            this.x = x;
            this.y = y;
            this.maxX = maxX;
            this.maxY = maxY;
        }

        void apply() {
            if (previous == null) {
                nvgScissor(vg, x, y, maxX - x, maxY - y);
            } else {
                float sx = Math.max(x, previous.x);
                float sy = Math.max(y, previous.y);
                float sw = Math.max(0f, Math.min(maxX, previous.maxX) - sx);
                float sh = Math.max(0f, Math.min(maxY, previous.maxY) - sy);
                nvgScissor(vg, sx, sy, sw, sh);
            }
        }
    }
}
