package com.runal.client;

public final class BossBarScaleState {
    public static final BossBarScaleState INSTANCE = new BossBarScaleState();

    private boolean enabled;
    public float scale = 0.7f;
    public float offsetX = 0f;
    public float offsetY = 0f;

    private BossBarScaleState() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
    }
}
