package com.runal.client;

import com.runal.client.mixin.FishingHookAccessor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;

public final class FishAlertController {
    private static final int PARTICLE_BURST_COUNT = 10;
    private static final int DING_INTERVAL_TICKS = 4;
    private static final float VOLUME_DECAY_PER_DING = 0.15f;

    private static boolean wasBiting = false;
    private static int biteTicks = 0;

    private FishAlertController() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(FishAlertController::tick);
    }

    private static void tick(Minecraft mc) {
        if (!FishAlertState.INSTANCE.isEnabled() || mc.player == null || mc.level == null) {
            wasBiting = false;
            return;
        }

        FishingHook hook = findOwnHook(mc);
        if (hook == null) {
            wasBiting = false;
            return;
        }

        boolean biting = ((FishingHookAccessor) hook).runal$isBiting();
        if (biting && !wasBiting) {
            biteTicks = 0;
            onBiteStart(mc, hook);
        } else if (biting) {
            biteTicks++;
            if (biteTicks % DING_INTERVAL_TICKS == 0) {
                float volume = 1.0f - VOLUME_DECAY_PER_DING * (biteTicks / DING_INTERVAL_TICKS);
                if (volume > 0f) {
                    mc.player.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), volume, 1.6f);
                }
            }
        }
        wasBiting = biting;
    }

    private static FishingHook findOwnHook(Minecraft mc) {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof FishingHook hook && hook.getPlayerOwner() == mc.player) return hook;
        }
        return null;
    }

    private static void onBiteStart(Minecraft mc, FishingHook hook) {
        mc.player.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 1.0f, 1.6f);

        var random = hook.getRandom();
        double x = hook.getX();
        double y = hook.getY();
        double z = hook.getZ();
        for (int i = 0; i < PARTICLE_BURST_COUNT; i++) {
            double dx = (random.nextDouble() - 0.5) * 0.6;
            double dz = (random.nextDouble() - 0.5) * 0.6;
            mc.level.addParticle(ParticleTypes.SPLASH, x + dx, y, z + dz, dx, 0.25, dz);
        }
    }
}
