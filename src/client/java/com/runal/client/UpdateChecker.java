package com.runal.client;

import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class UpdateChecker {
    private static final String LATEST_VERSION_URL = "https://runal-presence.lake-cockroach.workers.dev/latest-version";
    private static final String RELEASES_URL = "https://www.curseforge.com/minecraft/mc-mods/runal";
    private static final Logger LOGGER = LoggerFactory.getLogger("RunalUpdateChecker");

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                CompletableFuture.runAsync(UpdateChecker::checkForUpdate)
                        .exceptionally(throwable -> {
                            LOGGER.error("[UpdateChecker] check crashed", throwable);
                            return null;
                        }));
    }

    private static void checkForUpdate() {
        String current = currentVersion();
        if (current == null) return;

        String latest = fetchLatestVersion();
        if (latest == null || !isNewer(latest, current)) return;

        Minecraft.getInstance().execute(() -> notifyUpdate(current, latest));
    }

    private static String currentVersion() {
        return FabricLoader.getInstance().getModContainer("runal")
                .map(container -> container.getMetadata().getVersion().getFriendlyString().split("\\+")[0])
                .orElse(null);
    }

    private static String fetchLatestVersion() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(LATEST_VERSION_URL))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;

            var json = JsonParser.parseString(response.body()).getAsJsonObject();
            return json.has("version") ? json.get("version").getAsString() : null;
        } catch (Exception e) {
            LOGGER.error("[UpdateChecker] fetch failed", e);
            return null;
        }
    }

    private static boolean isNewer(String latest, String current) {
        int[] latestParts = parseVersion(latest);
        int[] currentParts = parseVersion(current);
        if (latestParts == null || currentParts == null) return false;

        for (int i = 0; i < 3; i++) {
            if (latestParts[i] != currentParts[i]) return latestParts[i] > currentParts[i];
        }
        return false;
    }

    private static int[] parseVersion(String version) {
        try {
            // Loom's friendly string for the local mod is "1.1.0+26.1.2", the version plus
            // the MC qualifier. This strips everything from "+" on before splitting.
            String core = version.trim().split("\\+")[0];
            String[] parts = core.split("\\.");
            int[] result = new int[3];
            for (int i = 0; i < 3; i++) result[i] = i < parts.length ? Integer.parseInt(parts[i]) : 0;
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    private static void notifyUpdate(String currentVersion, String latestVersion) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        //? if 26.2 {
        /*mc.gui.hud.setTimes(0, 60, 10);
        mc.gui.hud.setTitle(Message.gradientText("Runal Update Available"));
        *///?} else {
        mc.gui.setTimes(0, 60, 10);
        mc.gui.setTitle(Message.gradientText("Runal Update Available"));
        //?}
        mc.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0f, 1.0f);

        Message.sendRaw(buildUpdateMessage(currentVersion, latestVersion));
    }

    private static MutableComponent styled(String text, int color) {
        return Component.literal(text).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));
    }

    private static MutableComponent buildUpdateMessage(String currentVersion, String latestVersion) {
        int accent = 0x7CFFB2;
        int dim = 0xAAAAAA;

        // Every piece below carries its own explicit style rather than relying on inheriting
        // one from a shared parent. A styled separator() as the root once bled its
        // strikethrough into every sibling appended after it.
        MutableComponent message = Component.empty();
        message.append(separator())
                .append("\n")
                .append(Component.literal("UPDATE AVAILABLE").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(accent)).withBold(true)))
                .append("\n")
                .append(Message.gradientText("Runal"))
                .append(styled(" grows stronger, a new version awaits.", dim))
                .append("\n")
                .append(styled("Current version: ", dim))
                .append(styled("v" + currentVersion, accent))
                .append(styled("  ->  ", dim))
                .append(styled("v" + latestVersion, accent))
                .append("\n")
                .append(styled("Download: ", dim))
                .append(downloadLink())
                .append("\n")
                .append(separator());
        return message;
    }

    private static MutableComponent separator() {
        return Component.literal(" ".repeat(40))
                .withStyle(Style.EMPTY.withStrikethrough(true).withColor(TextColor.fromRgb(0x606060)));
    }

    private static MutableComponent downloadLink() {
        Style style = Style.EMPTY.withColor(TextColor.fromRgb(0x7CFFB2)).withUnderlined(true);
        //? if 1.21.4 {
        /*style = style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, RELEASES_URL));
        *///?} else {
        style = style.withClickEvent(new ClickEvent.OpenUrl(URI.create(RELEASES_URL)));
        //?}
        return Component.literal("Click here to download!").withStyle(style);
    }
}
