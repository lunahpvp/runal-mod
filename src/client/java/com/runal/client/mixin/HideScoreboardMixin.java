package com.runal.client.mixin;

import com.runal.client.HideScoreboardState;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Cancelling the render call didn't work (confirmed with VulkanMod both on and off), which
// means whatever's actually drawing the sidebar isn't reachable through that render method at
// all on this version. Going at the data layer instead: if there's no sidebar objective to
// begin with, nothing has anything to draw, regardless of which renderer asks.
@Mixin(Scoreboard.class)
public abstract class HideScoreboardMixin {

    @Inject(method = "getDisplayObjective", at = @At("RETURN"), cancellable = true, require = 0)
    private void runal$hideScoreboard(DisplaySlot slot, CallbackInfoReturnable<Objective> cir) {
        if (HideScoreboardState.INSTANCE.isEnabled() && slot == DisplaySlot.SIDEBAR) {
            cir.setReturnValue(null);
        }
    }
}
