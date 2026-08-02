package com.runal.client;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class EnumModuleSetting implements ModuleSetting {
    private final String label;
    private final String configLabel;
    private final List<String> values;
    private final Supplier<String> getter;
    private final Consumer<String> setter;
    private final String defaultValue;

    public EnumModuleSetting(String label, List<String> values, Supplier<String> getter, Consumer<String> setter) {
        this(label, label, values, getter, setter);
    }

    public EnumModuleSetting(String label, String configLabel, List<String> values, Supplier<String> getter, Consumer<String> setter) {
        this.label = label;
        this.configLabel = configLabel;
        this.values = values;
        this.getter = getter;
        this.setter = setter;
        this.defaultValue = getter.get();
    }

    @Override
    public String getLabel() { return label; }

    @Override
    public String getConfigKey() {
        return configLabel.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
    }

    @Override
    public String getDisplayValue() { return getter.get(); }

    @Override
    public void onClick() { cycle(1); }

    public void cycle(int direction) {
        int index = values.indexOf(getter.get());
        if (index < 0) index = 0;
        int next = Math.floorMod(index + direction, values.size());
        setter.accept(values.get(next));
        ModuleConfig.save();
    }

    @Override
    public String serialize() { return getter.get(); }

    @Override
    public void deserialize(String value) {
        if (values.contains(value)) setter.accept(value);
    }

    @Override
    public void resetToDefault() { setter.accept(defaultValue); }
}
