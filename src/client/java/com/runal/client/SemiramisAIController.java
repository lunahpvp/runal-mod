package com.runal.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public class SemiramisAIController {
    private static final String SEMIRAMIS_PREFIX = "Semiramis AI";
    private static final String HEAL_TRIGGER_PHRASE = "having to save you";
    private static final String SEMIRAMIS_ITEM_NAME = "Semiramis AI";
    private static final double SEMIRAMIS_COOLDOWN_SECONDS = 100;
    private static final int TITLE_DISPLAY_TICKS = 2 * 20;

    public static void register() {
        ClientReceiveMessageEvents.ALLOW_GAME.register(SemiramisAIController::handleMessage);
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (SemiramisAIState.cooldownEnabled) sendPhoeCommand();
        });
    }

    private static boolean handleMessage(Component message, boolean overlay) {
        if (overlay || !SemiramisAIState.cooldownEnabled) return true;

        String text = message.getString();
        if (!text.contains(SEMIRAMIS_PREFIX)) return true;

        if (text.toLowerCase().contains(HEAL_TRIGGER_PHRASE)) {
            AccessoryCooldownState.start(SEMIRAMIS_ITEM_NAME, SEMIRAMIS_COOLDOWN_SECONDS);
            triggerHealTitle();
        }

        return SemiramisAIState.showMessages;
    }

    private static void triggerHealTitle() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 1.0f, 1.6f);
        }
        SemiramisTitleState.currentText = "Healed!";
        SemiramisTitleState.displayTicksRemaining = TITLE_DISPLAY_TICKS;
    }

    private static void tick() {
        if (SemiramisTitleState.displayTicksRemaining > 0) {
            SemiramisTitleState.displayTicksRemaining--;
            if (SemiramisTitleState.displayTicksRemaining == 0) {
                SemiramisTitleState.currentText = null;
            }
        }
    }

    public static void sendPhoeCommand() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;
        mc.getConnection().sendCommand("runalphoe");
    }
}
