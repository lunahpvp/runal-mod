package com.runal.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
//? if 26.1.2 {
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry;
//?}

public final class RunalClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        RunalKeybinds.register();
        RunalCommands.register();
        TerrainMapCache.register();
        registerPictureInPictureRenderer();

        BuiltinModules.registerAll();
        loadPersistentState();
        registerFeatures();

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> savePersistentState());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                Message.info("Press Right Shift or Left Alt to open the Runal menu")
        );
    }

    private static void registerPictureInPictureRenderer() {
        //? if 26.1.2 {
        PictureInPictureRendererRegistry.register(context ->
                new NVGPIPRenderer(context.bufferSource())
        );
        //?}
    }

    private static void loadPersistentState() {
        ModuleConfig.load();
        TeamTrackerState.INSTANCE.load();
        WaypointManagerState.INSTANCE.load();
        BossDefeatState.load();
    }

    private static void savePersistentState() {
        ModuleConfig.save();
        TeamTrackerState.INSTANCE.save();
        WaypointManagerState.INSTANCE.save();
    }

    private static void registerFeatures() {
        LowHealthWarning.register();
        HealthBarRenderer.register();
        UtilityHudRenderer.register();
        HitboxRenderer.register();
        TeamTrackerRenderer.register();
        WaypointRenderer.register();

        AutoGGController.register();
        AutoTrashController.register();
        PartyController.register();
        EventTrackerController.register();
        RealPlayerTracker.register();
        BossTitleController.register();
        DiscordPresenceController.register();
        ArmorCooldownController.register();
        AccessoryCooldownController.register();
        DungeonTrackerController.register();
        BossDefeatController.register();
        RunalPresenceClient.register();
    }
}
