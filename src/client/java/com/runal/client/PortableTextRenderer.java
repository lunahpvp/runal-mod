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
import java.awt.BasicStroke;
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

public final class PortableTextRenderer {
    private static final Identifier FONT_RESOURCE =
            Identifier.fromNamespaceAndPath("scepterutils", "font/inter-28pt-medium.ttf");
    private static final int MAX_CACHE_ENTRIES = 512;
    private static final FontRenderContext FONT_CONTEXT =
            new FontRenderContext(new AffineTransform(), true, true);
    private static final Map<TextKey, TextTexture> CACHE =
            new LinkedHashMap<>(64, 0.75f, true);
    private static final Map<SwatchKey, TextTexture> SWATCH_CACHE =
            new LinkedHashMap<>(16, 0.75f, true);

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
        drawTexture(context, texture, x, y);
    }

    private static void drawTexture(GuiGraphicsExtractor context, TextTexture texture, int x, int y) {
        float inverseScale = 1f / texture.renderScale;
        //? if 1.21.4 {
        /*context.pose().pushPose();
        context.pose().translate(x, y, 0f);
        context.pose().scale(inverseScale, inverseScale, 1f);
        context.blit(
                RenderType::guiTextured,
                texture.id,
                0,
                0,
                0f,
                0f,
                texture.pixelWidth,
                texture.pixelHeight,
                texture.pixelWidth,
                texture.pixelHeight
        );
        context.pose().popPose();
        *///?} else {
        context.pose().pushMatrix();
        context.pose().translate(x, y);
        context.pose().scale(inverseScale, inverseScale);
        context.blit(
                texture.id,
                0,
                0,
                texture.pixelWidth,
                texture.pixelHeight,
                0f,
                1f,
                0f,
                1f
        );
        context.pose().popMatrix();
        //?}
    }

    public static void drawColorSwatch(
            GuiGraphicsExtractor context,
            int x,
            int y,
            int width,
            int height,
            int color
    ) {
        TextTexture texture = getSwatch(width, height, color);
        drawTexture(context, texture, x, y);
    }

    public static void clear() {
        Minecraft minecraft = Minecraft.getInstance();
        for (TextTexture texture : CACHE.values()) {
            minecraft.getTextureManager().release(texture.id);
        }
        CACHE.clear();
        for (TextTexture texture : SWATCH_CACHE.values()) {
            minecraft.getTextureManager().release(texture.id);
        }
        SWATCH_CACHE.clear();
    }

    private static TextTexture get(String text, int size, int color) {
        TextKey key = new TextKey(text, size, color, physicalScale());
        TextTexture cached = CACHE.get(key);
        if (cached != null) return cached;

        TextTexture created = rasterize(key);
        CACHE.put(key, created);
        trimCache();
        return created;
    }

    private static TextTexture rasterize(TextKey key) {
        Font font = font().deriveFont((float) key.size * key.scale);
        GlyphVector glyphs = font.createGlyphVector(FONT_CONTEXT, key.text);
        var bounds = glyphs.getPixelBounds(FONT_CONTEXT, 0f, 0f);
        int padding = 2 * key.scale;
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
        return new TextTexture(
                id,
                width,
                height,
                (int) Math.ceil(width / (double) key.scale),
                (int) Math.ceil(height / (double) key.scale),
                key.scale
        );
    }

    private static TextTexture getSwatch(int width, int height, int color) {
        SwatchKey key = new SwatchKey(width, height, color, physicalScale());
        TextTexture cached = SWATCH_CACHE.get(key);
        if (cached != null) return cached;

        TextTexture created = rasterizeSwatch(key);
        SWATCH_CACHE.put(key, created);
        trimSwatchCache();
        return created;
    }

    private static TextTexture rasterizeSwatch(SwatchKey key) {
        int supersample = 4;
        int pixelWidth = key.width * key.scale;
        int pixelHeight = key.height * key.scale;
        int highWidth = pixelWidth * supersample;
        int highHeight = pixelHeight * supersample;
        BufferedImage highResolution = new BufferedImage(highWidth, highHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = highResolution.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setColor(new Color(key.color, true));
            graphics.fillRoundRect(0, 0, highWidth, highHeight, 10 * key.scale * supersample, 10 * key.scale * supersample);

            graphics.setColor(new Color(key.color, true).darker());
            graphics.setStroke(new BasicStroke(2f * key.scale * supersample));
            int inset = key.scale * supersample;
            graphics.drawRoundRect(
                    inset,
                    inset,
                    highWidth - inset * 2 - 1,
                    highHeight - inset * 2 - 1,
                    8 * key.scale * supersample,
                    8 * key.scale * supersample
            );
        } finally {
            graphics.dispose();
        }

        BufferedImage buffered = new BufferedImage(pixelWidth, pixelHeight, BufferedImage.TYPE_INT_ARGB);
        graphics = buffered.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(highResolution, 0, 0, pixelWidth, pixelHeight, null);
        } finally {
            graphics.dispose();
        }

        NativeImage image = new NativeImage(pixelWidth, pixelHeight, true);
        for (int y = 0; y < pixelHeight; y++) {
            for (int x = 0; x < pixelWidth; x++) {
                image.setPixel(x, y, buffered.getRGB(x, y));
            }
        }

        String textureName = Integer.toUnsignedString(key.hashCode(), 36)
                + "_" + Long.toUnsignedString(nextTextureId++, 36);
        Identifier id = Identifier.fromNamespaceAndPath("runal", "portable_swatch/" + textureName);
        //? if 1.21.4 {
        /*DynamicTexture texture = new DynamicTexture(image);
        *///?} else {
        Supplier<String> label = () -> "Runal color swatch " + textureName;
        DynamicTexture texture = new DynamicTexture(label, image);
        //?}
        Minecraft.getInstance().getTextureManager().register(id, texture);
        texture.upload();
        return new TextTexture(id, pixelWidth, pixelHeight, key.width, key.height, key.scale);
    }

    private static int physicalScale() {
        var window = Minecraft.getInstance().getWindow();
        if (window == null || window.getGuiScaledWidth() <= 0) return 1;
        return Math.max(1, (int) Math.round(window.getWidth() / (double) window.getGuiScaledWidth()));
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

    private static void trimSwatchCache() {
        Minecraft minecraft = Minecraft.getInstance();
        while (SWATCH_CACHE.size() > 128) {
            var iterator = SWATCH_CACHE.entrySet().iterator();
            if (!iterator.hasNext()) return;
            TextTexture oldest = iterator.next().getValue();
            iterator.remove();
            minecraft.getTextureManager().release(oldest.id);
        }
    }

    private record TextKey(String text, int size, int color, int scale) {
    }

    private record SwatchKey(int width, int height, int color, int scale) {
    }

    private record TextTexture(
            Identifier id,
            int pixelWidth,
            int pixelHeight,
            int width,
            int height,
            float renderScale
    ) {
    }
}
