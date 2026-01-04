package uiviewer.render;

import javafx.animation.AnimationTimer;
import simcore.config.SimConfig;
import simcore.sim.SimulationEngine;
import simcore.snapshot.RenderSnapshot;
import uiviewer.config.UIConfig;

import java.util.function.BiConsumer;

public class RenderLoop extends AnimationTimer {
    private final SimulationEngine engine;
    private final CanvasRenderer renderer;
    private OverlayMode overlayMode;
    private final Camera camera;
    private final SelectionState selectionState;
    private boolean showAgents = true;
    private long lastRenderNanos = 0;
    private BiConsumer<RenderSnapshot, Long> afterRender;
    private final boolean benchmarkEnabled = UIConfig.RENDER_BENCHMARK_LOGGING;
    private long accumulatedRenderNanos = 0;
    private long accumulatedFrames = 0;
    private long lastLogNanos = 0;

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

    public void setAfterRender(BiConsumer<RenderSnapshot, Long> afterRender) {
        this.afterRender = afterRender;
    }

    @Override
    public void handle(long now) {
        long frameInterval = 1_000_000_000L / SimConfig.RENDER_MAX_FPS;
        if (now - lastRenderNanos < frameInterval) {
            return;
        }
        RenderSnapshot snapshot = engine.getLatestSnapshot();
        if (snapshot == null) {
            return;
        }
        long start = System.nanoTime();
        renderer.render(snapshot, overlayMode, camera, showAgents, selectionState);
        lastRenderNanos = now;
        if (afterRender != null) {
            afterRender.accept(snapshot, now);
        }
        long renderDuration = System.nanoTime() - start;
        trackBenchmark(now, renderDuration);
    }

    private void trackBenchmark(long now, long renderDuration) {
        if (!benchmarkEnabled) {
            return;
        }
        accumulatedRenderNanos += renderDuration;
        accumulatedFrames++;
        if (lastLogNanos == 0) {
            lastLogNanos = now;
            return;
        }
        long elapsed = now - lastLogNanos;
        if (elapsed >= 5_000_000_000L && accumulatedFrames > 0) {
            double avgMs = (double) accumulatedRenderNanos / accumulatedFrames / 1_000_000.0;
            System.out.printf("[ui] avg render %.3f ms over %d frames%n", avgMs, accumulatedFrames);
            accumulatedRenderNanos = 0;
            accumulatedFrames = 0;
            lastLogNanos = now;
        }
    }
}
