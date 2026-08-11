package com.runal.client;

/**
 * MageRPG has no scoreboard/tab-list/dimension signal for which zone you're in - the only
 * thing that reliably tells the zones apart is player position. Per the mapping the user
 * gave: Plains and Desert occupy the same coordinate space with no way to separate them
 * positionally, so they're reported as one zone. Sky is a separate, huge landmass far into
 * the negative quadrant (sampled around -5000, 160, -5000); Lower vs Upper Sky is a
 * Y-level split within that same area that you can fly/descend through.
 *
 * SKY_SPLIT_Y is an unconfirmed guess - the user wasn't sure of the exact altitude where
 * Lower Sky becomes Upper Sky, just that one exists. Tighten it once the real boundary is known.
 */
public final class WorldRegions {

    public enum Region {
        PLAINS_DESERT("Plains & Desert"),
        LOWER_SKY("Lower Sky"),
        UPPER_SKY("Upper Sky"),
        UNKNOWN(null);

        public final String label;

        Region(String label) {
            this.label = label;
        }
    }

    private static final double SKY_COORD_THRESHOLD = -2000.0;
    private static final double SKY_SPLIT_Y = 200.0;

    private WorldRegions() {
    }

    public static boolean isMageRpg() {
        return "MageRPG".equals(DiscordPresenceController.detectedServer());
    }

    public static Region regionOf(double x, double y, double z) {
        if (x <= SKY_COORD_THRESHOLD && z <= SKY_COORD_THRESHOLD) {
            return y >= SKY_SPLIT_Y ? Region.UPPER_SKY : Region.LOWER_SKY;
        }
        return Region.PLAINS_DESERT;
    }
}
