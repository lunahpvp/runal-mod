package com.runal.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.runal.client.TooltipScaleState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if 1.21.4 {
/*import com.mojang.blaze3d.vertex.PoseStack;
*///?} else {
import org.joml.Matrix3x2fStack;
//?}

import java.util.List;

@Mixin(GuiGraphicsExtractor.class)
public abstract class TooltipScaleMixin {
    //? if 1.21.4 {
    /*@Shadow
    public abstract PoseStack pose();
    *///?} else {
    @Shadow
    public abstract Matrix3x2fStack pose();
    //?}

    @Inject(
            method = "tooltip",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;extractTooltipBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIILnet/minecraft/resources/Identifier;)V"
            )
    )
    private void runal$applyTooltipScale(
            Font font,
            List<ClientTooltipComponent> lines,
            int xo,
            int yo,
            ClientTooltipPositioner positioner,
            @Nullable Identifier style,
            CallbackInfo ci,
            @Local(name = "textWidth") int textWidth,
            @Local(name = "tempHeight") int tempHeight
    ) {
        TooltipScaleState state = TooltipScaleState.INSTANCE;
        if (!state.isEnabled()) return;

        float scale = state.scale;
        //? if 1.21.4 {
        /*this.pose().translate(xo - xo * scale, yo - yo * scale, 0f);
        this.pose().scale(scale, scale, 1f);
        *///?} else {
        this.pose().translate(xo - xo * scale, yo - yo * scale);
        this.pose().scale(scale, scale);
        //?}
    }
}
