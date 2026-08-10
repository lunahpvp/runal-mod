package com.runal.client;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BossDefeatController {
    private static final Pattern SLAIN_PATTERN = Pattern.compile("^(.+?)\\s*\\[[^\\]]*]\\s*slain!?$");
    // Second, independent kill-message format - server randomizes between this and the one
    // above, e.g. "Fallen Minerian [Lvl 100] Slain!" (capitalized, any boss name/level).
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

        BossDefeatState.increment(matcher.group(1).trim());
    }

    private static String stripDecoration(String text) {
        String cleaned = text.replaceAll("[\\p{So}\\p{Co}|]", "");
        return cleaned.replaceAll("\\p{Zs}", " ").trim();
    }
}
