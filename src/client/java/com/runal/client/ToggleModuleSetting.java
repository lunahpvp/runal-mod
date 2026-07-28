package com.runal.client;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class ToggleModuleSetting implements ModuleSetting {
    private final String label;
    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;
    private final boolean defaultValue;
    private String description = "";

    public ToggleModuleSetting(String label, BooleanSupplier getter, Consumer<Boolean> setter) {
        this.label = label;
        this.getter = getter;
        this.setter = setter;
        this.defaultValue = getter.getAsBoolean();
    }

    public ToggleModuleSetting withDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String getDescription() { return description; }

    public boolean getValue() {
        return getter.getAsBoolean();
    }

    public void setValue(boolean value) {
        setter.accept(value);
        ModuleConfig.save();
    }

    @Override
    public String getLabel() { return label; }

    @Override
    public String getDisplayValue() { return getValue() ? "On" : "Off"; }

    @Override
    public void onClick() { setValue(!getValue()); }

    @Override
    public String serialize() { return String.valueOf(getValue()); }

    @Override
    public void deserialize(String value) { setter.accept(Boolean.parseBoolean(value)); }

    @Override
    public void resetToDefault() { setter.accept(defaultValue); }
}
