package com.runal.client;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BossDefeatController {
    private static final Pattern SLAIN_PATTERN = Pattern.compile("^(.+?)\\s*\\[[^\\]]*]\\s*slain!?$");
    private static final Pattern SLAIN_PATTERN_ALT =
            Pattern.compile("^(.+?)\\s*\\[Lvl\\s+[^\\]]+]\\s*Slain!?$", Pattern.CASE_INSENSITIVE);

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            handleMessage(stripDecoration(message.getString().trim()));
        });
    }

    private static void handleMessage(String text) {
        Matcher matcher = SLAIN_PATTERN.matcher(text);
        boolean matched = matcher.matches();
        if (!matched) {
            matcher = SLAIN_PATTERN_ALT.matcher(text);
            matched = matcher.matches();
        }
        if (!matched) return;

        String bossName = matcher.group(1).trim();
        BossDefeatState.increment(bossName);
        BossDefeatState.lastKilledBossName = bossName;
        BossDefeatState.lastKilledAtMs = System.currentTimeMillis();
        RunalPresenceClient.sendBossKill(bossName);
    }

    private static String stripDecoration(String text) {
        String cleaned = text.replaceAll("[\\p{So}\\p{Co}|]", "");
        return cleaned.replaceAll("\\p{Zs}", " ").trim();
    }
}
