package com.runal.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// Vanilla item cooldowns only expose a percent, not a total duration. This estimates it from
// how the percent changes between two samples.
class CooldownDurationEstimator {
    private record Ref(long tick, float percent) {
    }

    private static final Map<String, Ref> refs = new HashMap<>();

    static Integer estimateRemainingTicks(String key, float percent, long nowTick) {
        Ref ref = refs.get(key);
        if (ref == null || percent > ref.percent()) {
            refs.put(key, new Ref(nowTick, percent));
            return null;
        }

        long elapsedTicks = nowTick - ref.tick();
        float percentDrop = ref.percent() - percent;
        if (elapsedTicks <= 0 || percentDrop <= 0.0001f) return null;

        float totalTicks = elapsedTicks / percentDrop;
        return Math.round(percent * totalTicks);
    }

    static void pruneExcept(String prefix, Set<String> activeKeys) {
        refs.keySet().removeIf(k -> k.startsWith(prefix) && !activeKeys.contains(k));
    }
}
