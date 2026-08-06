package com.runal.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.runal.client.ViewModelState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
//? if 1.21.4 {
//?} else {
import net.minecraft.client.renderer.SubmitNodeCollector;
//?}
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemInHandRenderer.class)
public abstract class ViewModelMixin {
    @Shadow
    private float mainHandHeight;

    @Shadow
    private float offHandHeight;

    @Shadow
    private float oMainHandHeight;

    @Shadow
    private float oOffHandHeight;

    // Held-item position/rotation/scale offsets need the render-command-collector parameter
    // that only exists on the newer submit-node rendering API (1.21.11/26.1.2/26.2). On 1.21.4
    // those offsets are skipped; equip-animation and swing-speed still work everywhere below.
    //? if 1.21.4 {
    /*
    *///?} else {
    @Inject(
            method = "renderArmWithItem",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER)
    )
    private void runal$applyPositionOffset(
            AbstractClientPlayer player,
            float tickProgress,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack item,
            float equipProgress,
            PoseStack matrices,
            SubmitNodeCollector orderedRenderCommandQueue,
            int light,
            CallbackInfo ci
    ) {
        ViewModelState state = ViewModelState.INSTANCE;
        if (!state.isEnabled()) return;
        if (!state.applyToHand && item.isEmpty()) return;

        double offsetX = state.offsetX;
        double offsetY = state.offsetY;
        double offsetZ = state.offsetZ;

        if (hand == InteractionHand.MAIN_HAND) {
            matrices.translate(offsetX, offsetY, offsetZ);
        } else {
            matrices.translate(-offsetX, offsetY, offsetZ);
        }
    }

    @Inject(
            method = "renderArmWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V")
    )
    private void runal$applyRotationAndScale(
            AbstractClientPlayer player,
            float tickProgress,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack item,
            float equipProgress,
            PoseStack matrices,
            SubmitNodeCollector orderedRenderCommandQueue,
            int light,
            CallbackInfo ci
    ) {
        ViewModelState state = ViewModelState.INSTANCE;
        if (!state.isEnabled()) return;

        float rotY = state.rotY;
        float rotZ = state.rotZ;
        if (hand == InteractionHand.OFF_HAND) {
            rotY = -rotY;
            rotZ = -rotZ;
        }

        matrices.mulPose(Axis.XP.rotationDegrees(state.rotX));
        matrices.mulPose(Axis.YP.rotationDegrees(rotY));
        matrices.mulPose(Axis.ZP.rotationDegrees(rotZ));

        matrices.scale(state.scaleX, state.scaleY, state.scaleZ);
    }

    @Inject(method = "renderPlayerArm", at = @At("HEAD"))
    private void runal$applyHandPositionOffset(
            PoseStack matrices,
            SubmitNodeCollector queue,
            int light,
            float equipProgress,
            float swingProgress,
            HumanoidArm arm,
            CallbackInfo ci
    ) {
        ViewModelState state = ViewModelState.INSTANCE;
        if (!state.isEnabled() || !state.applyToHand) return;

        double offsetX = state.offsetX;
        double offsetY = state.offsetY;
        double offsetZ = state.offsetZ;

        if (arm == HumanoidArm.RIGHT) {
            matrices.translate(offsetX, offsetY, offsetZ);
        } else {
            matrices.translate(-offsetX, offsetY, offsetZ);
        }
    }

    @Inject(
            method = "renderPlayerArm",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;getPlayerRenderer(Lnet/minecraft/client/player/AbstractClientPlayer;)Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;")
    )
    private void runal$applyHandRotationAndScale(
            PoseStack matrices,
            SubmitNodeCollector queue,
            int light,
            float equipProgress,
            float swingProgress,
            HumanoidArm arm,
            CallbackInfo ci
    ) {
        ViewModelState state = ViewModelState.INSTANCE;
        if (!state.isEnabled() || !state.applyToHand) return;

        float rotY = state.rotY;
        float rotZ = state.rotZ;
        if (arm == HumanoidArm.LEFT) {
            rotY = -rotY;
            rotZ = -rotZ;
        }

        matrices.mulPose(Axis.XP.rotationDegrees(state.rotX));
        matrices.mulPose(Axis.YP.rotationDegrees(rotY));
        matrices.mulPose(Axis.ZP.rotationDegrees(rotZ));

        matrices.scale(state.scaleX, state.scaleY, state.scaleZ);
    }
    //?}

    @Redirect(
            method = "swingArm",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V", ordinal = 0)
    )
    private void runal$modifySwingTranslation(PoseStack instance, float x, float y, float z) {
        ViewModelState state = ViewModelState.INSTANCE;
        if (state.isEnabled()) {
            instance.translate(x * state.swingX, y * state.swingY, z * state.swingZ);
        } else {
            instance.translate(x, y, z);
        }
    }

    @Inject(method = "shouldInstantlyReplaceVisibleItem", at = @At("HEAD"), cancellable = true)
    private void runal$skipEquipAnimation(ItemStack from, ItemStack to, CallbackInfoReturnable<Boolean> cir) {
        ViewModelState state = ViewModelState.INSTANCE;
        if (state.isEnabled() && state.noEquipAnimation) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void runal$forceItemHeight(CallbackInfo ci) {
        ViewModelState state = ViewModelState.INSTANCE;
        if (state.isEnabled() && state.noEquipAnimation) {
            this.mainHandHeight = 1.0f;
            this.offHandHeight = 1.0f;
            this.oMainHandHeight = 1.0f;
            this.oOffHandHeight = 1.0f;
        }
    }
}
