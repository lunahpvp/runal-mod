package com.runal.client;

import java.util.List;

public class SettingGroup implements ModuleSetting {
    private final String label;
    private final String configLabel;
    private final List<ModuleSetting> settings;
    private boolean expanded = false;

    public SettingGroup(String label, List<ModuleSetting> settings) {
        this(label, label, settings);
    }

    public SettingGroup(String label, String configLabel, List<ModuleSetting> settings) {
        this.label = label;
        this.configLabel = configLabel;
        this.settings = settings;
    }

    public List<ModuleSetting> getSettings() { return settings; }
    public boolean isExpanded() { return expanded; }

    @Override
    public String getLabel() { return label; }

    @Override
    public String getConfigKey() {
        return configLabel.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
    }

    @Override
    public String getDisplayValue() { return expanded ? "-" : "+"; }

    @Override
    public void onClick() { expanded = !expanded; }

    @Override
    public void resetToDefault() {
        expanded = false;
        for (ModuleSetting setting : settings) setting.resetToDefault();
    }
}
