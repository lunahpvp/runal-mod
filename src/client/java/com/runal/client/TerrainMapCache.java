package com.runal.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.Heightmap;
//? if 1.21.4 {
//?} else {
import java.util.function.Supplier;
//?}

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Live terrain color sampler for the World Map, the same technique JourneyMap/Xaero's
 * Minimap use - Runal has no pre-rendered tile art like Melinoe (that needs someone to
 * have rendered the world into images ahead of time, which nobody has for MageRPG/
 * ScepterRPG), so instead this samples the topmost block's real vanilla map color per
 * column as chunks load near the player, painting it into per-tile textures. Explored
 * terrain accumulates as you walk around, and is saved to disk per-server so it stays
 * filled in across sessions instead of resetting every relaunch.
 */
public final class TerrainMapCache {
    private static final int TILE_CHUNKS = 8;
    private static final int TILE_SIZE = TILE_CHUNKS * 16;
    private static final int SCAN_RADIUS_CHUNKS = 12;
    private static final int SAMPLE_BUDGET_PER_TICK = 16;
    private static final long SAVE_INTERVAL_TICKS = 600L;

    private static final Map<Long, Tile> TILES = new HashMap<>();
    private static final Set<Long> SAMPLED_CHUNKS = new HashSet<>();
    private static long nextTextureId;
    private static long tickCounter;
    private static String currentServer = "ScepterRPG";

    private TerrainMapCache() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(TerrainMapCache::onTick);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> onJoin());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> onDisconnect());
    }

    public static List<TileInfo> allTiles() {
        List<TileInfo> result = new ArrayList<>(TILES.size());
        for (Tile tile : TILES.values()) {
            result.add(new TileInfo(tile.id, tile.originChunkX * 16, tile.originChunkZ * 16, TILE_SIZE));
        }
        return result;
    }

    private static void onJoin() {
        currentServer = DiscordPresenceController.detectedServer();
    }

    private static void onDisconnect() {
        flushDirty();
        Minecraft mc = Minecraft.getInstance();
        for (Tile tile : TILES.values()) {
            mc.getTextureManager().release(tile.id);
        }
        TILES.clear();
        SAMPLED_CHUNKS.clear();
    }

    private static void onTick(Minecraft mc) {
        if (mc.level == null || mc.player == null) return;
        currentServer = DiscordPresenceController.detectedServer();

        int centerChunkX = Math.floorDiv((int) Math.floor(mc.player.getX()), 16);
        int centerChunkZ = Math.floorDiv((int) Math.floor(mc.player.getZ()), 16);

        int sampled = 0;
        outer:
        for (int dx = -SCAN_RADIUS_CHUNKS; dx <= SCAN_RADIUS_CHUNKS; dx++) {
            for (int dz = -SCAN_RADIUS_CHUNKS; dz <= SCAN_RADIUS_CHUNKS; dz++) {
                int cx = centerChunkX + dx;
                int cz = centerChunkZ + dz;
                long key = chunkKey(cx, cz);
                if (SAMPLED_CHUNKS.contains(key)) continue;
                if (!mc.level.hasChunk(cx, cz)) continue;

                sampleChunk(mc, cx, cz);
                SAMPLED_CHUNKS.add(key);
                sampled++;
                if (sampled >= SAMPLE_BUDGET_PER_TICK) break outer;
            }
        }

        tickCounter++;
        if (tickCounter % SAVE_INTERVAL_TICKS == 0) flushDirty();
    }

    private static void sampleChunk(Minecraft mc, int chunkX, int chunkZ) {
        Tile tile = tileFor(chunkX, chunkZ);
        int tileOriginX = tile.originChunkX * 16;
        int tileOriginZ = tile.originChunkZ * 16;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int worldX = chunkX * 16 + lx;
                int worldZ = chunkZ * 16 + lz;
                int height = mc.level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ);
                int sampleY = Math.max(mc.level.getMinY(), height - 1);
                pos.set(worldX, sampleY, worldZ);
                var mapColor = mc.level.getBlockState(pos).getMapColor(mc.level, pos);
                int color = 0xFF000000 | mapColor.col;

                int px = worldX - tileOriginX;
                int pz = worldZ - tileOriginZ;
                tile.image.setPixel(px, pz, color);
            }
        }
        tile.dirty = true;
        tile.texture.upload();
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    private static Tile tileFor(int chunkX, int chunkZ) {
        int originChunkX = Math.floorDiv(chunkX, TILE_CHUNKS) * TILE_CHUNKS;
        int originChunkZ = Math.floorDiv(chunkZ, TILE_CHUNKS) * TILE_CHUNKS;
        long key = chunkKey(originChunkX, originChunkZ);

        Tile existing = TILES.get(key);
        if (existing != null) return existing;

        NativeImage image = loadFromDisk(originChunkX, originChunkZ);
        boolean loaded = image != null;
        if (image == null) {
            image = new NativeImage(TILE_SIZE, TILE_SIZE, true);
            for (int x = 0; x < TILE_SIZE; x++) {
                for (int z = 0; z < TILE_SIZE; z++) {
                    image.setPixel(x, z, 0);
                }
            }
        }

        String name = "tile_" + originChunkX + "_" + originChunkZ + "_" + (nextTextureId++);
        Identifier id = Identifier.fromNamespaceAndPath("runal", "terrain_map/" + name);
        //? if 1.21.4 {
        /*DynamicTexture texture = new DynamicTexture(image);
        *///?} else {
        Supplier<String> label = () -> "Runal terrain tile " + name;
        DynamicTexture texture = new DynamicTexture(label, image);
        //?}
        //? if 1.21.4 {
        /*texture.setFilter(true, false);
        *///?}
        Minecraft.getInstance().getTextureManager().register(id, texture);
        texture.upload();

        Tile tile = new Tile(image, id, texture, originChunkX, originChunkZ);
        tile.dirty = !loaded;
        TILES.put(key, tile);
        return tile;
    }

    private static void flushDirty() {
        for (Tile tile : TILES.values()) {
            if (!tile.dirty) continue;
            if (saveToDisk(tile)) tile.dirty = false;
        }
    }

    private static Path cacheDir() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("runal").resolve("map_cache").resolve(currentServer);
    }

    private static Path tilePath(int originChunkX, int originChunkZ) {
        return cacheDir().resolve("tile_" + originChunkX + "_" + originChunkZ + ".png");
    }

    private static NativeImage loadFromDisk(int originChunkX, int originChunkZ) {
        Path path = tilePath(originChunkX, originChunkZ);
        if (!Files.isRegularFile(path)) return null;
        try {
            NativeImage image = NativeImage.read(Files.newInputStream(path));
            if (image.getWidth() != TILE_SIZE || image.getHeight() != TILE_SIZE) {
                image.close();
                return null;
            }
            return image;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static boolean saveToDisk(Tile tile) {
        try {
            Path dir = cacheDir();
            Files.createDirectories(dir);
            tile.image.writeToFile(tilePath(tile.originChunkX, tile.originChunkZ));
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    public record TileInfo(Identifier id, int worldX, int worldZ, int size) {
    }

    private static final class Tile {
        final NativeImage image;
        final Identifier id;
        final DynamicTexture texture;
        final int originChunkX;
        final int originChunkZ;
        boolean dirty;

        Tile(NativeImage image, Identifier id, DynamicTexture texture, int originChunkX, int originChunkZ) {
            this.image = image;
            this.id = id;
            this.texture = texture;
            this.originChunkX = originChunkX;
            this.originChunkZ = originChunkZ;
        }
    }
}
