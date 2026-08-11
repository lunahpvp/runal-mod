package com.runal.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
//? if 1.21.4 {
//?} else {
import net.minecraft.client.input.MouseButtonEvent;
//?}
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Overhead pan/zoom view of the player's custom waypoints, in the spirit of Wynntils'/
 * Melinoe's full map screen. Melinoe's version layers pre-rendered world tile images
 * (downloaded from a data repo) under the markers; Runal has no such tile source for
 * ScepterRPG/MageRPG yet, so this draws a plain grid instead. The marker/pan/zoom/click
 * layer is otherwise the same idea and can grow a tile background later without changing
 * this screen's interaction model.
 */
public class MapScreen extends Screen {
    private static final double MIN_ZOOM = 0.25;
    private static final double MAX_ZOOM = 8.0;
    private static final int GRID_SPACING = 100;
    private static final int MARKER_SIZE = 6;
    private static final int PANEL_MARGIN = 20;
    private static final int PANEL_TOP = 40;
    private static final int PANEL_BOTTOM_MARGIN = 20;

    private double centerX;
    private double centerZ;
    private double zoom = 2.0;
    private boolean panning;

    public MapScreen() {
        super(Component.literal("World Map"));
    }

    @Override
    protected void init() {
        if (minecraft != null && minecraft.player != null) {
            centerX = minecraft.player.getX();
            centerZ = minecraft.player.getZ();
        }
    }

    //? if 1.21.4 || 1.21.11 {
    /*@Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        renderContent(context, mouseX, mouseY);
        super.render(context, mouseX, mouseY, deltaTicks);
    }
    *///?} else {
    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        renderContent(context, mouseX, mouseY);
        super.extractRenderState(context, mouseX, mouseY, deltaTicks);
    }
    //?}

    private void renderContent(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        context.fillGradient(0, 0, width, height, 0xEE0B0B0F, 0xEE0B0B0F);
        WorldRegions.Region region = currentRegion();
        String title = region.label != null ? "World Map - " + region.label : "World Map";
        context.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
        context.centeredText(font, "Drag to pan  •  Scroll to zoom  •  Click a waypoint to edit it  •  Right-click to recenter", width / 2, 24, 0xFFA7A8B2);

        int panelX = PANEL_MARGIN;
        int panelY = PANEL_TOP;
        int panelW = width - PANEL_MARGIN * 2;
        int panelH = height - PANEL_TOP - PANEL_BOTTOM_MARGIN;
        if (panelW <= 0 || panelH <= 0) return;

        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF15151B);
        context.enableScissor(panelX, panelY, panelX + panelW, panelY + panelH);

        drawGrid(context, panelX, panelY, panelW, panelH);

        List<Waypoint> waypoints = visibleWaypoints();
        Waypoint hovered = findWaypointAt(waypoints, panelX, panelY, panelW, panelH, mouseX, mouseY);
        for (Waypoint waypoint : waypoints) {
            drawWaypoint(context, waypoint, waypoint == hovered, panelX, panelY, panelW, panelH);
        }

        drawPlayerMarker(context, panelX, panelY, panelW, panelH);

        if (waypoints.isEmpty()) {
            String scope = region.label != null ? "in " + region.label + " yet." : "in this dimension yet.";
            context.centeredText(font, "No waypoints " + scope, panelX + panelW / 2, panelY + panelH / 2 - 5, 0xFF8B8D97);
            context.centeredText(font, "Press your New Waypoint keybind to add one.", panelX + panelW / 2, panelY + panelH / 2 + 6, 0xFF8B8D97);
        }

        context.disableScissor();
        context.outline(panelX, panelY, panelW, panelH, 0x33FFFFFF);

        if (hovered != null) drawTooltip(context, hovered, mouseX, mouseY);
    }

    private void drawGrid(GuiGraphicsExtractor context, int panelX, int panelY, int panelW, int panelH) {
        double worldMinX = screenToWorldX(panelX, panelX, panelW);
        double worldMaxX = screenToWorldX(panelX + panelW, panelX, panelW);
        double worldMinZ = screenToWorldZ(panelY, panelY, panelH);
        double worldMaxZ = screenToWorldZ(panelY + panelH, panelY, panelH);

        int startX = (int) (Math.floor(worldMinX / GRID_SPACING) * GRID_SPACING);
        for (int wx = startX; wx <= worldMaxX; wx += GRID_SPACING) {
            int sx = (int) worldToScreenX(wx, panelX, panelW);
            context.fill(sx, panelY, sx + 1, panelY + panelH, 0x22FFFFFF);
        }

        int startZ = (int) (Math.floor(worldMinZ / GRID_SPACING) * GRID_SPACING);
        for (int wz = startZ; wz <= worldMaxZ; wz += GRID_SPACING) {
            int sy = (int) worldToScreenZ(wz, panelY, panelH);
            context.fill(panelX, sy, panelX + panelW, sy + 1, 0x22FFFFFF);
        }
    }

    private void drawWaypoint(GuiGraphicsExtractor context, Waypoint waypoint, boolean hovered, int panelX, int panelY, int panelW, int panelH) {
        int sx = (int) worldToScreenX(waypoint.x, panelX, panelW);
        int sy = (int) worldToScreenZ(waypoint.z, panelY, panelH);
        int half = (hovered ? MARKER_SIZE + 2 : MARKER_SIZE) / 2;

        context.fill(sx - half - 1, sy - half - 1, sx + half + 1, sy + half + 1, 0xFF000000);
        context.fill(sx - half, sy - half, sx + half, sy + half, waypoint.color());

        if (hovered || zoom >= 1.5) {
            int labelWidth = font.width(waypoint.name);
            context.text(font, waypoint.name, sx - labelWidth / 2, sy + half + 3, 0xFFFFFFFF, true);
        }
    }

    private void drawPlayerMarker(GuiGraphicsExtractor context, int panelX, int panelY, int panelW, int panelH) {
        if (minecraft == null || minecraft.player == null) return;
        int sx = (int) worldToScreenX(minecraft.player.getX(), panelX, panelW);
        int sy = (int) worldToScreenZ(minecraft.player.getZ(), panelY, panelH);
        int size = 5;

        context.fill(sx - 1, sy - size - 2, sx + 1, sy + size + 2, 0xFF35D77A);
        context.fill(sx - size - 2, sy - 1, sx + size + 2, sy + 1, 0xFF35D77A);
    }

    private void drawTooltip(GuiGraphicsExtractor context, Waypoint waypoint, int mouseX, int mouseY) {
        String coords = waypoint.x + ", " + waypoint.y + ", " + waypoint.z;
        int lineWidth = Math.max(font.width(waypoint.name), font.width(coords));
        int boxW = lineWidth + 10;
        int boxH = 26;
        int x = Math.min(mouseX + 12, width - boxW - 4);
        int y = Math.min(mouseY + 12, height - boxH - 4);

        context.fill(x, y, x + boxW, y + boxH, 0xEE101216);
        context.outline(x, y, boxW, boxH, waypoint.color());
        context.text(font, waypoint.name, x + 5, y + 4, 0xFFFFFFFF, true);
        context.text(font, coords, x + 5, y + 15, 0xFFA7A8B2, true);
    }

    private Waypoint findWaypointAt(List<Waypoint> waypoints, int panelX, int panelY, int panelW, int panelH, int mouseX, int mouseY) {
        if (mouseX < panelX || mouseX > panelX + panelW || mouseY < panelY || mouseY > panelY + panelH) return null;
        Waypoint closest = null;
        int closestDistSq = Integer.MAX_VALUE;
        for (Waypoint waypoint : waypoints) {
            int sx = (int) worldToScreenX(waypoint.x, panelX, panelW);
            int sy = (int) worldToScreenZ(waypoint.z, panelY, panelH);
            int dx = mouseX - sx;
            int dy = mouseY - sy;
            int distSq = dx * dx + dy * dy;
            if (distSq <= 64 && distSq < closestDistSq) {
                closest = waypoint;
                closestDistSq = distSq;
            }
        }
        return closest;
    }

    private List<Waypoint> visibleWaypoints() {
        if (minecraft == null) return List.of();
        String dimensionKey = WaypointManagerState.currentDimensionKey(minecraft);
        WorldRegions.Region region = currentRegion();
        return WaypointManagerState.INSTANCE.getWaypoints().stream()
                .filter(w -> w.enabled && w.isEnabledInDimension(dimensionKey))
                .filter(w -> region == WorldRegions.Region.UNKNOWN || WorldRegions.regionOf(w.x, w.y, w.z) == region)
                .toList();
    }

    private WorldRegions.Region currentRegion() {
        if (minecraft == null || minecraft.player == null || !WorldRegions.isMageRpg()) return WorldRegions.Region.UNKNOWN;
        return WorldRegions.regionOf(minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ());
    }

    private double worldToScreenX(double worldX, int panelX, int panelW) {
        return panelX + panelW / 2.0 + (worldX - centerX) * zoom;
    }

    private double worldToScreenZ(double worldZ, int panelY, int panelH) {
        return panelY + panelH / 2.0 + (worldZ - centerZ) * zoom;
    }

    private double screenToWorldX(double screenX, int panelX, int panelW) {
        return centerX + (screenX - panelX - panelW / 2.0) / zoom;
    }

    private double screenToWorldZ(double screenY, int panelY, int panelH) {
        return centerZ + (screenY - panelY - panelH / 2.0) / zoom;
    }

    private boolean insidePanel(double mouseX, double mouseY) {
        int panelX = PANEL_MARGIN;
        int panelY = PANEL_TOP;
        int panelW = width - PANEL_MARGIN * 2;
        int panelH = height - PANEL_TOP - PANEL_BOTTOM_MARGIN;
        return mouseX >= panelX && mouseX <= panelX + panelW && mouseY >= panelY && mouseY <= panelY + panelH;
    }

    //? if 1.21.4 {
    /*@Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (handleMouseClicked((int) mouseX, (int) mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }
    *///?} else {
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (handleMouseClicked((int) event.x(), (int) event.y(), event.button())) return true;
        return super.mouseClicked(event, doubled);
    }
    //?}

    private boolean handleMouseClicked(int mouseX, int mouseY, int button) {
        if (!insidePanel(mouseX, mouseY)) return false;

        if (button == 1) {
            if (minecraft != null && minecraft.player != null) {
                centerX = minecraft.player.getX();
                centerZ = minecraft.player.getZ();
            }
            return true;
        }

        if (button == 0) {
            int panelX = PANEL_MARGIN;
            int panelY = PANEL_TOP;
            int panelW = width - PANEL_MARGIN * 2;
            int panelH = height - PANEL_TOP - PANEL_BOTTOM_MARGIN;
            Waypoint hovered = findWaypointAt(visibleWaypoints(), panelX, panelY, panelW, panelH, mouseX, mouseY);
            if (hovered != null) {
                if (minecraft != null) minecraft.setScreen(new WaypointEditScreen(this, hovered));
                return true;
            }
            panning = true;
            return true;
        }
        return false;
    }

    //? if 1.21.4 {
    /*@Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (handleMouseDragged(dragX, dragY)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    *///?} else {
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (handleMouseDragged(dragX, dragY)) return true;
        return super.mouseDragged(event, dragX, dragY);
    }
    //?}

    private boolean handleMouseDragged(double dragX, double dragY) {
        if (!panning) return false;
        centerX -= dragX / zoom;
        centerZ -= dragY / zoom;
        return true;
    }

    //? if 1.21.4 {
    /*@Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        panning = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }
    *///?} else {
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        panning = false;
        return super.mouseReleased(event);
    }
    //?}

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double oldZoom = zoom;
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * Math.pow(1.15, scrollY)));
        if (zoom != oldZoom && insidePanel(mouseX, mouseY)) {
            int panelX = PANEL_MARGIN;
            int panelY = PANEL_TOP;
            int panelW = width - PANEL_MARGIN * 2;
            int panelH = height - PANEL_TOP - PANEL_BOTTOM_MARGIN;
            centerX += (mouseX - panelX - panelW / 2.0) * (1.0 / oldZoom - 1.0 / zoom);
            centerZ += (mouseY - panelY - panelH / 2.0) * (1.0 / oldZoom - 1.0 / zoom);
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
