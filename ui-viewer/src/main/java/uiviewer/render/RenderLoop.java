package uiviewer.render;

import javafx.animation.AnimationTimer;
import simcore.config.SimConfig;
import simcore.sim.SimulationEngine;
import simcore.snapshot.RenderSnapshot;

public class RenderLoop extends AnimationTimer {
    private final SimulationEngine engine;
    private final CanvasRenderer renderer;
    private OverlayMode overlayMode;
    private final Camera camera;
    private final SelectionState selectionState;
    private boolean showAgents = true;
    private long lastRenderNanos = 0;

    public RenderLoop(SimulationEngine engine, CanvasRenderer renderer, Camera camera, SelectionState selectionState) {
        this.engine = engine;
        this.renderer = renderer;
        this.camera = camera;
        this.selectionState = selectionState;
        this.overlayMode = OverlayMode.FOOD;
    }

    public void setOverlayMode(OverlayMode overlayMode) {
        this.overlayMode = overlayMode;
    }

    public void setShowAgents(boolean showAgents) {
        this.showAgents = showAgents;
    }

    public Camera getCamera() {
        return camera;
    }

    public SelectionState getSelectionState() {
        return selectionState;
    }

    @Override
    public void handle(long now) {
        long frameInterval = 1_000_000_000L / SimConfig.RENDER_MAX_FPS;
        if (now - lastRenderNanos < frameInterval) {
            return;
        }
        lastRenderNanos = now;
        RenderSnapshot snapshot = engine.getLatestSnapshot();
        renderer.render(snapshot, overlayMode, camera, showAgents, selectionState);
    }
}
