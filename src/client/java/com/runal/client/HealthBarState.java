package com.runal.client;

public class HealthBarState {
    public static final HealthBarState INSTANCE = new HealthBarState();

    private boolean enabled = false;
    public String renderMode = "Third Person";
    public String renderStyle = "Bar";
    public String healthFormat = "Current";
    public boolean showMaxHealth = false;
    public String textPosition = "Center";
    public int yOffset = 0;
    public int textColor = 0xFFFFFFFF;
    public String textStyle = "Shadow";
    public float textScale = 1.0f;
    public boolean smoothInterpolation = true;
    public boolean damageFlash = true;
    public int damageFlashColor = 0xFFFF5555;
    public int highHpColor = 0xFF35D77A;
    public int midHpColor = 0xFFFFFF55;
    public int lowHpColor = 0xFFFF5555;
    public int backgroundColor = 0xAA202228;
    public int borderColor = 0xCC000000;
    public float midHpThreshold = 0.50f;
    public float lowHpThreshold = 0.30f;
    public int width = 66;
    public int height = 8;

    private HealthBarState() {}
    public boolean isEnabled() { return enabled; }
    public void toggle() { enabled = !enabled; }
}
