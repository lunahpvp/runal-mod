package com.runal.client;

//? if 1.21.4 || 1.21.11 {
/*import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
*///?} else {
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
//?}
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
//? if 1.21.4 || 1.21.11 {
//?} else {
import net.minecraft.resources.Identifier;
//?}
import net.minecraft.util.Mth;

public class HealthBarRenderer {

    private static float displayedPercent = 1.0f;
    private static float lastHealth = -1.0f;
    private static long lastDamageMs = 0L;

    public static void register() {
        //? if 1.21.4 || 1.21.11 {
        /*HudRenderCallback.EVENT.register(HealthBarRenderer::render);
        *///?} else {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("runal", "health_bar"), HealthBarRenderer::render);
        //?}
    }

    private static void render(net.minecraft.client.gui.GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        HealthBarState state = HealthBarState.INSTANCE;
        if (!state.isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        if ("Third Person".equals(state.renderMode) && mc.options.getCameraType() == CameraType.FIRST_PERSON) return;

        int[] pos = projectPlayer(graphics, deltaTracker, mc, player, state.yOffset);
        if (pos == null) return;

        float health = player.getHealth();
        if (lastHealth < 0f) lastHealth = health;
        if (state.damageFlash && health < lastHealth) lastDamageMs = System.currentTimeMillis();
        lastHealth = health;

        float healthPercent = Mth.clamp(health / player.getMaxHealth(), 0f, 1f);
        displayedPercent = state.smoothInterpolation ? Mth.lerp(0.18f, displayedPercent, healthPercent) : healthPercent;

        int barColor = getHealthColor(displayedPercent, state);
        long flashAge = System.currentTimeMillis() - lastDamageMs;
        if (state.damageFlash && flashAge < 260L) {
            float flash = 1f - flashAge / 260f;
            barColor = mixColor(barColor, state.damageFlashColor, flash);
        }

        if ("Text".equals(state.renderStyle)) {
            drawText(graphics, mc, pos[0], pos[1], player, state);
        } else if ("Compact".equals(state.renderStyle)) {
            drawCompact(graphics, mc, pos[0], pos[1], player, state, barColor);
        } else {
            drawBar(graphics, mc, pos[0], pos[1], player, state, barColor);
        }
    }

    private static int[] projectPlayer(net.minecraft.client.gui.GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker deltaTracker, Minecraft mc, LocalPlayer player, int yOffset) {
        Camera camera = mc.gameRenderer.getMainCamera();
        float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(false);
        double worldX = Mth.lerp(tickDelta, player.xOld, player.getX());
        double worldY = Mth.lerp(tickDelta, player.yOld, player.getY()) - 0.3 + yOffset / 20.0;
        double worldZ = Mth.lerp(tickDelta, player.zOld, player.getZ());
        var camPos = camera.position();
        double relX = worldX - camPos.x;
        double relY = worldY - camPos.y;
        double relZ = worldZ - camPos.z;
        double yaw = Math.toRadians(camera.yRot());
        double pitch = Math.toRadians(camera.xRot());
        double cosYaw = Math.cos(-yaw), sinYaw = Math.sin(-yaw);
        double x1 = relX * cosYaw - relZ * sinYaw;
        double z1 = relX * sinYaw + relZ * cosYaw;
        double cosPitch = Math.cos(-pitch), sinPitch = Math.sin(-pitch);
        double y2 = relY * cosPitch - z1 * sinPitch;
        double z2 = relY * sinPitch + z1 * cosPitch;
        if (z2 <= 0.1) return null;
        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();
        double fov = mc.options.fov().get();
        double aspect = (double) screenW / screenH;
        double scale = 1.0 / Math.tan(Math.toRadians(fov / 2.0));
        int finalX = (int) (((x1 / z2) * scale / aspect + 1.0) * 0.5 * screenW);
        int finalY = (int) ((1.0 - (y2 / z2) * scale) * 0.5 * screenH);
        if (finalX < -200 || finalX > screenW + 200 || finalY < -200 || finalY > screenH + 200) return null;
        return new int[]{finalX, finalY};
    }

    private static void drawBar(net.minecraft.client.gui.GuiGraphicsExtractor graphics, Minecraft mc, int x, int y, LocalPlayer player, HealthBarState state, int barColor) {
        int left = x - state.width / 2;
        int top = y - state.height / 2;
        int fillWidth = Math.round(state.width * displayedPercent);
        graphics.fill(left - 1, top - 1, left + state.width + 1, top + state.height + 1, state.borderColor);
        graphics.fill(left, top, left + state.width, top + state.height, state.backgroundColor);
        if (fillWidth > 0) graphics.fill(left, top, left + fillWidth, top + state.height, barColor);
        drawTextAtConfiguredPosition(graphics, mc, x, y, left, top, player, state);
    }

    private static void drawCompact(net.minecraft.client.gui.GuiGraphicsExtractor graphics, Minecraft mc, int x, int y, LocalPlayer player, HealthBarState state, int barColor) {
        int size = Math.max(10, state.height + 6);
        int left = x - state.width / 2;
        int top = y - size / 2;
        int fillWidth = Math.round(state.width * displayedPercent);
        graphics.fill(left - 1, top - 1, left + state.width + 1, top + size + 1, state.borderColor);
        graphics.fill(left, top, left + state.width, top + size, state.backgroundColor);
        if (fillWidth > 0) graphics.fill(left, top, left + fillWidth, top + size, barColor);
        drawText(graphics, mc, x, y - mc.font.lineHeight / 2, player, state);
    }

    private static void drawTextAtConfiguredPosition(net.minecraft.client.gui.GuiGraphicsExtractor graphics, Minecraft mc, int x, int y, int left, int top, LocalPlayer player, HealthBarState state) {
        int textWidth = mc.font.width(formatHealth(player.getHealth(), player.getMaxHealth(), state));
        int textX = switch (state.textPosition) {
            case "Left" -> left;
            case "Right" -> left + state.width - (int) (textWidth * state.textScale);
            case "Above" -> x - (int) (textWidth * state.textScale) / 2;
            case "Below" -> x - (int) (textWidth * state.textScale) / 2;
            default -> x - (int) (textWidth * state.textScale) / 2;
        };
        int textY = switch (state.textPosition) {
            case "Above" -> top - 10;
            case "Below" -> top + state.height + 2;
            default -> y - mc.font.lineHeight / 2;
        };
        drawText(graphics, mc, textX, textY, player, state);
    }

    private static void drawText(net.minecraft.client.gui.GuiGraphicsExtractor graphics, Minecraft mc, int x, int y, LocalPlayer player, HealthBarState state) {
        String text = formatHealth(player.getHealth(), player.getMaxHealth(), state);
        //? if 1.21.4 {
        /*graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0f);
        graphics.pose().scale(state.textScale, state.textScale, 1f);
        *///?} else {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(state.textScale, state.textScale);
        //?}
        graphics.text(mc.font, text, 0, 0, state.textColor, "Shadow".equals(state.textStyle));
        //? if 1.21.4 {
        /*graphics.pose().popPose();
        *///?} else {
        graphics.pose().popMatrix();
        //?}
    }

    private static String formatHealth(float health, float maxHealth, HealthBarState state) {
        // Shown as hearts (out of 10) to match vanilla's heart display, not raw HP (out of 20).
        int hp = Mth.ceil(health / 2f);
        int max = Mth.ceil(maxHealth / 2f);
        if ("Percent".equals(state.healthFormat)) return Math.round((health / maxHealth) * 100f) + "%";
        if (state.showMaxHealth) return hp + "/" + max;
        return String.valueOf(hp);
    }

    private static int getHealthColor(float percent, HealthBarState state) {
        if (percent <= state.lowHpThreshold) return state.lowHpColor;
        if (percent <= state.midHpThreshold) return state.midHpColor;
        return state.highHpColor;
    }

    private static int mixColor(int c1, int c2, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int a1 = (c1 >>> 24) & 255, r1 = (c1 >>> 16) & 255, g1 = (c1 >>> 8) & 255, b1 = c1 & 255;
        int a2 = (c2 >>> 24) & 255, r2 = (c2 >>> 16) & 255, g2 = (c2 >>> 8) & 255, b2 = c2 & 255;
        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
