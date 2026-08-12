package com.runal.client;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Purely a local, client-side filter on incoming Runal Chat messages - unlike mute (server-
 *  enforced, admin-only), anyone can ignore anyone; it only affects what they personally see. */
public final class RunalChatIgnoreState {
    private static final Set<String> IGNORED = new LinkedHashSet<>();

    private RunalChatIgnoreState() {
    }

    public static boolean isIgnored(String name) {
        return IGNORED.contains(name.toLowerCase());
    }

    public static boolean ignore(String name) {
        return IGNORED.add(name.toLowerCase());
    }

    public static boolean unignore(String name) {
        return IGNORED.remove(name.toLowerCase());
    }

    public static Set<String> ignored() {
        return Collections.unmodifiableSet(IGNORED);
    }

    public static String serialize() {
        return String.join(",", IGNORED);
    }

    public static void deserialize(String value) {
        IGNORED.clear();
        if (value == null || value.isBlank()) return;
        for (String name : value.split(",")) {
            String trimmed = name.trim().toLowerCase();
            if (!trimmed.isEmpty()) IGNORED.add(trimmed);
        }
    }
}
