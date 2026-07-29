package com.runal.client.mixin;

import com.runal.client.RunalBadge;
import com.runal.client.RunalPresenceClient;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityNameTagMixin {

    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
    private void runal$appendPresenceBadge(
            Entity entity,
            CallbackInfoReturnable<Component> cir
    ) {
        if (!(entity instanceof Player player)) return;
        if (!RunalPresenceClient.isActiveUser(
                entity.getUUID(),
                player.getGameProfile().name()
        )) return;
        cir.setReturnValue(RunalBadge.append(cir.getReturnValue()));
    }
}
