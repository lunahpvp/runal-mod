package com.runal.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
//? if 1.21.4 {
/*import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.renderer.RenderTypes;
*///?}
//? if 1.21.11 {
/*import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.renderer.rendertype.RenderTypes;
*///?}
//? if 26.1.2 || 26.2 {
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.renderer.rendertype.RenderTypes;
//?}
import net.minecraft.client.Minecraft;
//? if 1.21.4 || 1.21.11 || 26.1.2 {
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
//?}
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

import com.mojang.blaze3d.vertex.PoseStack;

public class HitboxRenderer {

    public static void register() {
        //? if 1.21.4 {
        /*WorldRenderEvents.AFTER_TRANSLUCENT.register(HitboxRenderer::render);
        *///?}
        //? if 1.21.11 {
        /*WorldRenderEvents.END_MAIN.register(HitboxRenderer::render);
        *///?}
        //? if 26.1.2 || 26.2 {
        LevelRenderEvents.END_MAIN.register(HitboxRenderer::render);
        //?}
    }

    //? if 1.21.4 || 1.21.11 {
    /*private static void render(WorldRenderContext context) {
    *///?}
    //? if 26.1.2 || 26.2 {
    private static void render(LevelRenderContext context) {
    //?}
        if (!HitboxesState.INSTANCE.isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Vec3 cameraPos = context.gameRenderer().getMainCamera().position();
        //? if 1.21.4 {
        /*PoseStack poseStack = context.matrixStack();
        MultiBufferSource.BufferSource bufferSource = (MultiBufferSource.BufferSource) context.consumers();
        VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.lines());
        *///?}
        //? if 1.21.11 {
        /*PoseStack poseStack = context.matrices();
        MultiBufferSource.BufferSource bufferSource = (MultiBufferSource.BufferSource) context.consumers();
        VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.lines());
        *///?}
        //? if 26.1.2 {
        PoseStack poseStack = context.poseStack();
        MultiBufferSource.BufferSource bufferSource = context.bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.lines());
        //?}
        //? if 26.2 {
        /*PoseStack poseStack = context.poseStack();
        *///?}

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity.isRemoved()) continue;
            if (entity == mc.player && mc.options.getCameraType().isFirstPerson()) continue;

            AABB box = getRenderBoundingBox(entity, tickDelta);
            if (isTooThin(box)) continue;

            int color = entity instanceof Player ? HitboxesState.INSTANCE.playerColor : HitboxesState.INSTANCE.entityColor;
            //? if 1.21.4 {
            /*ShapeRenderer.renderShape(poseStack, consumer, Shapes.create(box), 0, 0, 0, color);
            *///?}
            //? if 1.21.11 || 26.1.2 {
            ShapeRenderer.renderShape(poseStack, consumer, Shapes.create(box), 0, 0, 0, color, HitboxesState.INSTANCE.lineWidth);
            //?}
            //? if 26.2 {
            /*context.submitNodeCollector().submitShapeOutline(poseStack, Shapes.create(box), RenderTypes.LINES, color, HitboxesState.INSTANCE.lineWidth, false);
            *///?}
        }

        poseStack.popPose();
        //? if 1.21.4 {
        /*bufferSource.endBatch(RenderTypes.lines());
        *///?}
        //? if 1.21.11 || 26.1.2 {
        bufferSource.endBatch(RenderTypes.lines());
        //?}
    }

    private static AABB getRenderBoundingBox(Entity entity, float tickDelta) {
        double renderX = entity.xo + (entity.getX() - entity.xo) * tickDelta;
        double renderY = entity.yo + (entity.getY() - entity.yo) * tickDelta;
        double renderZ = entity.zo + (entity.getZ() - entity.zo) * tickDelta;
        return entity.getBoundingBox().move(
                renderX - entity.getX(),
                renderY - entity.getY(),
                renderZ - entity.getZ()
        );
    }

    private static boolean isTooThin(AABB box) {
        double minSize = 0.01;
        return box.getXsize() < minSize || box.getYsize() < minSize || box.getZsize() < minSize;
    }
}
