package uiviewer.render;

import simcore.util.MathUtil;

public class Camera {
    private double offsetX;
    private double offsetY;
    private double zoom;
    private final double minZoom = 0.2;
    private final double maxZoom = 8.0;
    private final int worldWidth;
    private final int worldHeight;
    private double viewportWidth = 1024;
    private double viewportHeight = 1024;

    public Camera(int worldWidth, int worldHeight) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.zoom = 1.0;
        this.offsetX = 0;
        this.offsetY = 0;
    }

    public void pan(double deltaScreenX, double deltaScreenY) {
        offsetX -= deltaScreenX / zoom;
        offsetY -= deltaScreenY / zoom;
        clamp();
    }

    public void zoomAt(double factor, double pivotScreenX, double pivotScreenY, double canvasWidth, double canvasHeight) {
        double beforeWorldX = screenToWorldX(pivotScreenX);
        double beforeWorldY = screenToWorldY(pivotScreenY);
        zoom = Math.min(maxZoom, Math.max(minZoom, zoom * factor));
        clamp();
        double afterWorldX = screenToWorldX(pivotScreenX);
        double afterWorldY = screenToWorldY(pivotScreenY);
        offsetX += beforeWorldX - afterWorldX;
        offsetY += beforeWorldY - afterWorldY;
        clamp();
    }

    public void setViewport(double viewportWidth, double viewportHeight) {
        this.viewportWidth = Math.max(1, viewportWidth);
        this.viewportHeight = Math.max(1, viewportHeight);
        clamp();
    }

    public double screenToWorldX(double screenX) {
        return offsetX + screenX / zoom;
    }

    public double screenToWorldY(double screenY) {
        return offsetY + screenY / zoom;
    }

    public double worldToScreenX(double worldX) {
        return (worldX - offsetX) * zoom;
    }

    public double worldToScreenY(double worldY) {
        return (worldY - offsetY) * zoom;
    }

    public double getZoom() {
        return zoom;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public int getVisibleMinX(double canvasWidth) {
        return MathUtil.clamp((int) Math.floor(screenToWorldX(0)), 0, worldWidth - 1);
    }

    public int getVisibleMaxX(double canvasWidth) {
        return MathUtil.clamp((int) Math.ceil(screenToWorldX(canvasWidth)), 0, worldWidth - 1);
    }

    public int getVisibleMinY(double canvasHeight) {
        return MathUtil.clamp((int) Math.floor(screenToWorldY(0)), 0, worldHeight - 1);
    }

    public int getVisibleMaxY(double canvasHeight) {
        return MathUtil.clamp((int) Math.ceil(screenToWorldY(canvasHeight)), 0, worldHeight - 1);
    }

    private void clamp() {
        double maxOffsetX = Math.max(0, worldWidth - viewportWidth / zoom);
        double maxOffsetY = Math.max(0, worldHeight - viewportHeight / zoom);
        offsetX = Math.max(0, Math.min(maxOffsetX, offsetX));
        offsetY = Math.max(0, Math.min(maxOffsetY, offsetY));
    }
}
