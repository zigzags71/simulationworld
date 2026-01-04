package uiviewer.render;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;
import simcore.snapshot.RenderSnapshot;

public class CanvasRenderer {
    private final Canvas canvas;

    public CanvasRenderer(Canvas canvas) {
        this.canvas = canvas;
    }

    public void render(RenderSnapshot snapshot, OverlayMode mode) {
        if (snapshot == null) {
            return;
        }
        int w = snapshot.getWidth();
        int h = snapshot.getHeight();
        double scaleX = canvas.getWidth() / w;
        double scaleY = canvas.getHeight() / h;
        javafx.scene.image.WritableImage image = new javafx.scene.image.WritableImage(w, h);
        PixelWriter writer = image.getPixelWriter();
        float[] overlay = selectOverlay(snapshot, mode);
        int[] cultureColors = buildCultureMap(snapshot, w, h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int idx = y * w + x;
                Color color = colorForValue(overlay[idx], mode, cultureColors[idx]);
                writer.setColor(x, y, color);
            }
        }
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.drawImage(image, 0, 0, w, h, 0, 0, canvas.getWidth(), canvas.getHeight());
        drawAgents(gc, snapshot, scaleX, scaleY);
    }

    private float[] selectOverlay(RenderSnapshot snapshot, OverlayMode mode) {
        return switch (mode) {
            case FOOD -> snapshot.getFood();
            case HAZARD -> snapshot.getHazard();
            case CROWDING, CULTURE -> snapshot.getCrowding();
        };
    }

    private int[] buildCultureMap(RenderSnapshot snapshot, int w, int h) {
        int[] culture = new int[w * h];
        int[] xs = snapshot.getAgentX();
        int[] ys = snapshot.getAgentY();
        int[] colors = snapshot.getAgentColorARGB();
        int count = snapshot.getAgentCount();
        for (int i = 0; i < count; i++) {
            int x = xs[i];
            int y = ys[i];
            if (x >= 0 && y >= 0 && x < w && y < h) {
                culture[y * w + x] = colors[i];
            }
        }
        return culture;
    }

    private Color colorForValue(float value, OverlayMode mode, int cultureColor) {
        double clamped = Math.max(0.0, Math.min(1.0, value));
        return switch (mode) {
            case FOOD -> Color.color(0.2 + clamped * 0.8, 0.5 + clamped * 0.5, 0.2, 1.0);
            case HAZARD -> Color.color(0.4 + clamped * 0.6, 0.2, 0.2 + clamped * 0.8, 1.0);
            case CROWDING -> Color.color(0.2, 0.2 + clamped * 0.8, 0.8, 1.0);
            case CULTURE -> cultureColor == 0 ? Color.DARKGRAY : colorFromArgb(cultureColor);
        };
    }

    private void drawAgents(GraphicsContext gc, RenderSnapshot snapshot, double scaleX, double scaleY) {
        int[] xs = snapshot.getAgentX();
        int[] ys = snapshot.getAgentY();
        int[] colors = snapshot.getAgentColorARGB();
        int count = snapshot.getAgentCount();
        for (int i = 0; i < count; i++) {
            int x = xs[i];
            int y = ys[i];
            int argb = colors[i];
            Color color = colorFromArgb(argb);
            gc.setFill(color);
            gc.fillRect(x * scaleX, y * scaleY, Math.max(1, scaleX), Math.max(1, scaleY));
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
