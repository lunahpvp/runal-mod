package com.runal.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.regex.Pattern;

public class SemiramisAIController {
    private static final String SEMIRAMIS_PREFIX = "Semiramis AI";
    private static final String HEAL_TRIGGER_PHRASE = "having to save you";
    private static final String SEMIRAMIS_ITEM_NAME = "Semiramis AI";
    private static final double SEMIRAMIS_COOLDOWN_SECONDS = 100;
    private static final int TITLE_DISPLAY_TICKS = 26;

    // This server embeds legacy formatting codes as literal characters in message text
    // rather than as Style metadata, which breaks plain substring matching if left in.
    private static final Pattern LEGACY_FORMATTING_CODE = Pattern.compile("(?i)§[0-9A-FK-OR]");

    public static void register() {
        ClientReceiveMessageEvents.ALLOW_GAME.register(SemiramisAIController::handleMessage);
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }

    private static boolean handleMessage(Component message, boolean overlay) {
        if (overlay || !SemiramisAIState.cooldownEnabled) return true;

        String text = LEGACY_FORMATTING_CODE.matcher(message.getString()).replaceAll("");
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
