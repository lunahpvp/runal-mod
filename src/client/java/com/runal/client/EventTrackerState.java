package com.runal.client;

import java.util.LinkedHashMap;
import java.util.Map;

public class EventTrackerState {
    public static boolean enabled = true;
    public static int x = 8;
    public static int y = 90;
    public static int nameColor = 0xFF7CFFB2;
    public static int valueColor = 0xFFFFFFFF;

    public static class TrackedEvent {
        public String name;
        public int remainingSeconds;
        public Integer nextPhaseSeconds;

        public TrackedEvent(String name, int remainingSeconds) {
            this.name = name;
            this.remainingSeconds = remainingSeconds;
        }
    }

    public static final Map<String, TrackedEvent> events = new LinkedHashMap<>();
    public static final Map<String, Integer> knownDurationSeconds = new LinkedHashMap<>();

    public static void clear() {
        events.clear();
    }

    public static void put(String name, int remainingSeconds) {
        events.put(name, new TrackedEvent(name, remainingSeconds));
    }

    public static void setNextPhase(String name, int nextPhaseSeconds) {
        TrackedEvent event = events.get(name);
        if (event != null) event.nextPhaseSeconds = nextPhaseSeconds;
    }

    public static void tickDown() {
        events.values().removeIf(event -> {
            if (event.remainingSeconds > 0) {
                event.remainingSeconds--;
                if (event.remainingSeconds <= 0) {
                    if (event.nextPhaseSeconds != null) {
                        event.remainingSeconds = event.nextPhaseSeconds;
                        event.nextPhaseSeconds = null;
                        return false;
                    }
                    return true;
                }
            }
            return false;
        });
    }

    public static String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%d:%02d", m, s);
    }
}
