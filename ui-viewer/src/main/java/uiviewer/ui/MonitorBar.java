package uiviewer.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import simcore.events.TelemetryBus;
import simcore.events.TelemetryEvent;

public class MonitorBar extends HBox {
    private final Label tickLabel = new Label("Tick: 0");
    private final Label populationLabel = new Label("Population: 0");
    private final Label predictionLabel = new Label("Mean Error: 0.0");
    private final Label deathsLabel = new Label("Deaths/Tick: 0 (Total 0)");
    private final Label energyLabel = new Label("Energy: --");
    private final Label hungerLabel = new Label("Hunger: --");
    private final Label stressLabel = new Label("Stress: --");
    private final Label fpsLabel = new Label("FPS: --");
    private final Label tpsLabel = new Label("TPS: --");

    public MonitorBar(TelemetryBus telemetryBus) {
        setSpacing(16);
        setPadding(new Insets(8));
        getChildren().addAll(tickLabel, populationLabel, predictionLabel, deathsLabel,
                energyLabel, hungerLabel, stressLabel, fpsLabel, tpsLabel);
        telemetryBus.subscribe(event -> Platform.runLater(() -> onTelemetry(event)));
    }

    private void onTelemetry(TelemetryEvent event) {
        tickLabel.setText("Tick: " + event.getTickIndex());
        populationLabel.setText("Population: " + event.getPopulation());
        predictionLabel.setText(String.format("Mean Error: %.3f", event.getMeanPredictionError()));
        deathsLabel.setText(String.format("Deaths/Tick: %d (Total %d)", event.getDeathsThisTick(), event.getTotalDeaths()));
        energyLabel.setText(String.format("Energy: %.3f", event.getMeanEnergy()));
        hungerLabel.setText(String.format("Hunger: %.3f", event.getMeanHunger()));
        stressLabel.setText(String.format("Stress: %.3f", event.getMeanStress()));
    }

    public void updatePerformance(double fps, long tps) {
        fpsLabel.setText(String.format("FPS: %.1f", fps));
        tpsLabel.setText(String.format("TPS: %d", tps));
    }
}
