package com.runal.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BossTitleController {
    private static final String[] BOSS_NAMES = {
            "Shaman Kerax",
            "Beast of Winds",
            "Shadow Assassin Nyx",
            "Great Paladin",
            "The Pharaoh",
            "The Shadow",
            "The Nightmarrow",
            "Ashen Beast",
            "Maelstrom",
            "Spider Queen",
            "Flame Captain Aron",
            "Plebelin",
            "Mushroom Amalgamation",
            "Natuir",
            "Angel",
            "Flame Lord",
            "Harbinger of Storms",
            "Ruinwing",
            "Aeralith",
            "Elemental Sheep",
    };

    private static final int MAX_WORDS = 5;

    private static final int DISPLAY_TICKS = 4 * 20;

    private static final Pattern SCEPTER_BOSS_LINE_PATTERN = buildScepterPattern();
    private static final Pattern MAGE_RPG_BOSS_LINE_PATTERN = Pattern.compile(
            "^(?:\\[[^\\]]*]\\s*)*\\[BOSS]\\s+(.+?)\\s+"
                    + "\\[Lvl\\s+[^\\]]+]\\s*(?:»|:|>)\\s*(.+)$",
            Pattern.CASE_INSENSITIVE
    );

    private static Pattern buildScepterPattern() {
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < BOSS_NAMES.length; i++) {
            if (i > 0) names.append('|');
            names.append(Pattern.quote(BOSS_NAMES[i]));
        }
        return Pattern.compile("^(?:\\[[^\\]]*]\\s*)*(" + names + ")\\s*\\[[^\\]]*]\\s*:\\s*(.+)$");
    }

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            handleMessage(message.getString().trim());
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (BossTitleState.displayTicksRemaining > 0) {
                BossTitleState.displayTicksRemaining--;
                if (BossTitleState.displayTicksRemaining == 0) {
                    BossTitleState.currentText = null;
                    BossTitleState.currentBossName = null;
                }
            }
        });
    }

    private static void handleMessage(String text) {
        if (text.isEmpty()) return;

        Matcher matcher = SCEPTER_BOSS_LINE_PATTERN.matcher(text);
        if (!matcher.matches()) {
            matcher = MAGE_RPG_BOSS_LINE_PATTERN.matcher(text);
            if (!matcher.matches()) return;
        }

        BossTitleState.currentBossName = matcher.group(1);
        BossTitleState.currentText = firstWords(matcher.group(2), MAX_WORDS);
        BossTitleState.displayTicksRemaining = DISPLAY_TICKS;

        BossTitleState.lastBossName = matcher.group(1);
        BossTitleState.lastBossMessageMs = System.currentTimeMillis();
    }

    private static String firstWords(String text, int maxWords) {
        String[] words = text.trim().split("\\s+");
        if (words.length <= maxWords) return text;
        return String.join(" ", java.util.Arrays.copyOf(words, maxWords));
    }
}
