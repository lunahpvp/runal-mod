package com.runal.client.mixin;

import com.runal.client.RunalBadge;
import com.runal.client.RunalPresenceClient;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {

    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void runal$appendPresenceBadge(
            PlayerInfo playerInfo,
            CallbackInfoReturnable<Component> cir
    ) {
        if (!RunalPresenceClient.isActiveUser(
                playerInfo.getProfile().id(),
                playerInfo.getProfile().name()
        )) return;
        cir.setReturnValue(RunalBadge.append(cir.getReturnValue()));
    }
}
