package com.runal.client.mixin;

import com.runal.client.ItemRarityDebug;
import com.runal.client.ItemRarityState;
import com.runal.client.ItemRarityUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class ItemRaritySlotMixin {

    @Inject(method = "renderSlot", at = @At("TAIL"), require = 0)
    private void runal$drawRarityBorder(GuiGraphicsExtractor guiGraphics, Slot slot, CallbackInfo ci) {
        ItemRarityState state = ItemRarityState.INSTANCE;
        if (!state.isEnabled() || !state.showInInventory) return;

        ItemRarityDebug.log("inventory", slot.getItem());
        Integer color = ItemRarityUtil.getRarityColor(slot.getItem());
        if (color == null) return;

        runal$drawBorder(guiGraphics, slot.x, slot.y, color, state.thickness);
    }

    private static void runal$drawBorder(GuiGraphicsExtractor guiGraphics, int x, int y, int color, float thickness) {
        int t = Math.max(1, Math.round(thickness));
        int x2 = x + 16;
        int y2 = y + 16;
        guiGraphics.fill(x, y, x2, y + t, color);
        guiGraphics.fill(x, y2 - t, x2, y2, color);
        guiGraphics.fill(x, y, x + t, y2, color);
        guiGraphics.fill(x2 - t, y, x2, y2, color);
    }
}
