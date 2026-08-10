package com.runal.client.mixin;

import com.runal.client.ItemRarityState;
import com.runal.client.ItemRarityUtil;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class ItemRarityHotbarMixin {

    @Inject(method = "renderSlot", at = @At("TAIL"), require = 0)
    private void runal$drawRarityBorder(
            GuiGraphicsExtractor guiGraphics,
            int x,
            int y,
            DeltaTracker deltaTracker,
            Player player,
            ItemStack stack,
            int seed,
            CallbackInfo ci
    ) {
        ItemRarityState state = ItemRarityState.INSTANCE;
        if (!state.isEnabled() || !state.showInHotbar) return;

        Integer color = ItemRarityUtil.getRarityColor(stack);
        if (color == null) return;

        int t = Math.max(1, Math.round(state.thickness));
        int x2 = x + 16;
        int y2 = y + 16;
        guiGraphics.fill(x, y, x2, y + t, color);
        guiGraphics.fill(x, y2 - t, x2, y2, color);
        guiGraphics.fill(x, y, x + t, y2, color);
        guiGraphics.fill(x2 - t, y, x2, y2, color);
    }
}
