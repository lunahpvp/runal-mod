package com.runal.client;

public final class ViewModelState {
    public static final ViewModelState INSTANCE = new ViewModelState();

    private boolean enabled;

    public boolean noEquipAnimation = false;
    public boolean applyToHand = false;
    public float swingSpeed = 0f;

    public float offsetX = 0f;
    public float offsetY = 0f;
    public float offsetZ = 0f;

    public float scaleX = 1f;
    public float scaleY = 1f;
    public float scaleZ = 1f;

    public float rotX = 0f;
    public float rotY = 0f;
    public float rotZ = 0f;

    public float swingX = 1f;
    public float swingY = 1f;
    public float swingZ = 1f;

    private ViewModelState() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
    }
}
