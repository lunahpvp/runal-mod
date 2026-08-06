package com.runal.client;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SliderModuleSetting implements ModuleSetting {
    private final String label;
    private final float min;
    private final float max;
    private final float step;
    private final Supplier<Float> getter;
    private final Consumer<Float> setter;
    private final float defaultValue;

    public SliderModuleSetting(String label, float min, float max, float step, Supplier<Float> getter, Consumer<Float> setter) {
        this.label = label;
        this.min = min;
        this.max = max;
        this.step = step;
        this.getter = getter;
        this.setter = setter;
        this.defaultValue = Math.max(min, Math.min(max, getter.get()));
    }

    @Override
    public String getLabel() { return label; }

    @Override
    public String getDisplayValue() {
        float value = getValue();
        if (Math.abs(value - Math.round(value)) < 0.001f) return String.valueOf(Math.round(value));
        int decimals = step > 0f && step < 0.1f ? 2 : 1;
        return String.format("%." + decimals + "f", value);
    }

    @Override
    public void onClick() {}

    public float getNormalizedValue() { return (getValue() - min) / (max - min); }

    public void setNormalizedValue(float normalized) { setValue(min + (max - min) * Math.max(0f, Math.min(1f, normalized))); }

    public void setValue(float value) {
        if (step > 0f) value = Math.round(value / step) * step;
        setter.accept(Math.max(min, Math.min(max, value)));
        ModuleConfig.save();
    }

    private float getValue() { return Math.max(min, Math.min(max, getter.get())); }

    @Override
    public String serialize() { return String.valueOf(getValue()); }

    @Override
    public void deserialize(String value) {
        try { setter.accept(Float.parseFloat(value)); }
        catch (NumberFormatException ignored) {}
    }

    @Override
    public void resetToDefault() { setter.accept(defaultValue); }
}
