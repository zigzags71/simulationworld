package uiviewer.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import simcore.events.TelemetryBus;
import simcore.events.TelemetryEvent;

public class MonitorBar extends HBox {
    private final Label tickLabel = new Label("Tick: 0");
    private final Label populationLabel = new Label("Population: 0");
    private final Label predictionLabel = new Label("Mean Error: 0.0");
    private final Label fpsLabel = new Label("FPS: --");
    private long lastFrameTime = 0;

    public MonitorBar(TelemetryBus telemetryBus) {
        setSpacing(16);
        setPadding(new Insets(8));
        getChildren().addAll(tickLabel, populationLabel, predictionLabel, fpsLabel);
        telemetryBus.subscribe(this::onTelemetry);
    }

    private void onTelemetry(TelemetryEvent event) {
        tickLabel.setText("Tick: " + event.getTickIndex());
        populationLabel.setText("Population: " + event.getPopulation());
        predictionLabel.setText(String.format("Mean Error: %.3f", event.getMeanPredictionError()));
    }

    public void markFrameRendered(long nowNanos) {
        if (lastFrameTime != 0) {
            double fps = 1_000_000_000.0 / (nowNanos - lastFrameTime);
            fpsLabel.setText(String.format("FPS: %.1f", fps));
        }
        lastFrameTime = nowNanos;
    }
}
