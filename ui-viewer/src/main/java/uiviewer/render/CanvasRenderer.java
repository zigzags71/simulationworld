package uiviewer.render;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import simcore.snapshot.RenderSnapshot;

import java.nio.IntBuffer;

public class CanvasRenderer {
    private static final PixelFormat<IntBuffer> ARGB_PRE_FORMAT = PixelFormat.getIntArgbPreInstance();
    private static final int AGENT_GREEN = 0xFF00FF00;
    private static final int SELECTED_AGENT = 0xFFFFFF66;
    private static final int CULTURE_BASE = encodeColor(0.15, 0.15, 0.15, 1.0);

    private final Canvas canvas;
    private WritableImage viewportImage;
    private int[] argbBuffer;
    private int bufferWidth;
    private int bufferHeight;

    public CanvasRenderer(Canvas canvas) {
        this.canvas = canvas;
    }

    public void render(RenderSnapshot snapshot, OverlayMode mode, Camera camera, boolean showAgents, SelectionState selectionState) {
        if (snapshot == null) {
            return;
        }
        double canvasWidth = canvas.getWidth();
        double canvasHeight = canvas.getHeight();
        camera.setViewport(canvasWidth, canvasHeight);

        int minX = camera.getVisibleMinX(canvasWidth);
        int maxX = camera.getVisibleMaxX(canvasWidth);
        int minY = camera.getVisibleMinY(canvasHeight);
        int maxY = camera.getVisibleMaxY(canvasHeight);
        int viewWidth = maxX - minX + 1;
        int viewHeight = maxY - minY + 1;
        if (viewWidth <= 0 || viewHeight <= 0) {
            return;
        }

        ensureBuffer(viewWidth, viewHeight);
        double overlayAlpha = showAgents ? 0.35 : 1.0;
        fillBase(snapshot, mode, minX, minY, viewWidth, viewHeight, overlayAlpha);
        if (showAgents) {
            overlayAgents(snapshot, mode, selectionState, minX, minY, viewWidth, viewHeight);
        }
        uploadBuffer(viewWidth, viewHeight);

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(showAgents ? Color.rgb(8, 8, 8) : Color.BLACK);
        gc.fillRect(0, 0, canvasWidth, canvasHeight);

        double destX = Math.floor(camera.worldToScreenX(minX));
        double destY = Math.floor(camera.worldToScreenY(minY));
        double destW = viewWidth * camera.getZoom();
        double destH = viewHeight * camera.getZoom();
        gc.drawImage(viewportImage, destX, destY, destW, destH);

        drawSelection(gc, camera, selectionState);
    }

    private void ensureBuffer(int viewWidth, int viewHeight) {
        if (viewportImage == null || viewWidth != bufferWidth || viewHeight != bufferHeight) {
            bufferWidth = viewWidth;
            bufferHeight = viewHeight;
            argbBuffer = new int[viewWidth * viewHeight];
            viewportImage = new WritableImage(viewWidth, viewHeight);
        }
    }

    private void fillBase(RenderSnapshot snapshot, OverlayMode mode, int minX, int minY, int viewWidth, int viewHeight, double overlayAlpha) {
        if (mode == OverlayMode.CULTURE) {
            int base = applyAlpha(CULTURE_BASE, overlayAlpha);
            for (int i = 0; i < viewWidth * viewHeight; i++) {
                argbBuffer[i] = base;
            }
            return;
        }

        float[] overlay = selectOverlay(snapshot, mode);
        int worldWidth = snapshot.getWidth();
        int bufferIndex = 0;
        for (int y = 0; y < viewHeight; y++) {
            int worldY = minY + y;
            int rowStart = worldY * worldWidth + minX;
            for (int x = 0; x < viewWidth; x++) {
                float value = overlay[rowStart + x];
                argbBuffer[bufferIndex++] = encodeOverlayColor(value, mode, overlayAlpha);
            }
        }
    }

    private void overlayAgents(RenderSnapshot snapshot, OverlayMode mode, SelectionState selectionState, int minX, int minY, int viewWidth, int viewHeight) {
        int[] xs = snapshot.getAgentX();
        int[] ys = snapshot.getAgentY();
        int[] colors = snapshot.getAgentColorARGB();
        long[] ids = snapshot.getAgentId();
        int count = snapshot.getAgentCount();
        long selectedId = selectionState != null ? selectionState.getSelectedAgentId() : -1;

        for (int i = 0; i < count; i++) {
            int worldX = xs[i];
            int worldY = ys[i];
            if (worldX < minX || worldX > minX + viewWidth - 1 || worldY < minY || worldY > minY + viewHeight - 1) {
                continue;
            }
            int bufferIndex = (worldY - minY) * viewWidth + (worldX - minX);
            boolean selected = ids[i] == selectedId;
            if (mode == OverlayMode.CULTURE) {
                argbBuffer[bufferIndex] = applyAlpha(colors[i] | 0xFF000000, 1.0);
            } else {
                argbBuffer[bufferIndex] = selected ? SELECTED_AGENT : AGENT_GREEN;
            }
        }
    }

    private void uploadBuffer(int viewWidth, int viewHeight) {
        PixelWriter writer = viewportImage.getPixelWriter();
        writer.setPixels(0, 0, viewWidth, viewHeight, ARGB_PRE_FORMAT, argbBuffer, 0, viewWidth);
    }

    private void drawSelection(GraphicsContext gc, Camera camera, SelectionState selectionState) {
        if (selectionState == null) {
            return;
        }
        double tileSize = camera.getZoom();
        if (selectionState.hasRegion()) {
            gc.setStroke(Color.YELLOW);
            gc.setLineWidth(1.5);
            double startX = camera.worldToScreenX(Math.min(selectionState.getRegionStartX(), selectionState.getRegionEndX()));
            double startY = camera.worldToScreenY(Math.min(selectionState.getRegionStartY(), selectionState.getRegionEndY()));
            double width = (Math.abs(selectionState.getRegionEndX() - selectionState.getRegionStartX()) + 1) * tileSize;
            double height = (Math.abs(selectionState.getRegionEndY() - selectionState.getRegionStartY()) + 1) * tileSize;
            gc.setFill(new Color(1, 1, 0, 0.18));
            gc.fillRect(startX, startY, width, height);
            gc.strokeRect(startX, startY, width, height);
        }

        if (selectionState.getSelectedTileX() >= 0) {
            gc.setStroke(Color.YELLOW);
            gc.setLineWidth(1.5);
            double sx = camera.worldToScreenX(selectionState.getSelectedTileX());
            double sy = camera.worldToScreenY(selectionState.getSelectedTileY());
            gc.strokeRect(sx, sy, tileSize, tileSize);
        }
    }

    private float[] selectOverlay(RenderSnapshot snapshot, OverlayMode mode) {
        return switch (mode) {
            case FOOD -> snapshot.getFood();
            case HAZARD -> snapshot.getHazard();
            case CROWDING, CULTURE -> snapshot.getCrowding();
        };
    }

    private int encodeOverlayColor(float value, OverlayMode mode, double alpha) {
        double clamped = Math.max(0.0, Math.min(1.0, value));
        return switch (mode) {
            case FOOD -> encodeColor(0.2 + clamped * 0.8, 0.5 + clamped * 0.5, 0.2, alpha);
            case HAZARD -> encodeColor(0.4 + clamped * 0.6, 0.2, 0.2 + clamped * 0.8, alpha);
            case CROWDING, CULTURE -> encodeColor(0.2, 0.2 + clamped * 0.8, 0.8, alpha);
        };
    }

    private static int encodeColor(double r, double g, double b, double alpha) {
        int a = (int) Math.round(Math.max(0, Math.min(1.0, alpha)) * 255.0);
        int pr = (int) Math.round(Math.max(0, Math.min(1.0, r)) * a);
        int pg = (int) Math.round(Math.max(0, Math.min(1.0, g)) * a);
        int pb = (int) Math.round(Math.max(0, Math.min(1.0, b)) * a);
        return (a << 24) | (pr << 16) | (pg << 8) | pb;
    }

    private static int applyAlpha(int argb, double alpha) {
        int a = (int) Math.round(Math.max(0, Math.min(1.0, alpha)) * 255.0);
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        int pr = (int) Math.round(r * (a / 255.0));
        int pg = (int) Math.round(g * (a / 255.0));
        int pb = (int) Math.round(b * (a / 255.0));
        return (a << 24) | (pr << 16) | (pg << 8) | pb;
    }
}
