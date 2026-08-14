package com.runal.client.mixin;

import com.runal.client.BossBarScaleState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossHealthOverlay.class)
public abstract class BossHealthOverlayScaleMixin {
    @Unique
    private boolean runal$scaled;

    //? if 1.21.4 || 1.21.11 {
    /*@Inject(method = "render", at = @At("HEAD"))
    private void runal$beginScale(GuiGraphicsExtractor graphics, CallbackInfo ci) {
    *///?} else {
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void runal$beginScale(GuiGraphicsExtractor graphics, CallbackInfo ci) {
    //?}
        BossBarScaleState state = BossBarScaleState.INSTANCE;
        float scale = state.scale;
        float offsetX = state.offsetX;
        float offsetY = state.offsetY;

        boolean hasScale = Math.abs(scale - 1.0f) > 0.001f;
        boolean hasOffset = Math.abs(offsetX) > 0.001f || Math.abs(offsetY) > 0.001f;
        runal$scaled = state.isEnabled() && (hasScale || hasOffset);
        if (!runal$scaled) return;

        float centerX = graphics.guiWidth() / 2.0f;
        //? if 1.21.4 {
        /*graphics.pose().pushPose();
        graphics.pose().translate(offsetX, offsetY, 0.0f);
        graphics.pose().translate(centerX, 0.0f, 0.0f);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.pose().translate(-centerX, 0.0f, 0.0f);
        *///?} else {
        graphics.pose().pushMatrix();
        graphics.pose().translate(offsetX, offsetY);
        graphics.pose().translate(centerX, 0.0f);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-centerX, 0.0f);
        //?}
    }

    //? if 1.21.4 || 1.21.11 {
    /*@Inject(method = "render", at = @At("RETURN"))
    private void runal$endScale(GuiGraphicsExtractor graphics, CallbackInfo ci) {
    *///?} else {
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void runal$endScale(GuiGraphicsExtractor graphics, CallbackInfo ci) {
    //?}
        if (!runal$scaled) return;
        //? if 1.21.4 {
        /*graphics.pose().popPose();
        *///?} else {
        graphics.pose().popMatrix();
        //?}
        runal$scaled = false;
    }
}
