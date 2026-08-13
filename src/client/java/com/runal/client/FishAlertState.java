package com.runal.client;

import java.util.List;

public final class FishAlertState {
    public static final FishAlertState INSTANCE = new FishAlertState();

    public static final List<String> PARTICLE_TYPES = List.of(
            "Electric Spark", "Splash", "Bubble", "Happy Villager", "Enchant",
            "Crit", "Firework", "Soul Fire", "Totem", "Heart"
    );

    private boolean enabled = true;
    public boolean soundEnabled = true;
    public float soundVolume = 1.0f;
    public boolean particlesEnabled = true;
    public String particleType = "Electric Spark";
    public float durationSeconds = 1.4f;

    private FishAlertState() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
    }
}
