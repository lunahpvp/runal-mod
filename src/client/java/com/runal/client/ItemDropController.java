package com.runal.client;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.Map;
import java.util.Optional;

public class ItemDropController {
    private static final long BOSS_ATTRIBUTION_WINDOW_MS = 5000L;

    // Not vanilla legacy color codes (those show up as literal "§x" text, which "You found"
    // messages don't have at all) - this server colors drop rarity with genuine RGB text
    // color, given by the server dev as &x&r&r&g&g&b&b hex: rare=#2368F7, legendary=#FDD017,
    // mythical=#FF00EE, epic=#9400D3.
    private static final Map<Integer, String> RARITY_COLORS = Map.of(
            0x2368F7, "rare",
            0xFDD017, "legendary",
            0xFF00EE, "mythical",
            0x9400D3, "epic"
    );

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            handleMessage(message);
        });
    }

    private static void handleMessage(Component message) {
        if (!message.getString().trim().startsWith("You found ")) return;

        String rarity = extractRarity(message);
        if (rarity == null) return;

        String boss = null;
        if (BossDefeatState.lastKilledBossName != null
                && System.currentTimeMillis() - BossDefeatState.lastKilledAtMs <= BOSS_ATTRIBUTION_WINDOW_MS) {
            boss = BossDefeatState.lastKilledBossName;
        }

        RunalPresenceClient.sendItemDrop(rarity, boss);
    }

    private static String extractRarity(Component message) {
        return message.visit((style, text) -> {
            TextColor color = style.getColor();
            if (color == null) return Optional.<String>empty();
            String rarity = RARITY_COLORS.get(color.getValue());
            return rarity != null ? Optional.of(rarity) : Optional.<String>empty();
        }, Style.EMPTY).orElse(null);
    }
}
