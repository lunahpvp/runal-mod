package com.runal.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.HitResult;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class DiscordPresenceController {
    private static final String SCEPTER_CLIENT_ID = "1523235604106580019";
    private static final String SCEPTER_LARGE_IMAGE_KEY = "runal_icon";
    private static final String MAGE_RPG_CLIENT_ID = "1536203081027555328";
    private static final String MAGE_RPG_LARGE_IMAGE_KEY = "mage";
    private static final String SCEPTER_DISCORD = "https://discord.gg/98FWkw7VtD";
    private static final String MAGE_RPG_DISCORD = "https://discord.gg/7CYqksVRkC";
    private static final String RUNAL_DISCORD = "https://discord.gg/G9JrtKjQdh";
    private static final long RECONNECT_INTERVAL_MS = 15_000L;
    private static final long MIN_UPDATE_INTERVAL_MS = 15_000L;
    private static final int MINING_CONFIRM_TICKS = 10;
    private static final long MINING_HOLD_MS = 2_000L;

    private static final String[] MAGE_RPG_ADDRESSES = {
            "magerpg.minehut.gg",
            "magerpg.minekeep.gg",
    };
    private static final String[] SCEPTER_RPG_ADDRESSES = {
            "scepterrpg.minehut.gg",
            "scepterrpg.minekeep.gg",
    };

    private static volatile String detectedServerName = "ScepterRPG";

    private static int miningStreakTicks = 0;
    private static long lastConfirmedMiningMs = 0;

    private static final DiscordIpcClient client = new DiscordIpcClient();
    private static final AtomicBoolean running = new AtomicBoolean(true);

    private static volatile String pendingDetails;
    private static volatile String pendingState;
    private static String lastSentDetails;
    private static String lastSentState;
    private static long lastSendMs;
    private static long lastConnectAttemptMs;
    private static long sessionStartMs;
    private static String activeClientId;

    public static void register() {
        sessionStartMs = System.currentTimeMillis();

        Thread thread = new Thread(DiscordPresenceController::ipcLoop, "Runal Discord RPC");
        thread.setDaemon(true);
        thread.start();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            var serverData = handler.getServerData();
            detectedServerName = detectServerName(serverData != null ? serverData.ip : null);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> detectedServerName = "ScepterRPG");

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (DiscordPresenceState.enabled) updatePendingText();
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> {
            running.set(false);
            client.close();
        });
    }

    private static String currentClientId() {
        return "MageRPG".equals(detectedServerName) ? MAGE_RPG_CLIENT_ID : SCEPTER_CLIENT_ID;
    }

    private static String currentLargeImageKey() {
        return "MageRPG".equals(detectedServerName) ? MAGE_RPG_LARGE_IMAGE_KEY : SCEPTER_LARGE_IMAGE_KEY;
    }

    private static String detectServerName(String ip) {
        if (ip == null) return "ScepterRPG";
        String normalized = ip.toLowerCase();
        for (String address : MAGE_RPG_ADDRESSES) {
            if (normalized.contains(address)) return "MageRPG";
        }
        for (String address : SCEPTER_RPG_ADDRESSES) {
            if (normalized.contains(address)) return "ScepterRPG";
        }
        return "ScepterRPG";
    }

    private static void updatePendingText() {
        pendingDetails = "Playing on " + detectedServerName;

        if (BossTitleState.isFightingBoss()) {
            pendingState = "Fighting " + BossTitleState.lastBossName + " in " + detectedServerName;
            return;
        }

        if (DungeonTrackerState.dungeonName != null) {
            pendingState = "Inside " + DungeonTrackerState.dungeonName + " Dungeon";
            return;
        }

        if (isFishing()) {
            pendingState = "Fishing in the Oasis";
            return;
        }

        if (isMining()) {
            pendingState = "Mining in the Caverns";
            return;
        }

        if (!EventTrackerState.events.isEmpty()) {
            pendingState = EventTrackerState.events.values().iterator().next().name;
            return;
        }

        pendingState = null;
    }

    private static boolean isMining() {
        Minecraft mc = Minecraft.getInstance();
        boolean punchingBlock = mc.player != null && mc.hitResult != null
                && mc.options.keyAttack.isDown() && mc.hitResult.getType() == HitResult.Type.BLOCK;

        // Require a bit of sustained punching before this counts - a single accidental left-click
        // on a block shouldn't flip the status, especially since Discord updates are throttled and
        // a one-tick blip could otherwise get stuck showing "Mining" for up to MIN_UPDATE_INTERVAL_MS.
        if (punchingBlock) {
            miningStreakTicks++;
            if (miningStreakTicks >= MINING_CONFIRM_TICKS) lastConfirmedMiningMs = System.currentTimeMillis();
        } else {
            miningStreakTicks = 0;
        }

        return System.currentTimeMillis() - lastConfirmedMiningMs < MINING_HOLD_MS;
    }

    private static boolean isFishing() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return false;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof FishingHook hook && hook.getPlayerOwner() == mc.player) return true;
        }
        return false;
    }

    private static void ipcLoop() {
        while (running.get()) {
            if (!DiscordPresenceState.enabled) {
                if (client.isConnected()) client.close();
                sleep(1000);
                continue;
            }

            String targetClientId = currentClientId();
            boolean needsReconnect = client.isConnected() && !targetClientId.equals(activeClientId);

            if (!client.isConnected() || needsReconnect) {
                long now = System.currentTimeMillis();
                if (now - lastConnectAttemptMs < RECONNECT_INTERVAL_MS && !needsReconnect) {
                    sleep(500);
                    continue;
                }
                lastConnectAttemptMs = now;
                if (!client.connect(targetClientId)) {
                    activeClientId = null;
                    sleep(500);
                    continue;
                }
                activeClientId = targetClientId;
                lastSentDetails = null;
                lastSentState = null;
            }

            long now = System.currentTimeMillis();
            String details = pendingDetails;
            String state = pendingState;
            boolean changed = !equalsNullable(details, lastSentDetails) || !equalsNullable(state, lastSentState);
            boolean canSend = now - lastSendMs >= MIN_UPDATE_INTERVAL_MS;

            if (changed && canSend) {
                try {
                    client.sendActivity(buildActivityJson(details, state));
                    lastSentDetails = details;
                    lastSentState = state;
                    lastSendMs = now;
                } catch (IOException e) {
                    client.close();
                }
            }

            sleep(500);
        }
    }

    private static boolean equalsNullable(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String buildActivityJson(String details, String state) {
        JsonObject activity = new JsonObject();
        if (details != null) activity.addProperty("details", details);
        if (state != null) activity.addProperty("state", state);

        JsonObject timestamps = new JsonObject();
        timestamps.addProperty("start", sessionStartMs);
        activity.add("timestamps", timestamps);

        JsonObject assets = new JsonObject();
        assets.addProperty("large_image", currentLargeImageKey());
        assets.addProperty("large_text", "Runal");
        activity.add("assets", assets);

        JsonArray buttons = new JsonArray();
        boolean onMageRpg = "MageRPG".equals(detectedServerName);
        JsonObject serverButton = new JsonObject();
        serverButton.addProperty("label", onMageRpg ? "Play MageRPG" : "Play ScepterRPG");
        serverButton.addProperty("url", onMageRpg ? MAGE_RPG_DISCORD : SCEPTER_DISCORD);
        buttons.add(serverButton);

        JsonObject runalButton = new JsonObject();
        runalButton.addProperty("label", "Use Runal");
        runalButton.addProperty("url", RUNAL_DISCORD);
        buttons.add(runalButton);
        activity.add("buttons", buttons);

        JsonObject args = new JsonObject();
        args.addProperty("pid", ProcessHandle.current().pid());
        args.add("activity", activity);

        JsonObject root = new JsonObject();
        root.addProperty("cmd", "SET_ACTIVITY");
        root.add("args", args);
        root.addProperty("nonce", UUID.randomUUID().toString());

        return root.toString();
    }
}
