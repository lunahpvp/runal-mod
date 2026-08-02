package com.runal.client;

public final class BossBarScaleState {
    public static final BossBarScaleState INSTANCE = new BossBarScaleState();

    private boolean enabled;
    public float scale = 0.7f;

    private BossBarScaleState() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
    }
}
