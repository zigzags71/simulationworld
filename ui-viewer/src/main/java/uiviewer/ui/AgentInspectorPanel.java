package uiviewer.ui;

import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import simcore.snapshot.RenderSnapshot;
import simcore.snapshot.RuleView;
import simcore.snapshot.SelectedAgentDetails;

import java.util.ArrayList;
import java.util.List;

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
        rulesList.setPlaceholder(new Label("No rules"));
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
        SelectedAgentDetails details = snapshot.getSelectedAgentDetails();
        if (details == null || details.getAgentId() != agentId) {
            clear();
            return;
        }
        idLabel.setText("Agent: " + details.getAgentId());
        positionLabel.setText("Position: (" + details.getX() + ", " + details.getY() + ")");
        ageLabel.setText("Age: " + details.getAgeTicks());
        energyLabel.setText(String.format("Energy: %.3f", details.getEnergy()));
        hungerLabel.setText(String.format("Hunger: %.3f", details.getHunger()));
        stressLabel.setText(String.format("Stress: %.3f", details.getStress()));
        predictionErrorLabel.setText(String.format("Prediction Error: %.3f", details.getPredictionError()));
        awarenessLabel.setText("Awareness: " + details.isAwareness());
        rulesList.getItems().setAll(renderRules(details.getRules()));
    }

    private List<String> renderRules(RuleView[] rules) {
        List<String> lines = new ArrayList<>();
        for (RuleView rule : rules) {
            String lastUsed = rule.getLastUsedTick() >= 0 ? String.valueOf(rule.getLastUsedTick()) : "-";
            lines.add(String.format("[%s/%s] ctx=%s action=%s trust=%.2f uses=%d succ=%d last=%s err=%.3f",
                    rule.getRuleId(), rule.getType(), rule.getContextSummary(), rule.getAction(),
                    rule.getTrust(), rule.getUses(), rule.getSuccesses(), lastUsed, rule.getLastError()));
        }
        return lines;
    }
}
