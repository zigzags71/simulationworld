package uiviewer.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import simcore.snapshot.RenderSnapshot;
import uiviewer.config.UIConfig;
import uiviewer.render.SelectionState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongConsumer;

public class RegionInspectorPanel extends VBox {
    private final Label regionLabel = new Label("Region: none");
    private final Label countLabel = new Label("Agents: --");
    private final Label energyLabel = new Label("Avg Energy: --");
    private final Label hungerLabel = new Label("Avg Hunger: --");
    private final Label stressLabel = new Label("Avg Stress: --");
    private final Label predictionLabel = new Label("Avg Prediction Error: --");
    private final Label extraLabel = new Label("");
    private final ListView<String> agentList = new ListView<>();
    private Runnable clearHandler;
    private LongConsumer agentSelectionHandler;

    public RegionInspectorPanel() {
        setSpacing(6);
        setPadding(new Insets(4, 0, 0, 0));
        agentList.setPrefHeight(160);
        agentList.setPlaceholder(new Label("No agents"));
        Button clearButton = new Button("Clear Selection");
        clearButton.setOnAction(e -> {
            if (clearHandler != null) {
                clearHandler.run();
            }
            clear();
        });
        agentList.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (val != null && agentSelectionHandler != null) {
                try {
                    agentSelectionHandler.accept(Long.parseLong(val));
                } catch (NumberFormatException ignored) {
                }
            }
        });
        HBox header = new HBox(8, new Label("Region Inspector"), clearButton);
        getChildren().addAll(header, regionLabel, countLabel, energyLabel, hungerLabel, stressLabel, predictionLabel, extraLabel,
                new Label("Agent IDs"), agentList);
    }

    public void setOnClearSelection(Runnable handler) {
        this.clearHandler = handler;
    }

    public void setOnAgentSelected(LongConsumer handler) {
        this.agentSelectionHandler = handler;
    }

    public void clear() {
        regionLabel.setText("Region: none");
        countLabel.setText("Agents: --");
        energyLabel.setText("Avg Energy: --");
        hungerLabel.setText("Avg Hunger: --");
        stressLabel.setText("Avg Stress: --");
        predictionLabel.setText("Avg Prediction Error: --");
        agentList.getItems().clear();
        agentList.getSelectionModel().clearSelection();
        extraLabel.setText("");
    }

    public void update(RenderSnapshot snapshot, SelectionState selectionState) {
        if (snapshot == null || selectionState == null || !selectionState.hasRegion()) {
            clear();
            return;
        }
        int minX = Math.min(selectionState.getRegionStartX(), selectionState.getRegionEndX());
        int maxX = Math.max(selectionState.getRegionStartX(), selectionState.getRegionEndX());
        int minY = Math.min(selectionState.getRegionStartY(), selectionState.getRegionEndY());
        int maxY = Math.max(selectionState.getRegionStartY(), selectionState.getRegionEndY());
        minX = clamp(minX, 0, snapshot.getWidth() - 1);
        maxX = clamp(maxX, 0, snapshot.getWidth() - 1);
        minY = clamp(minY, 0, snapshot.getHeight() - 1);
        maxY = clamp(maxY, 0, snapshot.getHeight() - 1);
        regionLabel.setText("Region: (" + minX + ", " + minY + ") - (" + maxX + ", " + maxY + ")");
        int count = snapshot.getAgentCount();
        int[] xs = snapshot.getAgentX();
        int[] ys = snapshot.getAgentY();
        long[] ids = snapshot.getAgentId();
        float[] energy = snapshot.getAgentEnergy();
        float[] hunger = snapshot.getAgentHunger();
        float[] stress = snapshot.getAgentStress();
        float[] prediction = snapshot.getAgentPredictionError();

        int agentsInRegion = 0;
        double energySum = 0;
        double hungerSum = 0;
        double stressSum = 0;
        double predictionSum = 0;
        List<String> listed = new ArrayList<>();
        int cap = UIConfig.REGION_AGENT_LIST_CAP;
        for (int i = 0; i < count; i++) {
            int x = xs[i];
            int y = ys[i];
            if (x >= minX && x <= maxX && y >= minY && y <= maxY) {
                agentsInRegion++;
                energySum += energy[i];
                hungerSum += hunger[i];
                stressSum += stress[i];
                predictionSum += prediction[i];
                if (listed.size() < cap) {
                    listed.add(String.valueOf(ids[i]));
                }
            }
        }
        countLabel.setText("Agents: " + agentsInRegion);
        if (agentsInRegion > 0) {
            energyLabel.setText(String.format("Avg Energy: %.3f", energySum / agentsInRegion));
            hungerLabel.setText(String.format("Avg Hunger: %.3f", hungerSum / agentsInRegion));
            stressLabel.setText(String.format("Avg Stress: %.3f", stressSum / agentsInRegion));
            predictionLabel.setText(String.format("Avg Prediction Error: %.3f", predictionSum / agentsInRegion));
        } else {
            energyLabel.setText("Avg Energy: --");
            hungerLabel.setText("Avg Hunger: --");
            stressLabel.setText("Avg Stress: --");
            predictionLabel.setText("Avg Prediction Error: --");
        }
        String selected = agentList.getSelectionModel().getSelectedItem();
        agentList.getItems().setAll(listed);
        if (selected != null && listed.contains(selected)) {
            agentList.getSelectionModel().select(selected);
        }
        int remaining = agentsInRegion - listed.size();
        extraLabel.setText(remaining > 0 ? "+" + remaining + " more" : "");
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
