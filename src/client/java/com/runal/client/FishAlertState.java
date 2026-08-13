package com.runal.client;

public final class FishAlertState {
    public static final FishAlertState INSTANCE = new FishAlertState();

    private boolean enabled = true;

    private FishAlertState() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
    }
}
