package com.runal.client;

import com.runal.AutoSprintState;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class BuiltinModules {

    public static void registerAll() {
        ModuleManager.register(new Module() {
            private final List<ModuleSetting> settings = List.of(new KeybindModuleSetting(RunalKeybinds.autoSprint()));
            public String getName() { return "Auto Sprint"; }
            public String getDescription() { return "Automatically sprints while moving forward."; }
            public String getCategory() { return "Combat"; }
            public boolean isEnabled() { return AutoSprintState.INSTANCE.isEnabled(); }
            public void toggle() { AutoSprintState.INSTANCE.toggle(); }
            public List<ModuleSetting> getSettings() { return settings; }
        });

        ModuleManager.register(new Module() {
            private final List<ModuleSetting> settings = List.of(
                    new SettingGroup("Display", List.of(
                            new EnumModuleSetting("Warning Mode", List.of("Sound", "Title", "Both"), () -> LowHealthWarning.warningMode, v -> LowHealthWarning.warningMode = v),
                            new ToggleModuleSetting("Low HP", () -> LowHealthWarning.lowHpEnabled, v -> LowHealthWarning.lowHpEnabled = v),
                            new SliderModuleSetting("Low HP Threshold", 0.05f, 1.0f, 0.05f, () -> LowHealthWarning.lowHpThreshold, v -> LowHealthWarning.lowHpThreshold = v),
                            new ToggleModuleSetting("Mid HP", () -> LowHealthWarning.midHpEnabled, v -> LowHealthWarning.midHpEnabled = v),
                            new SliderModuleSetting("Mid HP Threshold", 0.05f, 1.0f, 0.05f, () -> LowHealthWarning.midHpThreshold, v -> LowHealthWarning.midHpThreshold = v)
                    )),
                    new SettingGroup("Title Settings", List.of(
                            new TextModuleSetting("Low HP Title", () -> LowHealthWarning.lowHpTitle, v -> LowHealthWarning.lowHpTitle = v),
                            new TextModuleSetting("Mid HP Title", () -> LowHealthWarning.midHpTitle, v -> LowHealthWarning.midHpTitle = v)
                    )),
                    new SettingGroup("Sound Settings", List.of(
                            new ToggleModuleSetting("Sound", () -> LowHealthWarning.soundEnabled, v -> LowHealthWarning.soundEnabled = v),
                            new SliderModuleSetting("Volume", 0.1f, 2.0f, 0.1f, () -> LowHealthWarning.soundVolume, v -> LowHealthWarning.soundVolume = v)
                    )),
                    new KeybindModuleSetting(RunalKeybinds.lowHealth())
            );
            public String getName() { return "Health Indicator"; }
            public String getDescription() { return "Warns you when your health drops below a threshold."; }
            public String getCategory() { return "Combat"; }
            public boolean isEnabled() { return LowHealthWarning.isEnabled(); }
            public void toggle() { LowHealthWarning.toggle(); }
            public List<ModuleSetting> getSettings() { return settings; }
        });

        ModuleManager.register(new Module() {
            private final List<ModuleSetting> settings = List.of(
                    new ColorModuleSetting("Player Color", () -> HitboxesState.INSTANCE.playerColor, v -> HitboxesState.INSTANCE.playerColor = v),
                    new ColorModuleSetting("Entity Color", () -> HitboxesState.INSTANCE.entityColor, v -> HitboxesState.INSTANCE.entityColor = v),
                    new SliderModuleSetting("Line Width", 1.0f, 5.0f, 0.5f, () -> HitboxesState.INSTANCE.lineWidth, v -> HitboxesState.INSTANCE.lineWidth = v),
                    new KeybindModuleSetting(RunalKeybinds.hitboxes())
            );
            public String getName() { return "Hitboxes"; }
            public String getDescription() { return "Renders hitboxes around players and entities."; }
            public String getCategory() { return "Combat"; }
            public boolean isEnabled() { return HitboxesState.INSTANCE.isEnabled(); }
            public void toggle() { HitboxesState.INSTANCE.toggle(); }
            public List<ModuleSetting> getSettings() { return settings; }
        });

        ModuleManager.register(new Module() {
            private final List<ModuleSetting> settings = List.of(new KeybindModuleSetting(RunalKeybinds.fullbright()));
            public String getName() { return "Fullbright"; }
            public String getDescription() { return "Removes darkness so everything is fully lit."; }
            public String getCategory() { return "Visual"; }
            public boolean isEnabled() { return FullbrightState.INSTANCE.isEnabled(); }
            public void toggle() { FullbrightState.INSTANCE.toggle(); }
            public List<ModuleSetting> getSettings() { return settings; }
        });

        ModuleManager.register(new Module() {
            private final List<ModuleSetting> settings = List.of(new KeybindModuleSetting(RunalKeybinds.hideScoreboard()));
            public String getName() { return "Hide Scoreboard"; }
            public String getDescription() { return "Hides the sidebar scoreboard."; }
            public String getCategory() { return "Visual"; }
            public boolean isEnabled() { return HideScoreboardState.INSTANCE.isEnabled(); }
            public void toggle() { HideScoreboardState.INSTANCE.toggle(); }
            public List<ModuleSetting> getSettings() { return settings; }
        });

        ModuleManager.register(new Module() {
            private final List<ModuleSetting> settings = List.of(new KeybindModuleSetting(RunalKeybinds.hidePlayers()));
            public String getName() { return "Hide Players"; }
            public String getDescription() { return "Hides other players from rendering."; }
            public String getCategory() { return "Visual"; }
            public boolean isEnabled() { return HidePlayersState.INSTANCE.isEnabled(); }
            public void toggle() { HidePlayersState.INSTANCE.toggle(); }
            public List<ModuleSetting> getSettings() { return settings; }
        });

        ModuleManager.register(new Module() {
            private final List<ModuleSetting> settings = buildHotbarSwapSettings();
            public String getName() { return "Hotbar Swap"; }
            public String getDescription() { return "Quickly swaps hotbar slots with a keybind."; }
            public String getCategory() { return "Misc"; }
            public boolean isEnabled() { return true; }
            public void toggle() { }
            public List<ModuleSetting> getSettings() { return settings; }
        });

        ModuleManager.register(new Module() {
            private final List<ModuleSetting> settings = buildCommandBindSettings();
            public String getName() { return "Command Binds"; }
            public String getDescription() { return "Runs a chat command when a keybind is pressed."; }
            public String getCategory() { return "Misc"; }
            public boolean isEnabled() { return true; }
            public void toggle() { }
            public List<ModuleSetting> getSettings() { return settings; }
        });

        ModuleManager.register(new Module() {
            private final List<ModuleSetting> settings = List.of(
                    new TextModuleSetting("Response", () -> AutoGGState.INSTANCE.response, v -> AutoGGState.INSTANCE.response = v),
                    new ToggleModuleSetting("Epic", () -> AutoGGState.INSTANCE.triggerEpic, v -> AutoGGState.INSTANCE.triggerEpic = v),
                    new ToggleModuleSetting("Legendary", () -> AutoGGState.INSTANCE.triggerLegendary, v -> AutoGGState.INSTANCE.triggerLegendary = v),
                    new ToggleModuleSetting("Mythical", () -> AutoGGState.INSTANCE.triggerMythical, v -> AutoGGState.INSTANCE.triggerMythical = v),
                    new KeybindModuleSetting(RunalKeybinds.autoGG())
            );
            public String getName() { return "Auto GG"; }
            public String getDescription() { return "Announces a message when a player finds an important item."; }
            public String getCategory() { return "Misc"; }
            public boolean isEnabled() { return AutoGGState.INSTANCE.isEnabled(); }
            public void toggle() { AutoGGState.INSTANCE.toggle(); }
            public List<ModuleSetting> getSettings() { return settings; }
        });

        ModuleManager.register(new Module() {
            private final List<ModuleSetting> settings = List.of(
                    new ToggleModuleSetting("Trash Common", () -> AutoTrashState.INSTANCE.trashCommon, v -> AutoTrashState.INSTANCE.trashCommon = v),
                    new ToggleModuleSetting("Trash Uncommon", () -> AutoTrashState.INSTANCE.trashUncommon, v -> AutoTrashState.INSTANCE.trashUncommon = v),
                    new ToggleModuleSetting("Trash Rare", () -> AutoTrashState.INSTANCE.trashRare, v -> AutoTrashState.INSTANCE.trashRare = v),
                    new ToggleModuleSetting("Trash Epic", () -> AutoTrashState.INSTANCE.trashEpic, v -> AutoTrashState.INSTANCE.trashEpic = v),
                    new ToggleModuleSetting("Trash Legendary", () -> AutoTrashState.INSTANCE.trashLegendary, v -> AutoTrashState.INSTANCE.trashLegendary = v),
                    new ToggleModuleSetting("Trash Mythical", () -> AutoTrashState.INSTANCE.trashMythical, v -> AutoTrashState.INSTANCE.trashMythical = v),
                    new KeybindModuleSetting(RunalKeybinds.autoTrash())
            );
            public String getName() { return "Auto Trash"; }
            public String getDescription() { return "Automatically discards shards."; }
            public String getCategory() { return "Misc"; }
            public boolean isEnabled() { return AutoTrashState.INSTANCE.isEnabled(); }
            public void toggle() { AutoTrashState.INSTANCE.toggle(); }
            public List<ModuleSetting> getSettings() { return settings; }
        });

        ModuleManager.register(new Module() {
            private final List<ModuleSetting> settings = List.of(
                    new ToggleModuleSetting("Sound", () -> FishAlertState.INSTANCE.soundEnabled, v -> FishAlertState.INSTANCE.soundEnabled = v),
                    new SliderModuleSetting("Volume", 0.1f, 2.0f, 0.1f, () -> FishAlertState.INSTANCE.soundVolume, v -> FishAlertState.INSTANCE.soundVolume = v),
                    new SliderModuleSetting("Duration", 0.5f, 5.0f, 0.1f, () -> FishAlertState.INSTANCE.durationSeconds, v -> FishAlertState.INSTANCE.durationSeconds = v),
                    new ToggleModuleSetting("Particles", () -> FishAlertState.INSTANCE.particlesEnabled, v -> FishAlertState.INSTANCE.particlesEnabled = v),
                    new EnumModuleSetting("Particle Type", FishAlertState.PARTICLE_TYPES, () -> FishAlertState.INSTANCE.particleType, v -> FishAlertState.INSTANCE.particleType = v),
                    new ToggleModuleSetting("Title", () -> FishAlertState.INSTANCE.titleEnabled, v -> FishAlertState.INSTANCE.titleEnabled = v),
                    new TextModuleSetting("Title Message", () -> FishAlertState.INSTANCE.titleText, v -> FishAlertState.INSTANCE.titleText = v),
                    new KeybindModuleSetting(RunalKeybinds.fishAlert())
            );
            public String getName() { return "Fish Alert"; }
            public String getDescription() { return "Plays a sound and particles when a fish bites."; }
            public String getCategory() { return "Visual"; }
            public boolean isEnabled() { return FishAlertState.INSTANCE.isEnabled(); }
            public void toggle() { FishAlertState.INSTANCE.toggle(); }
            public List<ModuleSetting> getSettings() { return settings; }
        });

        ModuleManager.register(new Module() {
            private final List<ModuleSetting> settings = List.of(
                    new ToggleModuleSetting("Hide for Others", () -> HideArmorState.INSTANCE.hideForOthers, v -> HideArmorState.INSTANCE.hideForOthers = v),
                    new ToggleModuleSetting("Hide Helmet", () -> HideArmorState.INSTANCE.hideHelmet, v -> HideArmorState.INSTANCE.hideHelmet = v),
                    new ToggleModuleSetting("Hide Chestplate", () -> HideArmorState.INSTANCE.hideChestplate, v -> HideArmorState.INSTANCE.hideChestplate = v),
                    new ToggleModuleSetting("Hide Leggings", () -> HideArmorState.INSTANCE.hideLeggings, v -> HideArmorState.INSTANCE.hideLeggings = v),
                    new ToggleModuleSetting("Hide Boots", () -> HideArmorState.INSTANCE.hideBoots, v -> HideArmorState.INSTANCE.hideBoots = v),
                    new KeybindModuleSetting(RunalKeybinds.hideArmor())
            );
            public String getName() { return "Hide Armor"; }
            public String getDescription() { return "Hides your equipped armor pieces from rendering."; }
            public String getCategory() { return "Visual"; }
            public boolean isEnabled() { return HideArmorState.INSTANCE.isEnabled(); }
            public void toggle() { HideArmorState.INSTANCE.toggle(); }
            public List<ModuleSetting> getSettings() { return settings; }
        });

        ModuleManager.register(new Module() {
            private final HealthBarState state = HealthBarState.INSTANCE;
            private final List<ModuleSetting> settings = List.of(
                    new EnumModuleSetting("Render Mode", List.of("Third Person", "Always"), () -> state.renderMode, v -> state.renderMode = v),
                    new EnumModuleSetting("Render Style", List.of("Bar", "Compact", "Text"), () -> state.renderStyle, v -> state.renderStyle = v),
                    new SettingGroup("Display", "Display & Positioning", List.of(
                            new EnumModuleSetting("Health Format", List.of("Current", "Percent"), () -> state.healthFormat, v -> state.healthFormat = v),
                            new ToggleModuleSetting("Show Max Health", () -> state.showMaxHealth, v -> state.showMaxHealth = v)
                                    .withDescription("Shows your current maximum health."),
                            new EnumModuleSetting("Text Position", List.of("Center", "Left", "Right", "Above", "Below"), () -> state.textPosition, v -> state.textPosition = v),
                            new SliderModuleSetting("Y Offset", -40f, 40f, 1f, () -> (float) state.yOffset, v -> state.yOffset = Math.round(v))
                    )),
                    new SettingGroup("Text", "Text Customization", List.of(
                            new ColorModuleSetting("Text Color", () -> state.textColor, v -> state.textColor = v),
                            new EnumModuleSetting("Text Style", List.of("Shadow", "Flat"), () -> state.textStyle, v -> state.textStyle = v),
                            new SliderModuleSetting("Text Scale", 0.5f, 2.0f, 0.1f, () -> state.textScale, v -> state.textScale = v)
                    )),
                    new SettingGroup("Colors", "Colors & Animations", List.of(
                            new ToggleModuleSetting("Smooth Interpolation", () -> state.smoothInterpolation, v -> state.smoothInterpolation = v),
                            new ToggleModuleSetting("Damage Flash", () -> state.damageFlash, v -> state.damageFlash = v),
                            new ColorModuleSetting("Damage Flash Color", () -> state.damageFlashColor, v -> state.damageFlashColor = v),
                            new ColorModuleSetting("High HP Color", () -> state.highHpColor, v -> state.highHpColor = v),
                            new ColorModuleSetting("Mid HP Color", () -> state.midHpColor, v -> state.midHpColor = v),
                            new ColorModuleSetting("Low HP Color", () -> state.lowHpColor, v -> state.lowHpColor = v),
                            new ColorModuleSetting("Background Color", () -> state.backgroundColor, v -> state.backgroundColor = v),
                            new ColorModuleSetting("Border Color", () -> state.borderColor, v -> state.borderColor = v),
                            new SliderModuleSetting("Mid HP Threshold", 0.05f, 1.0f, 0.05f, () -> state.midHpThreshold, v -> state.midHpThreshold = v),
                            new SliderModuleSetting("Low HP Threshold", 0.05f, 1.0f, 0.05f, () -> state.lowHpThreshold, v -> state.lowHpThreshold = v)
                    )),
                    new SettingGroup("Dimensions", "Bar Dimensions", List.of(
                            new SliderModuleSetting("Width", 20f, 160f, 1f, () -> (float) state.width, v -> state.width = Math.round(v)),
                            new SliderModuleSetting("Height", 3f, 24f, 1f, () -> (float) state.height, v -> state.height = Math.round(v))
                    )),
                    new KeybindModuleSetting(RunalKeybinds.healthBar())
            );
            public String getName() { return "Health Bar"; }
            public String getDescription() { return "Displays a customizable health bar above players."; }
            public String getCategory() { return "Visual"; }
            public boolean isEnabled() { return state.isEnabled(); }
            public void toggle() { state.toggle(); }
            public List<ModuleSetting> getSettings() { return settings; }
        });

        ModuleManager.register(new Module() {
            private final List<ModuleSetting> settings = List.of(
                    new EnumModuleSetting("Target", List.of("Self", "Everyone"), () -> PlayerScaleState.INSTANCE.target, v -> PlayerScaleState.INSTANCE.target = v),
                    new SliderModuleSetting("X Scale", 0.1f, 3.0f, 0.1f, () -> PlayerScaleState.INSTANCE.getXScale(), v -> PlayerScaleState.INSTANCE.setXScale(v)),
                    new SliderModuleSetting("Y Scale", 0.1f, 3.0f, 0.1f, () -> PlayerScaleState.INSTANCE.getYScale(), v -> PlayerScaleState.INSTANCE.setYScale(v)),
                    new SliderModuleSetting("Z Scale", 0.1f, 3.0f, 0.1f, () -> PlayerScaleState.INSTANCE.getZScale(), v -> PlayerScaleState.INSTANCE.setZScale(v)),
                    new KeybindModuleSetting(RunalKeybinds.playerScale())
            );
            public String getName() { return "Player Size"; }
            public String getDescription() { return "Scales your player model up or down."; }
            public String getCategory() { return "Visual"; }
            public boolean isEnabled() { return PlayerScaleState.INSTANCE.isScaled(); }
            public void toggle() { if (isEnabled()) PlayerScaleState.INSTANCE.setScale(1.0f); else PlayerScaleState.INSTANCE.setScale(1.2f); }
            public List<ModuleSetting> getSettings() { return settings; }
        });

        ModuleManager.register(new Module() {
            private final List<ModuleSetting> settings = List.of(
                    new ToggleModuleSetting("Show HUD", () -> SessionManagerState.showHud, v -> SessionManagerState.showHud = v),
                    new ColorModuleSetting("Widget Color", () -> SessionManagerState.widgetColor, v -> SessionManagerState.widgetColor = v),
                    new ColorModuleSetting("Label Color", () -> SessionManagerState.labelColor, v -> SessionManagerState.labelColor = v),
                    new ColorModuleSetting("Value Color", () -> SessionManagerState.valueColor, v -> SessionManagerState.valueColor = v),
                    new EnumModuleSetting("Time Format", List.of("Short", "Long"), () -> SessionManagerState.timeFormat, v -> SessionManagerState.timeFormat = v),
                    new KeybindModuleSetting(RunalKeybinds.sessionManager())
            );
            public String getName() { return "Session Manager"; }
            public String getDescription() { return "Tracks and displays your play session stats."; }
            public String getCategory() { return "Tracking"; }
            public boolean isEnabled() { return SessionManagerState.enabled; }
            public void toggle() { SessionManagerState.enabled = !SessionManagerState.enabled; }
            public List<ModuleSetting> getSettings() { return settings; }
        });

        ModuleManager.register(new Module() {
            private final List<ModuleSetting> settings = List.of(
                    new ToggleModuleSetting("FPS", () -> PerformanceHudState.fps, v -> PerformanceHudState.fps = v),
                    new ToggleModuleSetting("TPS", () -> PerformanceHudState.tps, v -> PerformanceHudState.tps = v),
                    new ToggleModuleSetting("Ping", () -> PerformanceHudState.ping, v -> PerformanceHudState.ping = v),
                    new ToggleModuleSetting("Direction", () -> PerformanceHudState.direction, v -> PerformanceHudState.direction = v),
                    new ColorModuleSetting("Name Color", () -> PerformanceHudState.nameColor, v -> PerformanceHudState.nameColor = v),
                    new ColorModuleSetting("Value Color", () -> PerformanceHudState.valueColor, v -> PerformanceHudState.valueColor = v),
                    new KeybindModuleSetting(RunalKeybinds.performanceHud())
            );
            public String getName() { return "Performance HUD"; }
            public String getDescription() { return "Displays FPS, TPS, ping, and facing direction."; }
            public String getCategory() { return "Tracking"; }
            public boolean isEnabled() { return PerformanceHudState.enabled; }
            public void toggle() { PerformanceHudState.enabled = !PerformanceHudState.enabled; }
            public List<ModuleSetting> getSettings() { return settings; }
        });

        ModuleManager.register(new Module() {
            private final List<ModuleSetting> settings = List.of(
                    new ColorModuleSetting("Name Color", () -> EventTrackerState.nameColor, v -> EventTrackerState.nameColor = v),
                    new ColorModuleSetting("Value Color", () -> EventTrackerState.valueColor, v -> EventTrackerState.valueColor = v),
                    new KeybindModuleSetting(RunalKeybinds.eventTracker())
            );
            public String getName() { return "Event Tracker"; }
            public String getDescription() { return "Tracks in-game events and displays their status."; }
            public String getCategory() { return "Tracking"; }
            public boolean isEnabled() { return EventTrackerState.enabled; }
            public void toggle() { EventTrackerState.enabled = !EventTrackerState.enabled; }
            public List<ModuleSetting> getSettings() { return settings; }
        });

        ModuleManager.register(new Module() {
            private final List<ModuleSetting> settings = List.of(
                    new ColorModuleSetting("Name Color", () -> ItemCooldownHudState.nameColor, v -> ItemCooldownHudState.nameColor = v),
                    new ColorModuleSetting("Value Color", () -> ItemCooldownHudState.valueColor, v -> ItemCooldownHudState.valueColor = v),
                    new ToggleModuleSetting("Semiramis AI Cooldown", () -> SemiramisAIState.cooldownEnabled, v -> {
                        SemiramisAIState.cooldownEnabled = v;
                        if (v) SemiramisAIController.sendPhoeCommand();
                    }),
                    new ToggleModuleSetting("Semiramis Messages", () -> SemiramisAIState.showMessages, v -> SemiramisAIState.showMessages = v),
                    new KeybindModuleSetting(RunalKeybinds.weaponCooldown())
            );
            public String getName() { return "Weapon Cooldown"; }
            public String getConfigKey() { return "weapons_cooldown"; }
            public String getDescription() { return "Displays weapon ability cooldowns on screen."; }
            public String getCategory() { return "Combat"; }
            public boolean isEnabled() { return ItemCooldownHudState.enabled; }
            public void toggle() { ItemCooldownHudState.enabled = !ItemCooldownHudState.enabled; }
            public List<ModuleSetting> getSettings() { return settings; }
        });

        ModuleManager.register(new Module() {
            private final List<ModuleSetting> settings = List.of(
                    new ColorModuleSetting("Name Color", () -> ArmorCooldownHudState.nameColor, v -> ArmorCooldownHudState.nameColor = v),
                    new ColorModuleSetting("Value Color", () -> ArmorCooldownHudState.valueColor, v -> ArmorCooldownHudState.valueColor = v),
                    new KeybindModuleSetting(RunalKeybinds.armorCooldown())
            );
            public String getName() { return "Armor Cooldown"; }
            public String getDescription() { return "Displays armor ability cooldowns on screen."; }
            public String getCategory() { return "Combat"; }
            public boolean isEnabled() { return ArmorCooldownHudState.enabled; }
            public void toggle() { ArmorCooldownHudState.enabled = !ArmorCooldownHudState.enabled; }
            public List<ModuleSetting> getSettings() { return settings; }
        });

        ModuleManager.register(new Module() {
            private final List<ModuleSetting> settings = List.of(
                    new ColorModuleSetting("Name Color", () -> AccessoryCooldownState.nameColor, v -> AccessoryCooldownState.nameColor = v),
                    new ColorModuleSetting("Value Color", () -> AccessoryCooldownState.valueColor, v -> AccessoryCooldownState.valueColor = v),
                    new KeybindModuleSetting(RunalKeybinds.accessoryCooldown())
            );
            public String getName() { return "Accessory Cooldown"; }
            public String getConfigKey() { return "accessory_cooldown"; }
            public String getDescription() { return "Displays accessory ability cooldowns on screen."; }
            public String getCategory() { return "Combat"; }
            public boolean isEnabled() { return AccessoryCooldownState.enabled; }
            public void toggle() { AccessoryCooldownState.enabled = !AccessoryCooldownState.enabled; }
            public List<ModuleSetting> getSettings() { return settings; }
        });

        ModuleManager.register(new Module() {
            private final List<ModuleSetting> settings = List.of(
                    new ColorModuleSetting("Name Color", () -> DungeonTrackerState.nameColor, v -> DungeonTrackerState.nameColor = v),
                    new ColorModuleSetting("Value Color", () -> DungeonTrackerState.valueColor, v -> DungeonTrackerState.valueColor = v),
                    new KeybindModuleSetting(RunalKeybinds.dungeonTracker())
            );
            public String getName() { return "Dungeon Tracker"; }
            public String getDescription() { return "Tracks dungeon progress and displays it on screen."; }
            public String getCategory() { return "Tracking"; }
            public boolean isEnabled() { return DungeonTrackerState.enabled; }
            public void toggle() { DungeonTrackerState.enabled = !DungeonTrackerState.enabled; }
            public List<ModuleSetting> getSettings() { return settings; }
        });

        ModuleManager.register(new Module() {
            private final List<ModuleSetting> settings = List.of(
                    new ColorModuleSetting("Name Color", () -> BossDefeatState.nameColor, v -> BossDefeatState.nameColor = v),
                    new ColorModuleSetting("Value Color", () -> BossDefeatState.valueColor, v -> BossDefeatState.valueColor = v),
                    new KeybindModuleSetting(RunalKeybinds.killCounter())
            );
            public String getName() { return "Kill Counter"; }
            public String getConfigKey() { return "boss_defeat_counter"; }
            public String getDescription() { return "Counts and displays boss defeats."; }
            public String getCategory() { return "Tracking"; }
            public boolean isEnabled() { return BossDefeatState.enabled; }
            public void toggle() { BossDefeatState.enabled = !BossDefeatState.enabled; }
            public List<ModuleSetting> getSettings() { return settings; }
        });

        ModuleManager.register(new Module() {
            private final List<ModuleSetting> settings = List.of(
                    new ToggleModuleSetting("Show HUD", () -> ArmorHudState.showHud, v -> ArmorHudState.showHud = v),
                    new EnumModuleSetting("Orientation", List.of("Horizontal", "Vertical"), () -> ArmorHudState.orientation, v -> ArmorHudState.orientation = v),
                    new ColorModuleSetting("Widget Color", () -> ArmorHudState.widgetColor, v -> ArmorHudState.widgetColor = v),
                    new KeybindModuleSetting(RunalKeybinds.armorHud())
            );
            public String getName() { return "Armor HUD"; }
            public String getDescription() { return "Displays your equipped armor pieces on screen."; }
            public String getCategory() { return "Visual"; }
            public boolean isEnabled() { return ArmorHudState.enabled; }
            public void toggle() { ArmorHudState.enabled = !ArmorHudState.enabled; }
            public List<ModuleSetting> getSettings() { return settings; }
        });

        ModuleManager.register(new Module() {
            private final List<ModuleSetting> settings = List.of(
                    new SliderModuleSetting("Scale", 0.25f, 3.0f, 0.05f, () -> BossBarScaleState.INSTANCE.scale, v -> BossBarScaleState.INSTANCE.scale = v),
                    new KeybindModuleSetting(RunalKeybinds.bossBarScale())
            );
            public String getName() { return "Boss Bar Scale"; }
            public String getDescription() { return "Changes the size of boss bars without hiding their information."; }
            public String getCategory() { return "Visual"; }
            public boolean isEnabled() { return BossBarScaleState.INSTANCE.isEnabled(); }
            public void toggle() { BossBarScaleState.INSTANCE.toggle(); }
            public List<ModuleSetting> getSettings() { return settings; }
        });

        ModuleManager.register(new Module() {
            private final ViewModelState state = ViewModelState.INSTANCE;
            private final List<ModuleSetting> settings = List.of(
                    new ToggleModuleSetting("No Equip Animation", () -> state.noEquipAnimation, v -> state.noEquipAnimation = v)
                            .withDescription("Removes the item swapping animation."),
                    new ToggleModuleSetting("Apply To Hand", () -> state.applyToHand, v -> state.applyToHand = v)
                            .withDescription("Applies the viewmodel changes to the empty hand."),
                    new SliderModuleSetting("Swing Speed", 0f, 50f, 1f, () -> state.swingSpeed, v -> state.swingSpeed = v),
                    new SettingGroup("Position", List.of(
                            new SliderModuleSetting("Offset X", -2f, 2f, 0.01f, () -> state.offsetX, v -> state.offsetX = v),
                            new SliderModuleSetting("Offset Y", -2f, 2f, 0.01f, () -> state.offsetY, v -> state.offsetY = v),
                            new SliderModuleSetting("Offset Z", -2f, 2f, 0.01f, () -> state.offsetZ, v -> state.offsetZ = v)
                    )),
                    new SettingGroup("Scale", List.of(
                            new SliderModuleSetting("Scale X", 0.1f, 3f, 0.01f, () -> state.scaleX, v -> state.scaleX = v),
                            new SliderModuleSetting("Scale Y", 0.1f, 3f, 0.01f, () -> state.scaleY, v -> state.scaleY = v),
                            new SliderModuleSetting("Scale Z", 0.1f, 3f, 0.01f, () -> state.scaleZ, v -> state.scaleZ = v)
                    )),
                    new SettingGroup("Rotation", List.of(
                            new SliderModuleSetting("Rotation X", -180f, 180f, 0.5f, () -> state.rotX, v -> state.rotX = v),
                            new SliderModuleSetting("Rotation Y", -180f, 180f, 0.5f, () -> state.rotY, v -> state.rotY = v),
                            new SliderModuleSetting("Rotation Z", -180f, 180f, 0.5f, () -> state.rotZ, v -> state.rotZ = v)
                    )),
                    new SettingGroup("Swing Animation", List.of(
                            new SliderModuleSetting("Swing X", 0f, 2f, 0.01f, () -> state.swingX, v -> state.swingX = v),
                            new SliderModuleSetting("Swing Y", 0f, 2f, 0.01f, () -> state.swingY, v -> state.swingY = v),
                            new SliderModuleSetting("Swing Z", 0f, 2f, 0.01f, () -> state.swingZ, v -> state.swingZ = v)
                    )),
                    new KeybindModuleSetting(RunalKeybinds.viewModel())
            );
            public String getName() { return "View Model"; }
            public String getDescription() { return "Customize the position, scale, rotation and animations of your held items."; }
            public String getCategory() { return "Visual"; }
            public boolean isEnabled() { return state.isEnabled(); }
            public void toggle() { state.toggle(); }
            public List<ModuleSetting> getSettings() { return settings; }
        });

        ModuleManager.register(new Module() {
            private final List<ModuleSetting> settings = List.of(new KeybindModuleSetting(RunalKeybinds.inventoryHud()));
            public String getName() { return "Inventory HUD"; }
            public String getDescription() { return "Shows the three main inventory rows without opening your inventory."; }
            public String getCategory() { return "Visual"; }
            public boolean isEnabled() { return InventoryHudState.enabled; }
            public void toggle() { InventoryHudState.enabled = !InventoryHudState.enabled; }
            public List<ModuleSetting> getSettings() { return settings; }
        });

        ModuleManager.register(new Module() {
            private final List<ModuleSetting> settings = List.of(
                    new ToggleModuleSetting("Chat Notifications", () -> RunalSettings.chatNotifications, v -> RunalSettings.chatNotifications = v),
                    new ColorModuleSetting("Accent Color", () -> RunalSettings.accentColor, v -> RunalSettings.accentColor = v),
                    new ToggleModuleSetting("Rounded Menu", () -> RunalSettings.roundedPanelBottoms, v -> RunalSettings.roundedPanelBottoms = v),
                    new EnumModuleSetting("Cooldown", "Cooldown Display", List.of("Percent", "Seconds"), () -> RunalSettings.cooldownDisplayMode, v -> RunalSettings.cooldownDisplayMode = v),
                    new ButtonModuleSetting("HUD Editor", "Open", () -> Minecraft.getInstance().setScreen(new HudEditorScreen())),
                    new KeybindModuleSetting(RunalKeybinds.openMenu())
            );
            public String getName() { return "Click GUI"; }
            public String getDescription() { return "Settings for the click GUI itself."; }
            public String getCategory() { return "Misc"; }
            public boolean isEnabled() { return true; }
            public void toggle() { RunalScreen.open(); }
            public List<ModuleSetting> getSettings() { return settings; }
        });

        ModuleManager.register(new Module() {
            private final List<ModuleSetting> settings = List.of(new KeybindModuleSetting(RunalKeybinds.discordRpc()));
            public String getName() { return "DiscordRPC"; }
            public String getDescription() { return "Shows your current activity on Discord."; }
            public String getCategory() { return "Misc"; }
            public boolean isEnabled() { return DiscordPresenceState.enabled; }
            public void toggle() { DiscordPresenceState.enabled = !DiscordPresenceState.enabled; }
            public List<ModuleSetting> getSettings() { return settings; }
        });

        ModuleManager.register(new Module() {
            private final List<ModuleSetting> settings = List.of(
                    new EnumModuleSetting("Chat Color", List.copyOf(RunalChatState.CHAT_COLORS.keySet()), () -> RunalChatState.chatColor, v -> RunalChatState.chatColor = v),
                    new KeybindModuleSetting(RunalKeybinds.runalChat())
            );
            public String getName() { return "Runal Chat"; }
            public String getDescription() { return "See chat messages from Runal players on any server."; }
            public String getCategory() { return "Misc"; }
            public boolean isEnabled() { return RunalChatState.enabled; }
            public void toggle() { RunalChatState.enabled = !RunalChatState.enabled; }
            public List<ModuleSetting> getSettings() { return settings; }
        });

        ModuleManager.register(new Module() {
            private final List<ModuleSetting> settings = List.of(
                    new ToggleModuleSetting("Glow", () -> TeamTrackerState.INSTANCE.glowEnabled, v -> TeamTrackerState.INSTANCE.glowEnabled = v),
                    new ColorModuleSetting("Marker Color", () -> TeamTrackerState.INSTANCE.markerColor, v -> TeamTrackerState.INSTANCE.markerColor = v),
                    new ButtonModuleSetting("Team Selector", "Open", () -> Minecraft.getInstance().setScreen(new TeamTrackerScreen())),
                    new KeybindModuleSetting(RunalKeybinds.teamTracker())
            );
            public String getName() { return "Teammate Track"; }
            public String getDescription() { return "Tracks and highlights your party/team members."; }
            public String getCategory() { return "Visual"; }
            public boolean isEnabled() { return TeamTrackerState.INSTANCE.isEnabled(); }
            public void toggle() { TeamTrackerState.INSTANCE.toggle(); }
            public List<ModuleSetting> getSettings() { return settings; }
        });

        ModuleManager.register(new Module() {
            private final List<ModuleSetting> settings = List.of(
                    new ToggleModuleSetting("Show Beams", () -> WaypointManagerState.INSTANCE.showBeams, v -> WaypointManagerState.INSTANCE.showBeams = v),
                    new ButtonModuleSetting("Waypoint Manager", "Open", () -> Minecraft.getInstance().setScreen(new WaypointManagerScreen())),
                    new KeybindModuleSetting(RunalKeybinds.waypointManager()),
                    new KeybindModuleSetting(RunalKeybinds.newWaypoint())
            );
            public String getName() { return "Waypoints"; }
            public String getDescription() { return "Manage and display custom waypoints in the world."; }
            public String getCategory() { return "Tracking"; }
            public boolean isEnabled() { return WaypointManagerState.INSTANCE.isEnabled(); }
            public void toggle() { WaypointManagerState.INSTANCE.toggle(); }
            public List<ModuleSetting> getSettings() { return settings; }
        });

        ModuleManager.register(new Module() {
            private final List<ModuleSetting> settings = List.of(
                    new ColorModuleSetting("Text Color", () -> BossTitleState.textColor, v -> BossTitleState.textColor = v),
                    new SliderModuleSetting("Scale", 1.0f, 5.0f, 0.25f, () -> BossTitleState.scale, v -> BossTitleState.scale = v),
                    new KeybindModuleSetting(RunalKeybinds.bossCallout())
            );
            public String getName() { return "Boss Callout"; }
            public String getDescription() { return "Displays a title on screen when a boss uses an attack."; }
            public String getCategory() { return "Combat"; }
            public boolean isEnabled() { return BossTitleState.enabled; }
            public void toggle() { BossTitleState.enabled = !BossTitleState.enabled; }
            public List<ModuleSetting> getSettings() { return settings; }
        });
    }

    private static List<ModuleSetting> buildHotbarSwapSettings() {
        List<String> rowOptions = List.of(HotbarSwapState.ROW_TOP, HotbarSwapState.ROW_MIDDLE, HotbarSwapState.ROW_BOTTOM);
        List<ModuleSetting> settings = new ArrayList<>();
        for (int i = 0; i < HotbarSwapState.SLOT_COUNT; i++) {
            int idx = i;
            settings.add(new SettingGroup("Slot " + (i + 1), List.of(
                    new EnumModuleSetting("Row", rowOptions, () -> HotbarSwapState.INSTANCE.rows[idx], v -> HotbarSwapState.INSTANCE.rows[idx] = v),
                    new KeybindModuleSetting(RunalKeybinds.hotbarSwap(idx))
            )));
        }
        return settings;
    }

    private static List<ModuleSetting> buildCommandBindSettings() {
        List<ModuleSetting> settings = new ArrayList<>();
        settings.add(new KeybindModuleSetting("Warps", RunalKeybinds.scepterWarps()));
        settings.add(new KeybindModuleSetting("Class & Talents", RunalKeybinds.scepterClass()));
        settings.add(new KeybindModuleSetting("Achievements", RunalKeybinds.scepterAchievements()));
        settings.add(new KeybindModuleSetting("Pity", RunalKeybinds.scepterPity()));
        settings.add(new KeybindModuleSetting("Shards", RunalKeybinds.scepterShards()));
        settings.add(new KeybindModuleSetting("Player Vaults", RunalKeybinds.scepterVaults()));
        settings.add(new KeybindModuleSetting("Trash", RunalKeybinds.scepterTrash()));
        settings.add(new SettingGroup("Activities", List.of(
                new KeybindModuleSetting("AFK Area", RunalKeybinds.scepterAfk()),
                new KeybindModuleSetting("Radio", RunalKeybinds.scepterRadio()),
                new KeybindModuleSetting("Dance", RunalKeybinds.scepterDance())
        )));
        settings.add(new SettingGroup("Ranks", List.of(
                new KeybindModuleSetting("Sigil Shop", RunalKeybinds.scepterSigilShop()),
                new KeybindModuleSetting("Claim Luck", RunalKeybinds.scepterClaimLuck())
        )));
        settings.add(new SettingGroup("Safety", List.of(
                new KeybindModuleSetting("Suicide", RunalKeybinds.scepterSuicide())
        )));
        for (int i = 0; i < CommandBindState.SLOT_COUNT; i++) {
            int idx = i;
            settings.add(new SettingGroup("Custom Slot " + (i + 1), List.of(
                    new TextModuleSetting("Command", () -> CommandBindState.INSTANCE.commands[idx], v -> CommandBindState.INSTANCE.commands[idx] = v),
                    new KeybindModuleSetting(RunalKeybinds.commandBind(idx))
            )));
        }
        return settings;
    }
}

