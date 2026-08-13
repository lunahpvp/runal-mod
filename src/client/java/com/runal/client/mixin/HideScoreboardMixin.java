package com.runal.client.mixin;

import com.runal.client.HideScoreboardState;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Cancelling the render call didn't work, so this hides the sidebar at the data layer instead.
@Mixin(Scoreboard.class)
public abstract class HideScoreboardMixin {

    @Inject(method = "getDisplayObjective", at = @At("RETURN"), cancellable = true, require = 0)
    private void runal$hideScoreboard(DisplaySlot slot, CallbackInfoReturnable<Objective> cir) {
        if (HideScoreboardState.INSTANCE.isEnabled() && slot == DisplaySlot.SIDEBAR) {
            cir.setReturnValue(null);
        }
    }
}
