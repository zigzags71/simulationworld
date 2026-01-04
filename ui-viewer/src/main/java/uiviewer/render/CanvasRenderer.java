package uiviewer.render;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import simcore.snapshot.RenderSnapshot;

public class CanvasRenderer {
    private final Canvas canvas;

    public CanvasRenderer(Canvas canvas) {
        this.canvas = canvas;
    }

    public void render(RenderSnapshot snapshot, OverlayMode mode, Camera camera, boolean showAgents, SelectionState selectionState) {
        if (snapshot == null) {
            return;
        }
        camera.setViewport(canvas.getWidth(), canvas.getHeight());
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        float[] overlay = selectOverlay(snapshot, mode);
        int minX = camera.getVisibleMinX(canvas.getWidth());
        int maxX = camera.getVisibleMaxX(canvas.getWidth());
        int minY = camera.getVisibleMinY(canvas.getHeight());
        int maxY = camera.getVisibleMaxY(canvas.getHeight());
        int[] cultureColors = buildCultureMap(snapshot, minX, maxX, minY, maxY, snapshot.getWidth());
        double tileSize = camera.getZoom();
        double overlayAlpha = showAgents ? 0.35 : 1.0;
        for (int y = minY; y <= maxY; y++) {
            double screenY = camera.worldToScreenY(y);
            for (int x = minX; x <= maxX; x++) {
                int idx = y * snapshot.getWidth() + x;
                Color color = colorForValue(overlay[idx], mode, cultureColors[idx], overlayAlpha);
                double screenX = camera.worldToScreenX(x);
                gc.setFill(color);
                gc.fillRect(screenX, screenY, tileSize + 0.5, tileSize + 0.5);
            }
        }

        if (selectionState != null && selectionState.hasRegion()) {
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

        if (selectionState != null && selectionState.getSelectedTileX() >= 0) {
            gc.setStroke(Color.YELLOW);
            gc.setLineWidth(1.5);
            double sx = camera.worldToScreenX(selectionState.getSelectedTileX());
            double sy = camera.worldToScreenY(selectionState.getSelectedTileY());
            gc.strokeRect(sx, sy, tileSize, tileSize);
        }

        if (showAgents) {
            drawAgents(gc, snapshot, camera, selectionState, minX, maxX, minY, maxY);
        }
    }

    private float[] selectOverlay(RenderSnapshot snapshot, OverlayMode mode) {
        return switch (mode) {
            case FOOD -> snapshot.getFood();
            case HAZARD -> snapshot.getHazard();
            case CROWDING, CULTURE -> snapshot.getCrowding();
        };
    }

    private int[] buildCultureMap(RenderSnapshot snapshot, int minX, int maxX, int minY, int maxY, int worldWidth) {
        int width = snapshot.getWidth();
        int[] culture = new int[width * snapshot.getHeight()];
        int[] xs = snapshot.getAgentX();
        int[] ys = snapshot.getAgentY();
        int[] colors = snapshot.getAgentColorARGB();
        int count = snapshot.getAgentCount();
        for (int i = 0; i < count; i++) {
            int x = xs[i];
            int y = ys[i];
            if (x >= minX && x <= maxX && y >= minY && y <= maxY) {
                culture[y * worldWidth + x] = colors[i];
            }
        }
        return culture;
    }

    private Color colorForValue(float value, OverlayMode mode, int cultureColor, double alpha) {
        double clamped = Math.max(0.0, Math.min(1.0, value));
        return switch (mode) {
            case FOOD -> Color.color(0.2 + clamped * 0.8, 0.5 + clamped * 0.5, 0.2, alpha);
            case HAZARD -> Color.color(0.4 + clamped * 0.6, 0.2, 0.2 + clamped * 0.8, alpha);
            case CROWDING -> Color.color(0.2, 0.2 + clamped * 0.8, 0.8, alpha);
            case CULTURE -> cultureColor == 0 ? Color.color(0.15, 0.15, 0.15, alpha) : colorFromArgb(cultureColor).deriveColor(0, 1, 1, alpha);
        };
    }

    private void drawAgents(GraphicsContext gc, RenderSnapshot snapshot, Camera camera, SelectionState selectionState, int minX, int maxX, int minY, int maxY) {
        int[] xs = snapshot.getAgentX();
        int[] ys = snapshot.getAgentY();
        int[] colors = snapshot.getAgentColorARGB();
        long[] ids = snapshot.getAgentId();
        int count = snapshot.getAgentCount();
        double tileSize = camera.getZoom();
        long selectedId = selectionState != null ? selectionState.getSelectedAgentId() : -1;
        for (int i = 0; i < count; i++) {
            int x = xs[i];
            int y = ys[i];
            if (x < minX || x > maxX || y < minY || y > maxY) {
                continue;
            }
            Color color = Color.rgb(0, 255, 0);
            double screenX = camera.worldToScreenX(x);
            double screenY = camera.worldToScreenY(y);
            boolean selected = ids[i] == selectedId;
            double size = selected ? Math.max(3, tileSize * 1.6) : Math.max(2, tileSize * 0.9);
            double offset = (tileSize - size) / 2.0;
            gc.setFill(selected ? Color.LIME : color);
            gc.fillOval(screenX + offset, screenY + offset, size, size);
            if (selected) {
                gc.setStroke(Color.BLACK);
                gc.setLineWidth(1.2);
                gc.strokeOval(screenX + offset - 1, screenY + offset - 1, size + 2, size + 2);
            }
        }
    }

    private Color colorFromArgb(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return Color.rgb(r, g, b, a / 255.0);
    }
}
