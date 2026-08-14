package com.runal.client;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ModuleConfig {
    private static final String FILE_NAME = "runal.properties";
    private static final Object SAVE_LOCK = new Object();
    private static Properties pendingSave;
    private static boolean saveRunning;

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public static void save() {
        Properties props = collectProperties();
        synchronized (SAVE_LOCK) {
            pendingSave = props;
            if (saveRunning) return;
            saveRunning = true;
        }

        Thread thread = new Thread(ModuleConfig::drainSaves, "Runal Config Save");
        thread.start();
    }

    private static Properties collectProperties() {
        Properties props = new Properties();
        for (Module module : ModuleManager.getModules()) {
            String moduleKey = module.getConfigKey();
            props.setProperty(moduleKey + ".enabled", String.valueOf(module.isEnabled()));
            saveSettings(props, moduleKey, module.getSettings());
        }
        saveHudPositions(props);
        props.setProperty("runalchat.ignored", RunalChatIgnoreState.serialize());
        return props;
    }

    private static void saveHudPositions(Properties props) {
        props.setProperty("hud.session.x", String.valueOf(SessionManagerState.x));
        props.setProperty("hud.session.y", String.valueOf(SessionManagerState.y));
        props.setProperty("hud.performance.x", String.valueOf(PerformanceHudState.x));
        props.setProperty("hud.performance.y", String.valueOf(PerformanceHudState.y));
        props.setProperty("hud.armor.x", String.valueOf(ArmorHudState.x));
        props.setProperty("hud.armor.y", String.valueOf(ArmorHudState.y));
        props.setProperty("hud.inventory.x", String.valueOf(InventoryHudState.x));
        props.setProperty("hud.inventory.y", String.valueOf(InventoryHudState.y));
        props.setProperty("hud.events.x", String.valueOf(EventTrackerState.x));
        props.setProperty("hud.events.y", String.valueOf(EventTrackerState.y));
        props.setProperty("hud.item_cooldowns.x", String.valueOf(ItemCooldownHudState.x));
        props.setProperty("hud.item_cooldowns.y", String.valueOf(ItemCooldownHudState.y));
        props.setProperty("hud.dungeon_tracker.x", String.valueOf(DungeonTrackerState.x));
        props.setProperty("hud.dungeon_tracker.y", String.valueOf(DungeonTrackerState.y));
        props.setProperty("hud.low_hp_title.x", String.valueOf(LowHealthWarning.lowTitleX));
        props.setProperty("hud.low_hp_title.y", String.valueOf(LowHealthWarning.lowTitleY));
        props.setProperty("hud.mid_hp_title.x", String.valueOf(LowHealthWarning.midTitleX));
        props.setProperty("hud.mid_hp_title.y", String.valueOf(LowHealthWarning.midTitleY));
        props.setProperty("hud.boss_title.x", String.valueOf(BossTitleState.x));
        props.setProperty("hud.boss_title.y", String.valueOf(BossTitleState.y));
        props.setProperty("hud.semiramis_title.x", String.valueOf(SemiramisTitleState.x));
        props.setProperty("hud.semiramis_title.y", String.valueOf(SemiramisTitleState.y));
        props.setProperty("hud.boss_defeats.x", String.valueOf(BossDefeatState.x));
        props.setProperty("hud.boss_defeats.y", String.valueOf(BossDefeatState.y));
    }

    private static void loadHudPositions(Properties props) {
        SessionManagerState.x = getInt(props, "hud.session.x", SessionManagerState.x);
        SessionManagerState.y = getInt(props, "hud.session.y", SessionManagerState.y);
        PerformanceHudState.x = getInt(props, "hud.performance.x", PerformanceHudState.x);
        PerformanceHudState.y = getInt(props, "hud.performance.y", PerformanceHudState.y);
        ArmorHudState.x = getInt(props, "hud.armor.x", ArmorHudState.x);
        ArmorHudState.y = getInt(props, "hud.armor.y", ArmorHudState.y);
        InventoryHudState.x = getInt(props, "hud.inventory.x", InventoryHudState.x);
        InventoryHudState.y = getInt(props, "hud.inventory.y", InventoryHudState.y);
        EventTrackerState.x = getInt(props, "hud.events.x", EventTrackerState.x);
        EventTrackerState.y = getInt(props, "hud.events.y", EventTrackerState.y);
        ItemCooldownHudState.x = getInt(props, "hud.item_cooldowns.x", ItemCooldownHudState.x);
        ItemCooldownHudState.y = getInt(props, "hud.item_cooldowns.y", ItemCooldownHudState.y);
        DungeonTrackerState.x = getInt(props, "hud.dungeon_tracker.x", DungeonTrackerState.x);
        DungeonTrackerState.y = getInt(props, "hud.dungeon_tracker.y", DungeonTrackerState.y);
        LowHealthWarning.lowTitleX = getInt(props, "hud.low_hp_title.x", LowHealthWarning.lowTitleX);
        LowHealthWarning.lowTitleY = getInt(props, "hud.low_hp_title.y", LowHealthWarning.lowTitleY);
        LowHealthWarning.midTitleX = getInt(props, "hud.mid_hp_title.x", LowHealthWarning.midTitleX);
        LowHealthWarning.midTitleY = getInt(props, "hud.mid_hp_title.y", LowHealthWarning.midTitleY);
        BossTitleState.x = getInt(props, "hud.boss_title.x", BossTitleState.x);
        BossTitleState.y = getInt(props, "hud.boss_title.y", BossTitleState.y);
        SemiramisTitleState.x = getInt(props, "hud.semiramis_title.x", SemiramisTitleState.x);
        SemiramisTitleState.y = getInt(props, "hud.semiramis_title.y", SemiramisTitleState.y);
        BossDefeatState.x = getInt(props, "hud.boss_defeats.x", BossDefeatState.x);
        BossDefeatState.y = getInt(props, "hud.boss_defeats.y", BossDefeatState.y);
    }

    private static int getInt(Properties props, String key, int fallback) {
        String value = props.getProperty(key);
        if (value == null) return fallback;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static void drainSaves() {
        while (true) {
            Properties props;
            synchronized (SAVE_LOCK) {
                props = pendingSave;
                pendingSave = null;
                if (props == null) {
                    saveRunning = false;
                    return;
                }
            }

            try (OutputStream out = Files.newOutputStream(getConfigPath())) {
                props.store(out, "Runal module states and settings");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void load() {
        Path path = getConfigPath();
        if (!Files.exists(path)) return;

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        for (Module module : ModuleManager.getModules()) {
            String moduleKey = module.getConfigKey();
            String saved = props.getProperty(moduleKey + ".enabled");
            if (saved == null) saved = props.getProperty(module.getName());
            if (saved != null) {
                boolean shouldBeEnabled = Boolean.parseBoolean(saved);
                if (module.isEnabled() != shouldBeEnabled) module.toggle();
            }
            loadSettings(props, moduleKey, module.getSettings());
        }
        migrateTrackingStyleDefaults();
        loadHudPositions(props);
        RunalChatIgnoreState.deserialize(props.getProperty("runalchat.ignored"));
    }

    private static void migrateTrackingStyleDefaults() {
        if (SessionManagerState.widgetColor == 0xAA101216) SessionManagerState.widgetColor = 0xFF35D77A;
        if (SessionManagerState.labelColor == 0xFFA7A8B2) SessionManagerState.labelColor = 0xFF7CFFB2;
        if (PerformanceHudState.nameColor == 0xFFA7A8B2) PerformanceHudState.nameColor = 0xFF7CFFB2;
        if (EventTrackerState.nameColor == 0xFFA7A8B2) EventTrackerState.nameColor = 0xFF7CFFB2;
        if (DungeonTrackerState.nameColor == 0xFFA7A8B2) DungeonTrackerState.nameColor = 0xFF7CFFB2;
        if (BossDefeatState.nameColor == 0xFFA7A8B2) BossDefeatState.nameColor = 0xFF7CFFB2;
    }

    private static void saveSettings(Properties props, String prefix, Iterable<ModuleSetting> settings) {
        for (ModuleSetting setting : settings) {
            String settingKey = prefix + "." + setting.getConfigKey();
            if (setting instanceof SettingGroup group) {
                props.setProperty(settingKey + ".expanded", String.valueOf(group.isExpanded()));
                saveSettings(props, settingKey, group.getSettings());
            } else if (!(setting instanceof ButtonModuleSetting)) {
                props.setProperty(settingKey, setting.serialize());
            }
        }
    }

    private static void loadSettings(Properties props, String prefix, Iterable<ModuleSetting> settings) {
        for (ModuleSetting setting : settings) {
            String settingKey = prefix + "." + setting.getConfigKey();
            if (setting instanceof SettingGroup group) {
                loadSettings(props, settingKey, group.getSettings());
            } else {
                String saved = props.getProperty(settingKey);
                if (saved != null) setting.deserialize(saved);
            }
        }
    }

}
