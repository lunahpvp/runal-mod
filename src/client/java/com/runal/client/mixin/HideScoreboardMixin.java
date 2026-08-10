package com.runal.client.mixin;

import com.runal.client.HideScoreboardState;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class HideScoreboardMixin {

    //? if 1.21.4 || 1.21.11 {
    /*@Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true, require = 0)
    private void runal$hideScoreboard(GuiGraphicsExtractor context, CallbackInfo ci) {
    *///?} else {
    @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true, require = 0)
    private void runal$hideScoreboard(CallbackInfo ci) {
    //?}
        if (HideScoreboardState.INSTANCE.isEnabled()) {
            ci.cancel();
        }
    }
}
