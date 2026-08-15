package com.runal.client;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class BossDefeatState {
    private static final String FILE_NAME = "runal_boss_defeats.txt";

    public static boolean enabled = true;
    public static int x = 8;
    public static int y = 350;
    public static int nameColor = 0xFF7CFFB2;
    public static int valueColor = 0xFFFFFFFF;

    // Used by ItemDropController to attribute a drop to whichever boss was killed just before
    // it - not persisted, this only matters for the few seconds after a kill.
    public static String lastKilledBossName;
    public static long lastKilledAtMs;

    private static final Map<String, Integer> defeatCounts = new LinkedHashMap<>();

    public static int getCount(String bossName) {
        return defeatCounts.getOrDefault(bossName, 0);
    }

    public static void increment(String bossName) {
        defeatCounts.merge(bossName, 1, Integer::sum);
        save();
    }

    private static Path getPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public static void save() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : defeatCounts.entrySet()) {
            sb.append(entry.getKey()).append(':').append(entry.getValue()).append('\n');
        }
        try {
            Files.writeString(getPath(), sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void load() {
        Path path = getPath();
        if (!Files.exists(path)) return;
        try {
            defeatCounts.clear();
            for (String line : Files.readAllLines(path)) {
                line = line.trim();
                if (line.isEmpty()) continue;

                int separator = line.lastIndexOf(':');
                if (separator < 0) continue;
                try {
                    String name = line.substring(0, separator);
                    int count = Integer.parseInt(line.substring(separator + 1));
                    defeatCounts.put(name, count);
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
