package com.runal.client;

public final class TooltipScaleState {
    public static final TooltipScaleState INSTANCE = new TooltipScaleState();

    private boolean enabled;
    public float scale = 1.0f;

    private TooltipScaleState() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
    }
}
