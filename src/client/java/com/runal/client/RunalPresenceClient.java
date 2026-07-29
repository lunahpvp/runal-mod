package com.runal.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class RunalPresenceClient {
    private static final Logger LOGGER = LoggerFactory.getLogger("Runal Presence");
    private static final URI PRESENCE_URI =
            URI.create("wss://runal-presence.lake-cockroach.workers.dev/presence");
    private static final long RECONNECT_DELAY_SECONDS = 5L;

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(task, "Runal-Presence-Reconnect");
                thread.setDaemon(true);
                return thread;
            });
    private static final Set<String> ACTIVE_USERS = ConcurrentHashMap.newKeySet();
    private static final Set<String> ACTIVE_NAMES = ConcurrentHashMap.newKeySet();
    private static final AtomicBoolean CONNECTING = new AtomicBoolean(false);
    private static final AtomicInteger GENERATION = new AtomicInteger();

    private static volatile boolean connectionWanted;
    private static volatile WebSocket socket;

    private RunalPresenceClient() {
    }

    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (handler.getServerData() != null) connect();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> disconnect());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> disconnect());
    }

    public static boolean isActiveUser(UUID uuid, String name) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            if (uuid != null && uuid.equals(client.player.getUUID())) return true;
            if (name != null && normalizeName(name).equals(
                    normalizeName(client.player.getGameProfile().name())
            )) return true;
        }
        return (uuid != null && ACTIVE_USERS.contains(normalizeUuid(uuid.toString())))
                || (name != null && ACTIVE_NAMES.contains(normalizeName(name)));
    }

    private static void connect() {
        connectionWanted = true;
        if (socket != null || !CONNECTING.compareAndSet(false, true)) return;

        int generation = GENERATION.incrementAndGet();
        HTTP_CLIENT.newWebSocketBuilder()
                .buildAsync(PRESENCE_URI, new Listener(generation))
                .whenComplete((openedSocket, error) -> {
                    CONNECTING.set(false);
                    if (error != null) {
                        LOGGER.warn("Presence connection failed: {}", error.getMessage());
                        scheduleReconnect(generation);
                        return;
                    }
                    if (!connectionWanted || generation != GENERATION.get()) {
                        openedSocket.abort();
                        return;
                    }
                    socket = openedSocket;
                    LOGGER.info("Connected to Runal presence");
                });
    }

    private static void disconnect() {
        connectionWanted = false;
        GENERATION.incrementAndGet();
        CONNECTING.set(false);
        ACTIVE_USERS.clear();
        ACTIVE_NAMES.clear();

        WebSocket current = socket;
        socket = null;
        if (current != null) {
            try {
                current.sendClose(WebSocket.NORMAL_CLOSURE, "disconnecting");
            } catch (Exception ignored) {
                current.abort();
            }
        }
    }

    private static void scheduleReconnect(int generation) {
        if (!connectionWanted || generation != GENERATION.get()) return;
        SCHEDULER.schedule(() -> {
            if (connectionWanted && generation == GENERATION.get()) connect();
        }, RECONNECT_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    private static void authenticate(WebSocket webSocket, String serverId) {
        CompletableFuture.runAsync(() -> {
            Minecraft client = Minecraft.getInstance();
            User user = client.getUser();
            try {
                //? if 1.21.4 {
                /*client.getMinecraftSessionService().joinServer(
                        user.getProfileId(),
                        user.getAccessToken(),
                        serverId
                );
                *///?} else {
                client.services().sessionService().joinServer(
                        user.getProfileId(),
                        user.getAccessToken(),
                        serverId
                );
                //?}

                JsonObject response = new JsonObject();
                response.addProperty("action", "auth_response");
                response.addProperty("name", user.getName());
                response.addProperty("uuid", user.getProfileId().toString());
                response.addProperty("version", currentVersion());
                webSocket.sendText(response.toString(), true);
            } catch (Exception error) {
                LOGGER.warn("Could not authenticate Runal presence: {}", error.getMessage());
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "authentication failed");
            }
        });
    }

    private static void handleMessage(WebSocket webSocket, String text) {
        try {
            JsonObject message = JsonParser.parseString(text).getAsJsonObject();
            String action = message.get("action").getAsString();
            if ("auth_request".equals(action)) {
                authenticate(webSocket, message.get("serverId").getAsString());
                return;
            }
            if ("auth_success".equals(action)) {
                LOGGER.info("Authenticated with Runal presence");
                return;
            }
            if ("sync".equals(action)) {
                JsonArray users = message.getAsJsonArray("users");
                ACTIVE_USERS.clear();
                ACTIVE_NAMES.clear();
                for (JsonElement userElement : users) {
                    JsonObject user = userElement.getAsJsonObject();
                    ACTIVE_USERS.add(normalizeUuid(user.get("uuid").getAsString()));
                    ACTIVE_NAMES.add(normalizeName(user.get("name").getAsString()));
                }
                LOGGER.info("Runal presence synced {} user(s)", ACTIVE_USERS.size());
            }
        } catch (Exception error) {
            LOGGER.debug("Ignored invalid presence message: {}", error.getMessage());
        }
    }

    private static String currentVersion() {
        return FabricLoader.getInstance()
                .getModContainer("runal")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    private static String normalizeUuid(String uuid) {
        return uuid.replace("-", "").toLowerCase();
    }

    private static String normalizeName(String name) {
        return name.toLowerCase();
    }

    private static final class Listener implements WebSocket.Listener {
        private final int generation;
        private final StringBuilder messageBuffer = new StringBuilder();

        private Listener(int generation) {
            this.generation = generation;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            WebSocket.Listener.super.onOpen(webSocket);
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            messageBuffer.append(data);
            if (last) {
                String message = messageBuffer.toString();
                messageBuffer.setLength(0);
                handleMessage(webSocket, message);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            if (generation == GENERATION.get()) {
                LOGGER.warn(
                        "Presence socket closed ({}): {}",
                        statusCode,
                        reason == null || reason.isBlank() ? "no reason provided" : reason
                );
                socket = null;
                ACTIVE_USERS.clear();
                ACTIVE_NAMES.clear();
                scheduleReconnect(generation);
            }
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            if (generation == GENERATION.get()) {
                socket = null;
                ACTIVE_USERS.clear();
                ACTIVE_NAMES.clear();
                LOGGER.warn("Presence socket error: {}", error.getMessage());
                scheduleReconnect(generation);
            }
        }
    }
}
