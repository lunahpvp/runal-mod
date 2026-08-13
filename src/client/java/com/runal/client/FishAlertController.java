package com.runal.client;

import com.runal.client.mixin.FishingHookAccessor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;

import java.util.Map;

public final class FishAlertController {
    private static final int PARTICLE_BURST_COUNT = 20;
    private static final int DING_INTERVAL_TICKS = 4;
    private static final int TITLE_COLOR = 0xFF5555;

    private static final Map<String, SimpleParticleType> PARTICLES = Map.of(
            "Electric Spark", ParticleTypes.END_ROD,
            "Soul Fire", ParticleTypes.SOUL_FIRE_FLAME,
            "Totem", ParticleTypes.TOTEM_OF_UNDYING,
            "Firework", ParticleTypes.FIREWORK
    );

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
                float durationTicks = Math.max(1f, FishAlertState.INSTANCE.durationSeconds * 20f);
                float volume = FishAlertState.INSTANCE.soundVolume * (1.0f - biteTicks / durationTicks);
                if (FishAlertState.INSTANCE.soundEnabled && volume > 0f) {
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
        if (FishAlertState.INSTANCE.soundEnabled) {
            mc.player.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), FishAlertState.INSTANCE.soundVolume, 1.6f);
        }

        if (FishAlertState.INSTANCE.titleEnabled) {
            showTitle(mc);
        }

        if (!FishAlertState.INSTANCE.particlesEnabled) return;

        SimpleParticleType particle = PARTICLES.getOrDefault(FishAlertState.INSTANCE.particleType, ParticleTypes.END_ROD);
        var random = hook.getRandom();
        double x = hook.getX();
        double y = hook.getY();
        double z = hook.getZ();
        for (int i = 0; i < PARTICLE_BURST_COUNT; i++) {
            double dx = (random.nextDouble() - 0.5) * 1.0;
            double dz = (random.nextDouble() - 0.5) * 1.0;
            double dy = 0.15 + random.nextDouble() * 0.35;
            mc.level.addParticle(particle, x + dx, y, z + dz, dx, dy, dz);
        }
    }

    private static void showTitle(Minecraft mc) {
        String text = FishAlertState.INSTANCE.titleText.trim();
        if (text.isEmpty()) return;

        var title = Component.literal(text)
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(TITLE_COLOR)).withBold(true));

        //? if 26.2 {
        /*mc.gui.hud.setTimes(0, 20, 5);
        mc.gui.hud.setTitle(title);
        *///?} else {
        mc.gui.setTimes(0, 20, 5);
        mc.gui.setTitle(title);
        //?}
    }
}
