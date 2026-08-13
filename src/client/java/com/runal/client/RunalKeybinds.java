package com.runal.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.runal.AutoSprintState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
//? if 1.21.4 || 1.21.11 {
/*import net.fabricmc.fabric.api.client.keybinding.v1.KeyMappingHelper;
*///?} else {
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
//?}
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.function.BooleanSupplier;

final class RunalKeybinds {
    private static final KeyMapping[] HOTBAR_SWAPS =
            new KeyMapping[HotbarSwapState.SLOT_COUNT];
    private static final KeyMapping[] COMMAND_BINDS =
            new KeyMapping[CommandBindState.SLOT_COUNT];

    //? if 1.21.4 {
    /*private static final String CATEGORY = "key.categories.runal.general";
    *///?} else {
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("runal", "general"));
    //?}

    private static KeyMapping autoSprint;
    private static KeyMapping lowHealth;
    private static KeyMapping fullbright;
    private static KeyMapping hidePlayers;
    private static KeyMapping hideArmor;
    private static KeyMapping healthBar;
    private static KeyMapping playerScale;
    private static KeyMapping scaleUp;
    private static KeyMapping scaleDown;
    private static KeyMapping hitboxes;
    private static KeyMapping teamTracker;
    private static KeyMapping waypointManager;
    private static KeyMapping newWaypoint;
    private static KeyMapping openMenu;
    private static KeyMapping openMenuAlt;
    private static KeyMapping autoGG;
    private static KeyMapping autoTrash;
    private static KeyMapping scepterWarps;
    private static KeyMapping scepterClass;
    private static KeyMapping scepterAchievements;
    private static KeyMapping scepterPity;
    private static KeyMapping scepterShards;
    private static KeyMapping scepterVaults;
    private static KeyMapping scepterTrash;
    private static KeyMapping scepterAfk;
    private static KeyMapping scepterRadio;
    private static KeyMapping scepterDance;
    private static KeyMapping scepterSigilShop;
    private static KeyMapping scepterClaimLuck;
    private static KeyMapping scepterSuicide;
    private static long suicideConfirmUntil;

    private static KeyMapping hideScoreboard;
    private static KeyMapping fishAlert;
    private static KeyMapping sessionManager;
    private static KeyMapping performanceHud;
    private static KeyMapping eventTracker;
    private static KeyMapping weaponCooldown;
    private static KeyMapping armorCooldown;
    private static KeyMapping accessoryCooldown;
    private static KeyMapping dungeonTracker;
    private static KeyMapping killCounter;
    private static KeyMapping armorHud;
    private static KeyMapping bossBarScale;
    private static KeyMapping viewModel;
    private static KeyMapping inventoryHud;
    private static KeyMapping discordRpc;
    private static KeyMapping runalChat;
    private static KeyMapping bossCallout;

    private RunalKeybinds() {
    }

    static void register() {
        autoSprint = register("autosprint");
        lowHealth = register("lowhealthtoggle");
        fullbright = register("fullbright");
        hidePlayers = register("hideplayers");
        hideArmor = register("hidearmor");
        healthBar = register("healthbar");
        playerScale = register("playerscale");
        scaleUp = register("scaleup");
        scaleDown = register("scaledown");
        openMenu = register("openmenu", GLFW.GLFW_KEY_RIGHT_SHIFT);
        openMenuAlt = register("openmenualt", GLFW.GLFW_KEY_LEFT_ALT);
        hitboxes = register("hitboxes");
        teamTracker = register("teamtracker");
        waypointManager = register("waypointmanager");
        newWaypoint = register("newwaypoint");

        registerSeries(HOTBAR_SWAPS, "hotbarswap");
        registerSeries(COMMAND_BINDS, "commandbind");
        scepterWarps = register("scepter_warps");
        scepterClass = register("scepter_class");
        scepterAchievements = register("scepter_achievements");
        scepterPity = register("scepter_pity");
        scepterShards = register("scepter_shards");
        scepterVaults = register("scepter_vaults");
        scepterTrash = register("scepter_trash");
        scepterAfk = register("scepter_afk");
        scepterRadio = register("scepter_radio");
        scepterDance = register("scepter_dance");
        scepterSigilShop = register("scepter_sigilshop");
        scepterClaimLuck = register("scepter_claimluck");
        scepterSuicide = register("scepter_suicide");
        autoGG = register("autogg");
        autoTrash = register("autotrash");

        hideScoreboard = register("hidescoreboard");
        fishAlert = register("fishalert");
        sessionManager = register("sessionmanager");
        performanceHud = register("performancehud");
        eventTracker = register("eventtracker");
        weaponCooldown = register("weaponcooldown");
        armorCooldown = register("armorcooldown");
        accessoryCooldown = register("accessorycooldown");
        dungeonTracker = register("dungeontracker");
        killCounter = register("killcounter");
        armorHud = register("armorhud");
        bossBarScale = register("bossbarscale");
        viewModel = register("viewmodel");
        inventoryHud = register("inventoryhud");
        discordRpc = register("discordrpc");
        runalChat = register("runalchatkeybind");
        bossCallout = register("bosscallout");

        ClientTickEvents.END_CLIENT_TICK.register(RunalKeybinds::onEndTick);
    }

    static KeyMapping autoSprint() {
        return autoSprint;
    }

    static KeyMapping lowHealth() {
        return lowHealth;
    }

    static KeyMapping fullbright() {
        return fullbright;
    }

    static KeyMapping hidePlayers() {
        return hidePlayers;
    }

    static KeyMapping hideArmor() {
        return hideArmor;
    }

    static KeyMapping healthBar() {
        return healthBar;
    }

    static KeyMapping playerScale() {
        return playerScale;
    }

    static KeyMapping hitboxes() {
        return hitboxes;
    }

    static KeyMapping teamTracker() {
        return teamTracker;
    }

    static KeyMapping waypointManager() {
        return waypointManager;
    }

    static KeyMapping newWaypoint() {
        return newWaypoint;
    }

    static KeyMapping openMenu() {
        return openMenu;
    }

    static KeyMapping autoGG() {
        return autoGG;
    }

    static KeyMapping autoTrash() {
        return autoTrash;
    }

    static KeyMapping hotbarSwap(int index) {
        return HOTBAR_SWAPS[index];
    }

    static KeyMapping commandBind(int index) {
        return COMMAND_BINDS[index];
    }

    static KeyMapping scepterWarps() { return scepterWarps; }
    static KeyMapping scepterClass() { return scepterClass; }
    static KeyMapping scepterAchievements() { return scepterAchievements; }
    static KeyMapping scepterPity() { return scepterPity; }
    static KeyMapping scepterShards() { return scepterShards; }
    static KeyMapping scepterVaults() { return scepterVaults; }
    static KeyMapping scepterTrash() { return scepterTrash; }
    static KeyMapping scepterAfk() { return scepterAfk; }
    static KeyMapping scepterRadio() { return scepterRadio; }
    static KeyMapping scepterDance() { return scepterDance; }
    static KeyMapping scepterSigilShop() { return scepterSigilShop; }
    static KeyMapping scepterClaimLuck() { return scepterClaimLuck; }
    static KeyMapping scepterSuicide() { return scepterSuicide; }

    static KeyMapping hideScoreboard() { return hideScoreboard; }
    static KeyMapping fishAlert() { return fishAlert; }
    static KeyMapping sessionManager() { return sessionManager; }
    static KeyMapping performanceHud() { return performanceHud; }
    static KeyMapping eventTracker() { return eventTracker; }
    static KeyMapping weaponCooldown() { return weaponCooldown; }
    static KeyMapping armorCooldown() { return armorCooldown; }
    static KeyMapping accessoryCooldown() { return accessoryCooldown; }
    static KeyMapping dungeonTracker() { return dungeonTracker; }
    static KeyMapping killCounter() { return killCounter; }
    static KeyMapping armorHud() { return armorHud; }
    static KeyMapping bossBarScale() { return bossBarScale; }
    static KeyMapping viewModel() { return viewModel; }
    static KeyMapping inventoryHud() { return inventoryHud; }
    static KeyMapping discordRpc() { return discordRpc; }
    static KeyMapping runalChat() { return runalChat; }
    static KeyMapping bossCallout() { return bossCallout; }

    private static KeyMapping register(String name) {
        return register(name, GLFW.GLFW_KEY_UNKNOWN);
    }

    private static KeyMapping register(String name, int defaultKey) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.runal." + name,
                InputConstants.Type.KEYSYM,
                defaultKey,
                CATEGORY
        ));
    }

    private static void registerSeries(KeyMapping[] target, String name) {
        for (int index = 0; index < target.length; index++) {
            target[index] = register(name + (index + 1));
        }
    }

    private static void onEndTick(Minecraft client) {
        drain(autoSprint, () -> toggle(
                client,
                "Auto Sprint",
                AutoSprintState.INSTANCE::toggle,
                AutoSprintState.INSTANCE::isEnabled
        ));
        drain(lowHealth, () -> toggle(
                client,
                "Low Health Warning",
                LowHealthWarning::toggle,
                LowHealthWarning::isEnabled
        ));
        drain(fullbright, () -> toggle(
                client,
                "Fullbright",
                FullbrightState.INSTANCE::toggle,
                FullbrightState.INSTANCE::isEnabled
        ));
        drain(hidePlayers, () -> toggle(
                client,
                "Hide Players",
                HidePlayersState.INSTANCE::toggle,
                HidePlayersState.INSTANCE::isEnabled
        ));
        drain(hideArmor, () -> toggle(
                client,
                "Hide Armor",
                HideArmorState.INSTANCE::toggle,
                HideArmorState.INSTANCE::isEnabled
        ));
        drain(healthBar, () -> toggle(
                client,
                "Health Bar",
                HealthBarState.INSTANCE::toggle,
                HealthBarState.INSTANCE::isEnabled
        ));
        drain(playerScale, RunalKeybinds::togglePlayerScale);
        drain(hitboxes, () -> toggle(
                client,
                "Hitboxes",
                HitboxesState.INSTANCE::toggle,
                HitboxesState.INSTANCE::isEnabled
        ));
        drain(teamTracker, () -> toggle(
                client,
                "Team Tracker",
                TeamTrackerState.INSTANCE::toggle,
                TeamTrackerState.INSTANCE::isEnabled
        ));
        drain(waypointManager, () -> client.setScreen(new WaypointManagerScreen()));
        drain(newWaypoint, () -> createWaypoint(client));
        drain(scaleUp, RunalKeybinds::increasePlayerScale);
        drain(scaleDown, RunalKeybinds::decreasePlayerScale);
        drain(openMenu, RunalScreen::open);
        drain(openMenuAlt, RunalScreen::open);

        for (int index = 0; index < HOTBAR_SWAPS.length; index++) {
            int slot = index;
            drain(HOTBAR_SWAPS[index], () ->
                    HotbarSwapController.swap(HotbarSwapState.INSTANCE.rows[slot])
            );
        }

        for (int index = 0; index < COMMAND_BINDS.length; index++) {
            int slot = index;
            drain(COMMAND_BINDS[index], () ->
                    runCommand(client, CommandBindState.INSTANCE.commands[slot])
            );
        }

        drain(scepterWarps, () -> runCommand(client, "warps"));
        drain(scepterClass, () -> runCommand(client, "class"));
        drain(scepterAchievements, () -> runCommand(client, "achievements"));
        drain(scepterPity, () -> runCommand(client, "pity"));
        drain(scepterShards, () -> runCommand(client, "shards"));
        drain(scepterVaults, () -> runCommand(client, "pv"));
        drain(scepterTrash, () -> runCommand(client, "trash"));
        drain(scepterAfk, () -> runCommand(client, "afk"));
        drain(scepterRadio, () -> runCommand(client, "radio"));
        drain(scepterDance, () -> runCommand(client, "dance"));
        drain(scepterSigilShop, () -> runCommand(client, "sigilshop"));
        drain(scepterClaimLuck, () -> runCommand(client, "claimluck"));
        drain(scepterSuicide, () -> confirmSuicide(client));

        drain(autoGG, () -> toggle(
                client,
                "Auto GG",
                AutoGGState.INSTANCE::toggle,
                AutoGGState.INSTANCE::isEnabled
        ));
        drain(autoTrash, () -> toggle(
                client,
                "Auto Trash",
                AutoTrashState.INSTANCE::toggle,
                AutoTrashState.INSTANCE::isEnabled
        ));

        drain(hideScoreboard, () -> toggle(
                client,
                "Hide Scoreboard",
                HideScoreboardState.INSTANCE::toggle,
                HideScoreboardState.INSTANCE::isEnabled
        ));
        drain(fishAlert, () -> toggle(
                client,
                "Fish Alert",
                FishAlertState.INSTANCE::toggle,
                FishAlertState.INSTANCE::isEnabled
        ));
        drain(sessionManager, () -> toggle(
                client,
                "Session Manager",
                () -> SessionManagerState.enabled = !SessionManagerState.enabled,
                () -> SessionManagerState.enabled
        ));
        drain(performanceHud, () -> toggle(
                client,
                "Performance HUD",
                () -> PerformanceHudState.enabled = !PerformanceHudState.enabled,
                () -> PerformanceHudState.enabled
        ));
        drain(eventTracker, () -> toggle(
                client,
                "Event Tracker",
                () -> EventTrackerState.enabled = !EventTrackerState.enabled,
                () -> EventTrackerState.enabled
        ));
        drain(weaponCooldown, () -> toggle(
                client,
                "Weapon Cooldown",
                () -> ItemCooldownHudState.enabled = !ItemCooldownHudState.enabled,
                () -> ItemCooldownHudState.enabled
        ));
        drain(armorCooldown, () -> toggle(
                client,
                "Armor Cooldown",
                () -> ArmorCooldownHudState.enabled = !ArmorCooldownHudState.enabled,
                () -> ArmorCooldownHudState.enabled
        ));
        drain(accessoryCooldown, () -> toggle(
                client,
                "Accessory Cooldown",
                () -> AccessoryCooldownState.enabled = !AccessoryCooldownState.enabled,
                () -> AccessoryCooldownState.enabled
        ));
        drain(dungeonTracker, () -> toggle(
                client,
                "Dungeon Tracker",
                () -> DungeonTrackerState.enabled = !DungeonTrackerState.enabled,
                () -> DungeonTrackerState.enabled
        ));
        drain(killCounter, () -> toggle(
                client,
                "Kill Counter",
                () -> BossDefeatState.enabled = !BossDefeatState.enabled,
                () -> BossDefeatState.enabled
        ));
        drain(armorHud, () -> toggle(
                client,
                "Armor HUD",
                () -> ArmorHudState.enabled = !ArmorHudState.enabled,
                () -> ArmorHudState.enabled
        ));
        drain(bossBarScale, () -> toggle(
                client,
                "Boss Bar Scale",
                BossBarScaleState.INSTANCE::toggle,
                BossBarScaleState.INSTANCE::isEnabled
        ));
        drain(viewModel, () -> toggle(
                client,
                "View Model",
                ViewModelState.INSTANCE::toggle,
                ViewModelState.INSTANCE::isEnabled
        ));
        drain(inventoryHud, () -> toggle(
                client,
                "Inventory HUD",
                () -> InventoryHudState.enabled = !InventoryHudState.enabled,
                () -> InventoryHudState.enabled
        ));
        drain(discordRpc, () -> toggle(
                client,
                "DiscordRPC",
                () -> DiscordPresenceState.enabled = !DiscordPresenceState.enabled,
                () -> DiscordPresenceState.enabled
        ));
        drain(runalChat, () -> toggle(
                client,
                "Runal Chat",
                () -> RunalChatState.enabled = !RunalChatState.enabled,
                () -> RunalChatState.enabled
        ));
        drain(bossCallout, () -> toggle(
                client,
                "Boss Callout",
                () -> BossTitleState.enabled = !BossTitleState.enabled,
                () -> BossTitleState.enabled
        ));
    }

    private static void drain(KeyMapping key, Runnable action) {
        while (key.consumeClick()) action.run();
    }

    private static void toggle(
            Minecraft client,
            String name,
            Runnable action,
            BooleanSupplier state
    ) {
        action.run();
        ModuleConfig.save();
        if (client.player == null) return;

        if (state.getAsBoolean()) Message.success(name + " enabled");
        else Message.error(name + " disabled");
    }

    private static void togglePlayerScale() {
        float current = PlayerScaleState.INSTANCE.getScale();
        PlayerScaleState.INSTANCE.setScale(Math.abs(current - 1.0f) > 0.01f ? 1.0f : 1.2f);
        reportPlayerScale();
    }

    private static void increasePlayerScale() {
        float next = Math.min(PlayerScaleState.INSTANCE.getScale() + 0.1f, 3.0f);
        PlayerScaleState.INSTANCE.setScale(next);
        reportPlayerScale();
    }

    private static void decreasePlayerScale() {
        float next = Math.max(PlayerScaleState.INSTANCE.getScale() - 0.1f, 0.1f);
        PlayerScaleState.INSTANCE.setScale(next);
        reportPlayerScale();
    }

    private static void reportPlayerScale() {
        Message.success(
                "Player scale: "
                        + String.format("%.1f", PlayerScaleState.INSTANCE.getScale())
        );
    }

    private static void runCommand(Minecraft client, String command) {
        if (client.player == null || client.getConnection() == null) return;

        String value = command.trim();
        if (value.isEmpty()) return;
        if (value.startsWith("/")) value = value.substring(1);
        client.getConnection().sendCommand(value);
    }

    private static void confirmSuicide(Minecraft client) {
        long now = System.currentTimeMillis();
        if (now <= suicideConfirmUntil) {
            suicideConfirmUntil = 0L;
            runCommand(client, "suicide");
            return;
        }

        suicideConfirmUntil = now + 3_000L;
        Message.commandError("Press the Suicide key again within 3 seconds to confirm.");
    }

    private static void createWaypoint(Minecraft client) {
        if (client.player == null) return;

        Waypoint waypoint = WaypointManagerState.INSTANCE.create(
                "Waypoint",
                (int) client.player.getX(),
                (int) client.player.getY(),
                (int) client.player.getZ(),
                WaypointManagerState.currentDimensionKey(client)
        );
        client.setScreen(new WaypointEditScreen(null, waypoint));
    }
}
