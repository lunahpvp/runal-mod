package com.runal.client;

import com.mojang.blaze3d.vertex.PoseStack;
//? if 1.21.4 {
/*import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.renderer.RenderTypes;
*///?}
//? if 1.21.11 {
/*import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.renderer.rendertype.RenderTypes;
*///?}
//? if 26.1.2 || 26.2 {
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
//?}
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
//? if 1.21.4 || 1.21.11 || 26.1.2 {
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
//?}
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

public class WaypointRenderer {

    private static final double BEAM_HEIGHT = 320.0;
    private static final double BEAM_HALF_WIDTH = 0.6;
    private static final float BEAM_LINE_WIDTH = 12.0f;
    private static final double BEAM_HIDE_DISTANCE = 20.0;
    private static final double CLOSE_RANGE = 3.0;
    private static final double AIM_Y_MIN = -100.0;
    private static final double AIM_Y_MAX = 500.0;
    private static final double AIM_HALF_WIDTH = 2.0;
    private static final double AIM_RAY_LENGTH = 4096.0;

    public static void register() {
        //? if 1.21.4 {
        /*WorldRenderEvents.AFTER_TRANSLUCENT.register(WaypointRenderer::renderBeams);
        HudRenderCallback.EVENT.register(WaypointRenderer::renderLabels);
        *///?}
        //? if 1.21.11 {
        /*WorldRenderEvents.END_MAIN.register(WaypointRenderer::renderBeams);
        HudRenderCallback.EVENT.register(WaypointRenderer::renderLabels);
        *///?}
        //? if 26.1.2 || 26.2 {
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(WaypointRenderer::renderBeams);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("runal", "waypoint_labels"), WaypointRenderer::renderLabels);
        //?}
    }

    //? if 1.21.4 || 1.21.11 {
    /*private static void renderBeams(WorldRenderContext context) {
    *///?}
    //? if 26.1.2 || 26.2 {
    private static void renderBeams(LevelRenderContext context) {
    //?}
        if (!WaypointManagerState.INSTANCE.isEnabled()) return;
        if (!WaypointManagerState.INSTANCE.showBeams) return;
        if (WaypointManagerState.INSTANCE.getWaypoints().isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        String dimensionKey = WaypointManagerState.currentDimensionKey(mc);
        Vec3 cameraPos = context.gameRenderer().getMainCamera().position();
        //? if 1.21.4 {
        /*PoseStack poseStack = context.matrixStack();
        MultiBufferSource.BufferSource bufferSource = (MultiBufferSource.BufferSource) context.consumers();
        *///?}
        //? if 1.21.11 {
        /*PoseStack poseStack = context.matrices();
        MultiBufferSource.BufferSource bufferSource = (MultiBufferSource.BufferSource) context.consumers();
        *///?}
        //? if 26.1.2 {
        PoseStack poseStack = context.poseStack();
        MultiBufferSource.BufferSource bufferSource = context.bufferSource();
        //?}
        //? if 26.2 {
        /*PoseStack poseStack = context.poseStack();
        *///?}
        boolean renderedLines = false;

        for (Waypoint waypoint : WaypointManagerState.INSTANCE.getWaypoints()) {
            if (!waypoint.enabled || !waypoint.isEnabledInDimension(dimensionKey)) continue;

            double baseX = waypoint.x + 0.5;
            double baseY = waypoint.y;
            double baseZ = waypoint.z + 0.5;

            double toBaseX = baseX - mc.player.getX();
            double toBaseZ = baseZ - mc.player.getZ();
            double dy = baseY - mc.player.getY();
            double distance = Math.sqrt(toBaseX * toBaseX + dy * dy + toBaseZ * toBaseZ);
            if (distance <= BEAM_HIDE_DISTANCE) continue;

            poseStack.pushPose();
            poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            AABB beam = new AABB(baseX - BEAM_HALF_WIDTH, baseY, baseZ - BEAM_HALF_WIDTH, baseX + BEAM_HALF_WIDTH, baseY + BEAM_HEIGHT, baseZ + BEAM_HALF_WIDTH);
            //? if 1.21.4 {
            /*ShapeRenderer.renderShape(poseStack, bufferSource.getBuffer(RenderTypes.lines()), Shapes.create(beam), 0, 0, 0, waypoint.color());
            *///?}
            //? if 1.21.11 || 26.1.2 {
            ShapeRenderer.renderShape(poseStack, bufferSource.getBuffer(RenderTypes.lines()), Shapes.create(beam), 0, 0, 0, waypoint.color(), BEAM_LINE_WIDTH);
            //?}
            //? if 26.2 {
            /*context.submitNodeCollector().submitShapeOutline(poseStack, Shapes.create(beam), RenderTypes.LINES, waypoint.color(), BEAM_LINE_WIDTH, false);
            *///?}
            poseStack.popPose();
            renderedLines = true;
        }

        //? if 1.21.4 {
        /*if (renderedLines) bufferSource.endBatch(RenderTypes.lines());
        *///?}
        //? if 1.21.11 || 26.1.2 {
        if (renderedLines) bufferSource.endBatch(RenderTypes.lines());
        //?}
    }

    private static void renderLabels(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!WaypointManagerState.INSTANCE.isEnabled()) return;
        if (WaypointManagerState.INSTANCE.getWaypoints().isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        String dimensionKey = WaypointManagerState.currentDimensionKey(mc);
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();
        int screenCenterX = graphics.guiWidth() / 2;
        int screenCenterY = graphics.guiHeight() / 2;

        for (Waypoint waypoint : WaypointManagerState.INSTANCE.getWaypoints()) {
            if (!waypoint.enabled || !waypoint.isEnabledInDimension(dimensionKey)) continue;

            double baseX = waypoint.x + 0.5;
            double baseY = waypoint.y;
            double baseZ = waypoint.z + 0.5;

            double toBaseX = baseX - mc.player.getX();
            double toBaseZ = baseZ - mc.player.getZ();
            double dy = baseY - mc.player.getY();
            double distance = Math.sqrt(toBaseX * toBaseX + dy * dy + toBaseZ * toBaseZ);
            double toBaseHorizLen = Math.sqrt(toBaseX * toBaseX + toBaseZ * toBaseZ);

            boolean aimed;
            if (toBaseHorizLen <= CLOSE_RANGE) {
                aimed = true;
            } else {
                AABB aimBox = new AABB(baseX - AIM_HALF_WIDTH, AIM_Y_MIN, baseZ - AIM_HALF_WIDTH,
                        baseX + AIM_HALF_WIDTH, AIM_Y_MAX, baseZ + AIM_HALF_WIDTH);
                Vec3 viewVec = mc.player.getViewVector(1.0f);
                Vec3 rayEnd = cameraPos.add(viewVec.scale(AIM_RAY_LENGTH));
                aimed = aimBox.clip(cameraPos, rayEnd).isPresent();
            }
            if (!aimed) continue;

            String label = waypoint.name + " (" + Math.round(distance) + "m)";
            int textWidth = mc.font.width(label);
            int x = screenCenterX - textWidth / 2;
            int y = screenCenterY - 22;
            graphics.fill(x - 3, y - 2, x + textWidth + 3, y + 10, 0x88000000);
            graphics.text(mc.font, label, x, y, 0xFFFFFFFF, true);
        }
    }
}
