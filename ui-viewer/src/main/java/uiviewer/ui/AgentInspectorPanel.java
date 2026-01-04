package uiviewer.ui;

import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import simcore.agents.AgentState;

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
        rulesList.setPlaceholder(new Label("Rules pending implementation"));
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

    public void update(AgentState agent) {
        if (agent == null) {
            clear();
            return;
        }
        idLabel.setText("Agent: " + agent.getId().value());
        positionLabel.setText("Position: (" + agent.getX() + ", " + agent.getY() + ")");
        ageLabel.setText("Age: " + agent.getAge());
        energyLabel.setText(String.format("Energy: %.3f", agent.getEnergy()));
        hungerLabel.setText(String.format("Hunger: %.3f", agent.getHunger()));
        stressLabel.setText(String.format("Stress: %.3f", agent.getStress()));
        predictionErrorLabel.setText(String.format("Prediction Error: %.3f", agent.getPredictionError()));
        awarenessLabel.setText("Awareness: " + agent.isAwarenessFlag());
        rulesList.getItems().setAll();
    }
}
