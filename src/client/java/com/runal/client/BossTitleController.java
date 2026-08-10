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

    // MageRPG boss chat lines are inconsistent about the "[BOSS]" tag - sometimes it's there,
    // sometimes not (confirmed against live examples of both, plus varying separators: ":",
    // "»", no separator at all). When the tag IS present, that alone reliably marks a genuine
    // boss broadcast (players can't put "[BOSS]" in front of their own chat), so ANY name is
    // accepted there - this is what makes newly-seen bosses like "Fallen Minerian" work without
    // needing to be added to a list. When the tag is absent there's no other distinguishing
    // signal from normal player chat (players show their own level the same way too), so that
    // path stays restricted to known boss names.
    private static final String[] MAGE_RPG_BOSS_NAMES = {
            "Delta",
    };

    private static final Pattern SCEPTER_BOSS_LINE_PATTERN = buildScepterPattern();
    private static final Pattern MAGE_RPG_TAGGED_BOSS_LINE_PATTERN = Pattern.compile(
            "\\[BOSS]\\s*(.+?)\\s*\\[Lvl\\s+[^\\]]+]\\s*(?:[:»>]\\s*)?(.+)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern MAGE_RPG_NAMED_BOSS_LINE_PATTERN = buildMageRpgNamedPattern();

    private static Pattern buildScepterPattern() {
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < BOSS_NAMES.length; i++) {
            if (i > 0) names.append('|');
            names.append(Pattern.quote(BOSS_NAMES[i]));
        }
        return Pattern.compile("^(?:\\[[^\\]]*]\\s*)*(" + names + ")\\s*\\[[^\\]]*]\\s*:\\s*(.+)$");
    }

    private static Pattern buildMageRpgNamedPattern() {
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < MAGE_RPG_BOSS_NAMES.length; i++) {
            if (i > 0) names.append('|');
            names.append(Pattern.quote(MAGE_RPG_BOSS_NAMES[i]));
        }
        // Not anchored to the start/end of the line on purpose - a "[BOSS] " prefix, or any
        // other prefix, or its absence, is irrelevant as long as "Name [Lvl ...]" shows up
        // somewhere followed by a message.
        return Pattern.compile(
                "(" + names + ")\\s*\\[Lvl\\s+[^\\]]+]\\s*(?:[:»>]\\s*)?(.+)$",
                Pattern.CASE_INSENSITIVE
        );
    }

    private static final Pattern LEGACY_FORMATTING_CODE = Pattern.compile("(?i)§[0-9A-FK-OR]");

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = LEGACY_FORMATTING_CODE.matcher(message.getString()).replaceAll("");
            handleMessage(text.trim());
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
        boolean matched = matcher.matches();
        if (!matched) {
            matcher = MAGE_RPG_TAGGED_BOSS_LINE_PATTERN.matcher(text);
            matched = matcher.find();
        }
        if (!matched) {
            matcher = MAGE_RPG_NAMED_BOSS_LINE_PATTERN.matcher(text);
            matched = matcher.find();
        }
        if (!matched) return;

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
