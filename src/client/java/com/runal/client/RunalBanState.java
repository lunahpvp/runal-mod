package com.runal.client;

import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class RunalBanState {
    private static final Logger LOGGER = LoggerFactory.getLogger("Runal Ban");
    private static final String BAN_STATUS_URL =
            "https://runal-presence.lake-cockroach.workers.dev/ban-status?uuid=";
    private static final long RECHECK_MINUTES = 5L;

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(task, "Runal-Ban-Check");
                thread.setDaemon(true);
                return thread;
            });

    private static volatile boolean banned = false;
    private static volatile String reason = "No reason given";

    private RunalBanState() {
    }

    public static void register() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            check();
            SCHEDULER.scheduleWithFixedDelay(
                    RunalBanState::check, RECHECK_MINUTES, RECHECK_MINUTES, TimeUnit.MINUTES);
        });
    }

    public static boolean isBanned() {
        return banned;
    }

    public static String reason() {
        return reason;
    }

    private static void check() {
        User user = Minecraft.getInstance().getUser();
        if (user == null || user.getProfileId() == null) return;

        String uuid = user.getProfileId().toString().replace("-", "");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BAN_STATUS_URL + uuid))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) return;
                    var json = JsonParser.parseString(response.body()).getAsJsonObject();
                    boolean isBanned = json.has("banned") && json.get("banned").getAsBoolean();
                    if (isBanned && json.has("reason")) {
                        reason = json.get("reason").getAsString();
                    }
                    banned = isBanned;
                })
                .exceptionally(error -> {
                    LOGGER.debug("Ban status check failed: {}", error.getMessage());
                    return null;
                });
    }
}
