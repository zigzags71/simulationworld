package uiviewer.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class TileHoverPanel extends VBox {
    private final Label coordLabel = new Label("Tile: --");
    private final Label foodLabel = new Label("Food: --");
    private final Label hazardLabel = new Label("Hazard: --");
    private final Label crowdingLabel = new Label("Crowding: --");

    public TileHoverPanel() {
        setSpacing(4);
        getChildren().addAll(coordLabel, foodLabel, hazardLabel, crowdingLabel);
    }

    public void update(int x, int y, float food, float hazard, float crowding) {
        coordLabel.setText("Tile: (" + x + ", " + y + ")");
        foodLabel.setText(String.format("Food: %.3f", food));
        hazardLabel.setText(String.format("Hazard: %.3f", hazard));
        crowdingLabel.setText(String.format("Crowding: %.3f", crowding));
    }
}
