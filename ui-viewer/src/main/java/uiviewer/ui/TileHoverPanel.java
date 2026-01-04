package uiviewer.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class TileHoverPanel extends VBox {
    private final Label hoverTitle = new Label("Hover");
    private final Label hoverCoord = new Label("Tile: --");
    private final Label hoverFood = new Label("Food: --");
    private final Label hoverHazard = new Label("Hazard: --");
    private final Label hoverCrowding = new Label("Crowding: --");
    private final Label hoverAgents = new Label("Agents: --");

    private final Label selectionTitle = new Label("Selected");
    private final Label selectionCoord = new Label("Tile: --");
    private final Label selectionFood = new Label("Food: --");
    private final Label selectionHazard = new Label("Hazard: --");
    private final Label selectionCrowding = new Label("Crowding: --");
    private final Label selectionAgents = new Label("Agents: --");

    public TileHoverPanel() {
        setSpacing(4);
        getChildren().addAll(hoverTitle, hoverCoord, hoverFood, hoverHazard, hoverCrowding, hoverAgents,
                selectionTitle, selectionCoord, selectionFood, selectionHazard, selectionCrowding, selectionAgents);
    }

    public void updateHover(int x, int y, float food, float hazard, float crowding, int agentCount) {
        hoverCoord.setText("Tile: (" + x + ", " + y + ")");
        hoverFood.setText(String.format("Food: %.3f", food));
        hoverHazard.setText(String.format("Hazard: %.3f", hazard));
        hoverCrowding.setText(String.format("Crowding: %.3f", crowding));
        hoverAgents.setText("Agents: " + agentCount);
    }

    public void updateSelection(int x, int y, float food, float hazard, float crowding, int agentCount) {
        selectionCoord.setText("Tile: (" + x + ", " + y + ")");
        selectionFood.setText(String.format("Food: %.3f", food));
        selectionHazard.setText(String.format("Hazard: %.3f", hazard));
        selectionCrowding.setText(String.format("Crowding: %.3f", crowding));
        selectionAgents.setText("Agents: " + agentCount);
    }

    public void clearSelection() {
        selectionCoord.setText("Tile: --");
        selectionFood.setText("Food: --");
        selectionHazard.setText("Hazard: --");
        selectionCrowding.setText("Crowding: --");
        selectionAgents.setText("Agents: --");
    }
}
