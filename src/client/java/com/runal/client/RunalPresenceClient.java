package com.runal.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    private static final String SESSION_SERVER_URL =
            "https://sessionserver.mojang.com/session/minecraft/hasJoined";
    private static final long RECONNECT_DELAY_SECONDS = 5L;
    private static final long CLAIM_REFRESH_SECONDS = 15L;
    private static final int BANNED_CLOSE_CODE = 4403;

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(task, "Runal-Presence-Reconnect");
                thread.setDaemon(true);
                return thread;
            });
    private static volatile Set<String> activeUsers = Set.of();
    private static volatile Set<String> activeNames = Set.of();
    private static final Set<String> VERIFIED_CLAIMS = ConcurrentHashMap.newKeySet();
    private static final Set<String> CURRENT_CLAIMS = ConcurrentHashMap.newKeySet();
    private static final AtomicBoolean CONNECTING = new AtomicBoolean(false);
    private static final AtomicInteger GENERATION = new AtomicInteger();
    private static final AtomicInteger SYNC_GENERATION = new AtomicInteger();

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

        // Lets "# hey guys" typed straight into normal chat go to Runal Chat instead of the
        // server, without needing the full /rc command.
        ClientSendMessageEvents.ALLOW_CHAT.register(rawMessage -> {
            if (!rawMessage.startsWith("#")) return true;

            String text = rawMessage.substring(1).trim();
            if (!text.isEmpty() && !sendChat(text)) {
                Message.commandError("Runal Chat is unavailable right now.");
            }
            return false;
        });
    }

    public static boolean sendChat(String text) {
        WebSocket current = socket;
        if (current == null) return false;

        JsonObject message = new JsonObject();
        message.addProperty("action", "chat");
        message.addProperty("text", text);
        message.addProperty("server", DiscordPresenceController.detectedServer());
        current.sendText(message.toString(), true);
        return true;
    }

    public static boolean sendMute(String target, int seconds) {
        WebSocket current = socket;
        if (current == null) return false;

        JsonObject message = new JsonObject();
        message.addProperty("action", "mute");
        message.addProperty("target", target);
        message.addProperty("seconds", seconds);
        current.sendText(message.toString(), true);
        return true;
    }

    public static boolean sendUnmute(String target) {
        WebSocket current = socket;
        if (current == null) return false;

        JsonObject message = new JsonObject();
        message.addProperty("action", "unmute");
        message.addProperty("target", target);
        current.sendText(message.toString(), true);
        return true;
    }

    public static boolean isActiveUser(UUID uuid, String name) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            if (uuid != null && uuid.equals(client.player.getUUID())) return true;
            if (name != null && normalizeName(name).equals(
                    normalizeName(client.player.getGameProfile().name())
            )) return true;
        }
        return (uuid != null && activeUsers.contains(normalizeUuid(uuid.toString())))
                || (name != null && activeNames.contains(normalizeName(name)));
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
        activeUsers = Set.of();
        activeNames = Set.of();
        CURRENT_CLAIMS.clear();
        SYNC_GENERATION.incrementAndGet();

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
                LOGGER.info("Registered with Runal presence");
                scheduleClaimRefresh(webSocket);
                return;
            }
            if ("sync".equals(action)) {
                verifySnapshot(message.getAsJsonArray("users"));
                return;
            }
            if ("chat".equals(action)) {
                String server = message.has("server") ? message.get("server").getAsString() : "unknown";
                handleChatReceived(server, message.get("name").getAsString(), message.get("text").getAsString());
                return;
            }
            if ("chat_error".equals(action)) {
                handleChatError(message.get("reason").getAsString());
                return;
            }
            if ("mute_ack".equals(action)) {
                handleMuteAck(message.get("target").getAsString(), message.get("muting").getAsBoolean());
            }
        } catch (Exception error) {
            LOGGER.debug("Ignored invalid presence message: {}", error.getMessage());
        }
    }

    private static void handleChatReceived(String server, String name, String text) {
        if (!RunalChatState.enabled) return;
        Minecraft.getInstance().execute(() -> {
            var line = Message.colored("[RC] ", 0x55FFAA)
                    .append(Message.colored("(" + server + ") ", 0x777777))
                    .append(Message.colored(name, 0xFFFFFF))
                    .append(Message.colored(" » ", 0xAAAAAA))
                    .append(Message.colored(text, 0xFFFFFF));
            Message.sendRaw(line);
        });
    }

    private static void handleChatError(String reason) {
        Minecraft.getInstance().execute(() -> {
            if ("muted".equals(reason)) {
                Message.commandError("You are currently muted from Runal Chat.");
            } else if ("not_admin".equals(reason)) {
                Message.commandError("Only the admin can mute/unmute players.");
            }
        });
    }

    private static void handleMuteAck(String target, boolean muting) {
        Minecraft.getInstance().execute(() ->
                Message.commandSuccess((muting ? "Muted " : "Unmuted ") + target + " from Runal Chat."));
    }

    private static void scheduleClaimRefresh(WebSocket webSocket) {
        int generation = GENERATION.get();
        SCHEDULER.schedule(() -> {
            if (!connectionWanted
                    || generation != GENERATION.get()
                    || socket != webSocket) {
                return;
            }
            webSocket.sendText("{\"action\":\"refresh\"}", true);
        }, CLAIM_REFRESH_SECONDS, TimeUnit.SECONDS);
    }

    private static void verifySnapshot(JsonArray users) {
        int syncGeneration = SYNC_GENERATION.incrementAndGet();
        List<PresenceClaim> claims = new ArrayList<>();
        Set<String> presentKeys = ConcurrentHashMap.newKeySet();

        for (JsonElement userElement : users) {
            try {
                JsonObject user = userElement.getAsJsonObject();
                String name = user.get("name").getAsString();
                String uuid = normalizeUuid(user.get("uuid").getAsString());
                String serverId = user.get("serverId").getAsString();
                if (!name.matches("[A-Za-z0-9_]{1,16}")
                        || !uuid.matches("[0-9a-f]{32}")
                        || !serverId.matches("[0-9a-f]{40}")) {
                    continue;
                }
                PresenceClaim claim = new PresenceClaim(uuid, name, serverId);
                claims.add(claim);
                presentKeys.add(claim.key());
            } catch (Exception ignored) {
                // Ignore malformed claims without dropping the rest of the snapshot.
            }
        }

        CURRENT_CLAIMS.clear();
        CURRENT_CLAIMS.addAll(presentKeys);
        VERIFIED_CLAIMS.retainAll(presentKeys);

        List<CompletableFuture<PresenceClaim>> checks = new ArrayList<>();
        for (PresenceClaim claim : claims) {
            if (VERIFIED_CLAIMS.contains(claim.key())) {
                checks.add(CompletableFuture.completedFuture(claim));
                continue;
            }
            checks.add(verifyClaim(claim.name(), claim.uuid(), claim.serverId())
                    .thenApply(valid -> {
                        if (!valid) return null;
                        if (CURRENT_CLAIMS.contains(claim.key())) {
                            VERIFIED_CLAIMS.add(claim.key());
                        }
                        return claim;
                    }));
        }

        CompletableFuture.allOf(checks.toArray(CompletableFuture[]::new))
                .thenRun(() -> {
                    if (syncGeneration != SYNC_GENERATION.get()) return;

                    Set<String> newUsers = new HashSet<>();
                    Set<String> newNames = new HashSet<>();
                    for (CompletableFuture<PresenceClaim> check : checks) {
                        PresenceClaim claim = check.join();
                        if (claim == null) continue;
                        newUsers.add(claim.uuid());
                        newNames.add(normalizeName(claim.name()));
                    }
                    // Swap the reference instead of mutating the existing sets in place -
                    // isActiveUser() reads these from the render thread with no synchronization,
                    // so a clear()-then-repopulate sequence would be visible mid-update as an
                    // empty set, making every other player's badge flicker off on each sync.
                    activeUsers = Set.copyOf(newUsers);
                    activeNames = Set.copyOf(newNames);
                    LOGGER.info("Runal presence verified {} user(s)", newUsers.size());
                });
    }

    private static CompletableFuture<Boolean> verifyClaim(
            String name,
            String expectedUuid,
            String serverId
    ) {
        URI uri = URI.create(SESSION_SERVER_URL
                + "?username=" + name
                + "&serverId=" + serverId);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(5))
                .header("Accept", "application/json")
                .header("User-Agent", "Runal/1.2")
                .GET()
                .build();
        return verifyClaim(request, name, expectedUuid, 0);
    }

    private static CompletableFuture<Boolean> verifyClaim(
            HttpRequest request,
            String name,
            String expectedUuid,
            int attempt
    ) {
        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .handle((response, error) -> {
                    if (error != null || response.statusCode() != 200 || response.body().isBlank()) {
                        return false;
                    }
                    try {
                        JsonObject profile = JsonParser.parseString(response.body()).getAsJsonObject();
                        return expectedUuid.equals(normalizeUuid(profile.get("id").getAsString()))
                                && name.equalsIgnoreCase(profile.get("name").getAsString());
                    } catch (Exception ignored) {
                        return false;
                    }
                })
                .thenCompose(valid -> {
                    if (valid || attempt >= 4) return CompletableFuture.completedFuture(valid);
                    return CompletableFuture.supplyAsync(
                                    () -> null,
                                    CompletableFuture.delayedExecutor(300, TimeUnit.MILLISECONDS)
                            )
                            .thenCompose(ignored -> verifyClaim(
                                    request,
                                    name,
                                    expectedUuid,
                                    attempt + 1
                            ));
                });
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

    private record PresenceClaim(String uuid, String name, String serverId) {
        private String key() {
            return uuid + ":" + serverId;
        }
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
                activeUsers = Set.of();
                activeNames = Set.of();
                if (statusCode == BANNED_CLOSE_CODE) {
                    Minecraft.getInstance().execute(() ->
                            Message.commandError("You have been banned from Runal's online features."));
                } else {
                    scheduleReconnect(generation);
                }
            }
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            if (generation == GENERATION.get()) {
                socket = null;
                activeUsers = Set.of();
                activeNames = Set.of();
                LOGGER.warn("Presence socket error: {}", error.getMessage());
                scheduleReconnect(generation);
            }
        }
    }
}
