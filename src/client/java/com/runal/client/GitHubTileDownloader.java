package com.runal.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Downloads map tiles other players (or a past you) have already explored and shared, so a
 * fresh install doesn't start from a blank map - the same idea as Melinoe's tile
 * downloader, except the source is Runal's own GitHub repo rather than a dedicated data
 * repo, since Runal generates its tiles by exploring (TerrainMapCache) instead of needing
 * someone with server/world access to pre-render them.
 *
 * To make tiles available: drop the PNGs plus a manifest.json (a JSON array of their
 * filenames, e.g. ["tile_0_0.png", "tile_8_0.png"]) into map_tiles/<server>/ in the
 * runal-mod repo. A missing manifest just means "nobody's shared tiles for this server
 * yet" - not an error - so this is safe to ship before that folder exists.
 */
public final class GitHubTileDownloader {
    private static final String BASE_URL = "https://raw.githubusercontent.com/lunahpvp/runal-mod/main/map_tiles/";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private GitHubTileDownloader() {
    }

    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                CompletableFuture.runAsync(GitHubTileDownloader::syncTiles));
    }

    private static void syncTiles() {
        String server = DiscordPresenceController.detectedServer();
        JsonArray manifest = fetchManifest(server);
        if (manifest == null || manifest.isEmpty()) return;

        Path dir = TerrainMapCache.cacheDir(server);
        boolean downloadedAny = false;
        for (var element : manifest) {
            String fileName = element.getAsString();
            if (!fileName.matches("tile_-?\\d+_-?\\d+\\.png")) continue;

            Path localPath = dir.resolve(fileName);
            if (Files.isRegularFile(localPath)) continue;
            if (downloadTile(server, fileName, localPath)) downloadedAny = true;
        }

        if (downloadedAny) {
            Minecraft.getInstance().execute(TerrainMapCache::loadAllCachedTiles);
        }
    }

    private static JsonArray fetchManifest(String server) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + server + "/manifest.json"))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;
            return JsonParser.parseString(response.body()).getAsJsonArray();
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean downloadTile(String server, String fileName, Path localPath) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + server + "/" + fileName))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) return false;

            Files.createDirectories(localPath.getParent());
            Path tmp = localPath.resolveSibling(localPath.getFileName() + ".tmp");
            Files.write(tmp, response.body());
            Files.move(tmp, localPath, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
