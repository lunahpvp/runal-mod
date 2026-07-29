package com.runal.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
//? if 1.21.4 {
/*import net.minecraft.client.renderer.RenderType;
*///?}

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
//? if 1.21.4 {
//?} else {
import java.util.function.Supplier;
//?}

/**
 * CPU-rasterized Inter text for graphics backends that cannot run NanoVGGL3.
 *
 * Each string is rasterized directly from the TTF at its final physical size. The
 * resulting exact-size texture is submitted through Minecraft's normal GUI texture
 * pipeline, which works on both the vanilla OpenGL backend and VulkanMod. No Minecraft
 * glyph atlas or fractional glyph scaling is involved.
 */
public final class PortableTextRenderer {
    private static final Identifier FONT_RESOURCE =
            Identifier.fromNamespaceAndPath("scepterutils", "font/inter-28pt-medium.ttf");
    private static final int MAX_CACHE_ENTRIES = 512;
    private static final FontRenderContext FONT_CONTEXT =
            new FontRenderContext(new AffineTransform(), true, true);
    private static final Map<TextKey, TextTexture> CACHE =
            new LinkedHashMap<>(64, 0.75f, true);

    private static Font baseFont;
    private static long nextTextureId;

    private PortableTextRenderer() {
    }

    public static int width(String text, int size) {
        return get(text, size, 0xFFFFFFFF).width;
    }

    public static int height(String text, int size) {
        return get(text, size, 0xFFFFFFFF).height;
    }

    public static void draw(
            GuiGraphicsExtractor context,
            String text,
            int x,
            int y,
            int size,
            int color
    ) {
        if (text == null || text.isEmpty()) return;
        TextTexture texture = get(text, size, color);
        //? if 1.21.4 {
        /*context.blit(
                RenderType::guiTextured,
                texture.id,
                x,
                y,
                0f,
                0f,
                texture.width,
                texture.height,
                texture.width,
                texture.height
        );
        *///?} else {
        context.blit(
                texture.id,
                x,
                y,
                x + texture.width,
                y + texture.height,
                0f,
                1f,
                0f,
                1f
        );
        //?}
    }

    public static void clear() {
        Minecraft minecraft = Minecraft.getInstance();
        for (TextTexture texture : CACHE.values()) {
            minecraft.getTextureManager().release(texture.id);
        }
        CACHE.clear();
    }

    private static TextTexture get(String text, int size, int color) {
        TextKey key = new TextKey(text, size, color);
        TextTexture cached = CACHE.get(key);
        if (cached != null) return cached;

        TextTexture created = rasterize(key);
        CACHE.put(key, created);
        trimCache();
        return created;
    }

    private static TextTexture rasterize(TextKey key) {
        Font font = font().deriveFont((float) key.size);
        GlyphVector glyphs = font.createGlyphVector(FONT_CONTEXT, key.text);
        var bounds = glyphs.getPixelBounds(FONT_CONTEXT, 0f, 0f);
        int padding = 2;
        int width = Math.max(1, bounds.width + padding * 2);
        int height = Math.max(1, bounds.height + padding * 2);

        BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = buffered.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setColor(new Color(key.color, true));
            graphics.drawGlyphVector(glyphs, padding - bounds.x, padding - bounds.y);
        } finally {
            graphics.dispose();
        }

        NativeImage image = new NativeImage(width, height, true);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setPixel(x, y, buffered.getRGB(x, y));
            }
        }

        String textureName = Integer.toUnsignedString(key.hashCode(), 36)
                + "_" + Long.toUnsignedString(nextTextureId++, 36);
        Identifier id = Identifier.fromNamespaceAndPath("runal", "portable_text/" + textureName);
        //? if 1.21.4 {
        /*DynamicTexture texture = new DynamicTexture(image);
        *///?} else {
        Supplier<String> label = () -> "Runal portable text " + textureName;
        DynamicTexture texture = new DynamicTexture(label, image);
        //?}
        Minecraft.getInstance().getTextureManager().register(id, texture);
        texture.upload();
        return new TextTexture(id, width, height);
    }

    private static Font font() {
        if (baseFont != null) return baseFont;
        try (InputStream stream = Minecraft.getInstance().getResourceManager()
                .getResource(FONT_RESOURCE)
                .orElseThrow()
                .open()) {
            baseFont = Font.createFont(Font.TRUETYPE_FONT, stream);
            return baseFont;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load Runal's Inter font", exception);
        }
    }

    private static void trimCache() {
        Minecraft minecraft = Minecraft.getInstance();
        while (CACHE.size() > MAX_CACHE_ENTRIES) {
            var iterator = CACHE.entrySet().iterator();
            if (!iterator.hasNext()) return;
            TextTexture oldest = iterator.next().getValue();
            iterator.remove();
            minecraft.getTextureManager().release(oldest.id);
        }
    }

    private record TextKey(String text, int size, int color) {
    }

    private record TextTexture(Identifier id, int width, int height) {
    }
}
