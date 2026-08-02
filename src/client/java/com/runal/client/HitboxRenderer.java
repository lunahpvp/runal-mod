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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

import com.mojang.blaze3d.vertex.PoseStack;

import java.util.ArrayList;
import java.util.List;

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

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        List<RenderedHitbox> hitboxes = new ArrayList<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity.isRemoved()) continue;
            if (entity == mc.player && mc.options.getCameraType().isFirstPerson()) continue;
            // Server-side display/interaction helpers can have enormous synthetic
            // bounding boxes. Rendering those is what produced the blue plane across
            // the screen. "Entities" here means mobs/players and dropped items.
            if (entity instanceof ArmorStand) continue;
            if (!(entity instanceof LivingEntity) && !(entity instanceof ItemEntity)) continue;

            AABB box = getRenderBoundingBox(entity, tickDelta);
            if (isTooThin(box)) continue;

            int color = entity instanceof Player ? HitboxesState.INSTANCE.playerColor : HitboxesState.INSTANCE.entityColor;
            hitboxes.add(new RenderedHitbox(box, color));
        }

        //? if 1.21.4 || 1.21.11 || 26.1.2 {
        VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.lines());
        for (RenderedHitbox hitbox : hitboxes) {
            //? if 1.21.4 {
            /*ShapeRenderer.renderShape(poseStack, consumer, Shapes.create(hitbox.box()), 0, 0, 0, hitbox.color());
            *///?}
            //? if 1.21.11 || 26.1.2 {
            ShapeRenderer.renderShape(poseStack, consumer, Shapes.create(hitbox.box()), 0, 0, 0, hitbox.color(), HitboxesState.INSTANCE.lineWidth);
            //?}
        }
        bufferSource.endBatch(RenderTypes.lines());
        //?}
        //? if 26.2 {
        /*for (RenderedHitbox hitbox : hitboxes) {
            context.submitNodeCollector().submitShapeOutline(poseStack, Shapes.create(hitbox.box()), RenderTypes.LINES, hitbox.color(), HitboxesState.INSTANCE.lineWidth, false);
        }
        *///?}

        poseStack.popPose();
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

    private record RenderedHitbox(AABB box, int color) {
    }
}
