package uiviewer.ui;

import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import simcore.snapshot.RenderSnapshot;

import java.util.Collections;

public class AgentInspectorPanel extends VBox {
    private final Label idLabel = new Label("Agent: none");
    private final Label positionLabel = new Label("Position: --");
    private final Label ageLabel = new Label("Age: --");
    private final Label energyLabel = new Label("Energy: --");
    private final Label hungerLabel = new Label("Hunger: --");
    private final Label stressLabel = new Label("Stress: --");
    private final Label predictionErrorLabel = new Label("Prediction Error: --");
    private final Label awarenessLabel = new Label("Awareness: --");
    private final ListView<String> rulesList = new ListView<>();

    public AgentInspectorPanel() {
        setSpacing(4);
        rulesList.setPlaceholder(new Label("No rules recorded (v0.2)"));
        getChildren().addAll(idLabel, positionLabel, ageLabel, energyLabel, hungerLabel, stressLabel,
                predictionErrorLabel, awarenessLabel, new Label("Rules"), rulesList);
    }

    public void clear() {
        idLabel.setText("Agent: none");
        positionLabel.setText("Position: --");
        ageLabel.setText("Age: --");
        energyLabel.setText("Energy: --");
        hungerLabel.setText("Hunger: --");
        stressLabel.setText("Stress: --");
        predictionErrorLabel.setText("Prediction Error: --");
        awarenessLabel.setText("Awareness: --");
        rulesList.getItems().clear();
    }

    public void update(RenderSnapshot snapshot, long agentId) {
        if (snapshot == null || agentId < 0) {
            clear();
            return;
        }
        int foundIndex = -1;
        for (int i = 0; i < snapshot.getAgentCount(); i++) {
            if (snapshot.getAgentId()[i] == agentId) {
                foundIndex = i;
                break;
            }
        }
        if (foundIndex < 0) {
            clear();
            return;
        }
        idLabel.setText("Agent: " + snapshot.getAgentId()[foundIndex]);
        positionLabel.setText("Position: (" + snapshot.getAgentX()[foundIndex] + ", " + snapshot.getAgentY()[foundIndex] + ")");
        ageLabel.setText("Age: " + snapshot.getAgentAge()[foundIndex]);
        energyLabel.setText(String.format("Energy: %.3f", snapshot.getAgentEnergy()[foundIndex]));
        hungerLabel.setText(String.format("Hunger: %.3f", snapshot.getAgentHunger()[foundIndex]));
        stressLabel.setText(String.format("Stress: %.3f", snapshot.getAgentStress()[foundIndex]));
        predictionErrorLabel.setText(String.format("Prediction Error: %.3f", snapshot.getAgentPredictionError()[foundIndex]));
        awarenessLabel.setText("Awareness: " + snapshot.getAgentAwareness()[foundIndex]);
        rulesList.getItems().setAll(Collections.emptyList());
    }
}
