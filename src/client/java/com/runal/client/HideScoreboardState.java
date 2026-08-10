package com.runal.client;

public final class HideScoreboardState {
    public static final HideScoreboardState INSTANCE = new HideScoreboardState();

    private boolean enabled;

    private HideScoreboardState() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
    }
}
