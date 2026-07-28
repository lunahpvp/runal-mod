package com.runal.client;

public interface ModuleSetting {
    String getLabel();
    String getDisplayValue();
    void onClick();

    default String getDescription() {
        return "";
    }

    default String getConfigKey() {
        return getLabel().toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
    }

    default String serialize() {
        return getDisplayValue();
    }

    default void deserialize(String value) {
    }

    default void resetToDefault() {
    }
}
