package uiviewer.render;

import javafx.animation.AnimationTimer;
import simcore.config.SimConfig;
import simcore.sim.SimulationEngine;
import simcore.snapshot.RenderSnapshot;

public class RenderLoop extends AnimationTimer {
    private final SimulationEngine engine;
    private final CanvasRenderer renderer;
    private OverlayMode overlayMode;
    private long lastRenderNanos = 0;

    public RenderLoop(SimulationEngine engine, CanvasRenderer renderer) {
        this.engine = engine;
        this.renderer = renderer;
        this.overlayMode = OverlayMode.FOOD;
    }

    public void setOverlayMode(OverlayMode overlayMode) {
        this.overlayMode = overlayMode;
    }

    @Override
    public void handle(long now) {
        long frameInterval = 1_000_000_000L / SimConfig.RENDER_MAX_FPS;
        if (now - lastRenderNanos < frameInterval) {
            return;
        }
        lastRenderNanos = now;
        RenderSnapshot snapshot = engine.getLatestSnapshot();
        renderer.render(snapshot, overlayMode);
    }
}
