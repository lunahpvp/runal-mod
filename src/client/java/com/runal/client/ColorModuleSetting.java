package com.runal.client;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class ColorModuleSetting implements ModuleSetting {
    private final String label;
    private final IntSupplier getter;
    private final IntConsumer setter;
    private final int defaultColor;

    private boolean extended = false;
    /** null = not dragging; 0 = saturation/brightness square, 1 = hue strip */
    private Integer dragSection = null;
    private String hexInput = null;
    private boolean hexEditing = false;

    public ColorModuleSetting(String label, IntSupplier getter, IntConsumer setter) {
        this.label = label;
        this.getter = getter;
        this.setter = setter;
        this.defaultColor = getter.getAsInt();
    }

    public int getColor() { return getter.getAsInt(); }

    public boolean isExtended() { return extended; }

    public Integer getDragSection() { return dragSection; }

    public void setDragSection(Integer section) { this.dragSection = section; }

    public void setColor(int color) {
        setter.accept(color);
        ModuleConfig.save();
    }

    public float[] getHSB() {
        int c = getColor();
        return java.awt.Color.RGBtoHSB((c >> 16) & 255, (c >> 8) & 255, c & 255, null);
    }

    public void setHSB(float h, float s, float br) {
        int alpha = (getColor() >>> 24) & 255;
        int rgb = java.awt.Color.HSBtoRGB(h, s, br) & 0xFFFFFF;
        setColor((alpha << 24) | rgb);
        hexInput = null;
    }

    public String getHexInput() {
        if (hexInput == null) hexInput = String.format("%06X", getColor() & 0xFFFFFF);
        return hexInput;
    }

    public boolean isHexEditing() { return hexEditing; }

    public void startHexEditing() {
        hexEditing = true;
        hexInput = String.format("%06X", getColor() & 0xFFFFFF);
    }

    public void stopHexEditing() { hexEditing = false; }

    public void appendHex(char c) {
        if (!hexEditing || getHexInput().length() >= 6) return;
        char upper = Character.toUpperCase(c);
        if (!((upper >= '0' && upper <= '9') || (upper >= 'A' && upper <= 'F'))) return;
        hexInput = getHexInput() + upper;
        if (hexInput.length() == 6) applyHex();
    }

    public void backspaceHex() {
        if (!hexEditing || getHexInput().isEmpty()) return;
        hexInput = getHexInput().substring(0, getHexInput().length() - 1);
    }

    private void applyHex() {
        try {
            int rgb = Integer.parseInt(getHexInput(), 16);
            int alpha = (getColor() >>> 24) & 255;
            setter.accept((alpha << 24) | rgb);
            ModuleConfig.save();
        } catch (NumberFormatException ignored) {
        }
    }

    @Override
    public String getLabel() { return label; }

    @Override
    public String getDisplayValue() { return String.format("#%06X", getColor() & 0xFFFFFF); }

    @Override
    public void onClick() {
        extended = !extended;
        dragSection = null;
        hexEditing = false;
        hexInput = null;
    }

    @Override
    public String serialize() { return String.valueOf(getColor()); }

    @Override
    public void deserialize(String value) {
        try { setter.accept((int) Long.parseLong(value)); }
        catch (NumberFormatException ignored) {}
    }

    @Override
    public void resetToDefault() {
        setter.accept(defaultColor);
        extended = false;
    }
}
