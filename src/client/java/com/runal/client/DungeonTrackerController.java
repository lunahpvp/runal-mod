package com.runal.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DungeonTrackerController {
    private static final String[] ROOM_CYCLE = {
            "Normal", "Normal", "Normal", "Normal", "Parkour",
            "Normal", "Normal", "Normal", "Boss", "Treasure",
    };

    private static final Map<String, String> DUNGEON_BOSS_NAMES = Map.of(
            "Forest Dungeon", "Spider Queen",
            "Divine Catacombs", "Great Paladin",
            "Heart of the Divine", "Divine Guardian",
            "Elemental Chamber", "Random Boss"
    );

    private static final Pattern ROOM_PATTERN = Pattern.compile("^(.+?)\\s*[-–—]\\s*Room (\\d+)$", Pattern.MULTILINE);
    private static final Pattern CANCELLED_PATTERN = Pattern.compile("^.+? run cancelled\\.?$", Pattern.MULTILINE);
    private static final long TIMEOUT_MS = 60_000L;

    private static int tickCounter = 0;

    private record StyledRun(String text, Style style) {
    }

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            handleMessage(message);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.player.getHealth() <= 0f) {
                DungeonTrackerState.dungeonName = null;
            }

            tickCounter++;
            if (tickCounter >= 20) {
                tickCounter = 0;
                if (DungeonTrackerState.dungeonName != null
                        && System.currentTimeMillis() - DungeonTrackerState.lastMessageMs >= TIMEOUT_MS) {
                    DungeonTrackerState.dungeonName = null;
                }
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> DungeonTrackerState.dungeonName = null);
    }

    private static void handleMessage(Component message) {
        List<StyledRun> runs = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        List<int[]> ranges = new ArrayList<>();

        message.visit((style, text) -> {
            String cleaned = stripDecoration(text);
            int start = sb.length();
            sb.append(cleaned);
            ranges.add(new int[]{start, sb.length()});
            runs.add(new StyledRun(cleaned, style));
            return Optional.<Void>empty();
        }, Style.EMPTY);

        String fullText = sb.toString();

        if (fullText.contains("[G]")) return;

        if (CANCELLED_PATTERN.matcher(fullText).find()) {
            DungeonTrackerState.dungeonName = null;
            return;
        }

        Matcher matcher = ROOM_PATTERN.matcher(fullText);
        if (!matcher.find()) return;

        String name = matcher.group(1).trim();
        DungeonTrackerState.dungeonName = name;
        DungeonTrackerState.currentRoom = Integer.parseInt(matcher.group(2));
        DungeonTrackerState.lastMessageMs = System.currentTimeMillis();
        DungeonTrackerState.bossName = DUNGEON_BOSS_NAMES.getOrDefault(name, "Boss");

        int nameStart = matcher.start(1);
        for (int i = 0; i < ranges.size(); i++) {
            int[] range = ranges.get(i);
            if (nameStart >= range[0] && nameStart < range[1]) {
                Style style = runs.get(i).style;
                if (style.getColor() != null) {
                    DungeonTrackerState.themeColor = 0xFF000000 | (style.getColor().getValue() & 0xFFFFFF);
                }
                break;
            }
        }
    }

    public static int roomsUntil(String type) {
        int currentRoom = DungeonTrackerState.currentRoom;
        for (int offset = 0; offset < ROOM_CYCLE.length; offset++) {
            int idx = (currentRoom - 1 + offset) % ROOM_CYCLE.length;
            if (ROOM_CYCLE[idx].equals(type)) return offset;
        }
        return -1;
    }

    private static String stripDecoration(String text) {
        String cleaned = text.replaceAll("[\\p{So}\\p{Co}|]", "");
        return cleaned.replaceAll("\\p{Zs}", " ");
    }
}
