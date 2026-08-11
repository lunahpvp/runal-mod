package com.runal.client;

/**
 * MageRPG has no scoreboard/tab-list/dimension signal for which zone you're in - the only
 * thing that reliably tells the zones apart is player position. Per the user's own
 * coordinates: Plains/Desert starts around (0, 114, 0), Lower Sky starts around
 * (-5000, 100, -5000), and Upper Sky starts around (-10000, 118, -10000) - three anchor
 * points strung out along the same diagonal, not a vertical Y-level split as originally
 * guessed. Plains and Desert share the same coordinate space with no way to separate them
 * positionally, so they're reported as one zone. Classification is nearest-anchor: whichever
 * of the three start points a position is closest to (in the X/Z plane) is its region.
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

    private static final double PLAINS_DESERT_X = 0.0;
    private static final double PLAINS_DESERT_Z = 0.0;
    private static final double LOWER_SKY_X = -5000.0;
    private static final double LOWER_SKY_Z = -5000.0;
    private static final double UPPER_SKY_X = -10000.0;
    private static final double UPPER_SKY_Z = -10000.0;

    private WorldRegions() {
    }

    public static boolean isMageRpg() {
        return "MageRPG".equals(DiscordPresenceController.detectedServer());
    }

    public static Region regionOf(double x, double y, double z) {
        double distPlainsDesert = distanceSq(x, z, PLAINS_DESERT_X, PLAINS_DESERT_Z);
        double distLowerSky = distanceSq(x, z, LOWER_SKY_X, LOWER_SKY_Z);
        double distUpperSky = distanceSq(x, z, UPPER_SKY_X, UPPER_SKY_Z);

        if (distUpperSky <= distLowerSky && distUpperSky <= distPlainsDesert) return Region.UPPER_SKY;
        if (distLowerSky <= distPlainsDesert) return Region.LOWER_SKY;
        return Region.PLAINS_DESERT;
    }

    private static double distanceSq(double x, double z, double anchorX, double anchorZ) {
        double dx = x - anchorX;
        double dz = z - anchorZ;
        return dx * dx + dz * dz;
    }
}
