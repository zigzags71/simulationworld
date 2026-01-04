package uiviewer.app;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import simcore.agents.AgentState;
import simcore.config.SimConfig;
import simcore.events.TelemetryBus;
import simcore.sim.SimulationEngine;
import simcore.snapshot.RenderSnapshot;
import simcore.util.MathUtil;
import uiviewer.render.CanvasRenderer;
import uiviewer.render.OverlayMode;
import uiviewer.render.RenderLoop;
import uiviewer.ui.AgentInspectorPanel;
import uiviewer.ui.MonitorBar;
import uiviewer.ui.TileHoverPanel;

public class MainApp extends Application {
    private SimulationEngine engine;
    private RenderLoop renderLoop;
    private MonitorBar monitorBar;
    private TileHoverPanel tileHoverPanel;
    private AgentInspectorPanel agentInspectorPanel;

    @Override
    public void start(Stage primaryStage) {
        long seed = Long.getLong("sim.seed", SimConfig.DEFAULT_SEED);
        TelemetryBus telemetryBus = new TelemetryBus();
        engine = new SimulationEngine(seed, telemetryBus);
        monitorBar = new MonitorBar(telemetryBus);
        tileHoverPanel = new TileHoverPanel();
        agentInspectorPanel = new AgentInspectorPanel();

        Canvas canvas = new Canvas(1024, 1024);
        CanvasRenderer renderer = new CanvasRenderer(canvas);
        renderLoop = new RenderLoop(engine, renderer) {
            @Override
            public void handle(long now) {
                super.handle(now);
                monitorBar.markFrameRendered(now);
            }
        };

        VBox leftPanel = buildOverlayPanel();
        VBox rightPanel = buildInspectorPanel();

        BorderPane root = new BorderPane();
        root.setTop(monitorBar);
        root.setLeft(leftPanel);
        root.setCenter(canvas);
        root.setRight(rightPanel);

        attachInteractionHandlers(canvas);

        Scene scene = new Scene(root, 1280, 900);
        primaryStage.setTitle("Simulation Viewer");
        primaryStage.setScene(scene);
        primaryStage.show();

        engine.start();
        renderLoop.start();
    }

    private VBox buildOverlayPanel() {
        ToggleButton food = new ToggleButton("Food");
        ToggleButton hazard = new ToggleButton("Hazard");
        ToggleButton crowding = new ToggleButton("Crowding");
        ToggleButton culture = new ToggleButton("Culture");
        ToggleGroup group = new ToggleGroup();
        food.setToggleGroup(group);
        hazard.setToggleGroup(group);
        crowding.setToggleGroup(group);
        culture.setToggleGroup(group);
        food.setSelected(true);
        group.selectedToggleProperty().addListener((obs, old, val) -> {
            if (val == food) {
                renderLoop.setOverlayMode(OverlayMode.FOOD);
            } else if (val == hazard) {
                renderLoop.setOverlayMode(OverlayMode.HAZARD);
            } else if (val == crowding) {
                renderLoop.setOverlayMode(OverlayMode.CROWDING);
            } else if (val == culture) {
                renderLoop.setOverlayMode(OverlayMode.CULTURE);
            }
        });
        VBox box = new VBox(8, food, hazard, crowding, culture);
        box.setPadding(new Insets(8));
        return box;
    }

    private VBox buildInspectorPanel() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(8));
        box.getChildren().addAll(tileHoverPanel, agentInspectorPanel);
        VBox.setVgrow(agentInspectorPanel, Priority.ALWAYS);
        agentInspectorPanel.setPrefHeight(300);
        return box;
    }

    private void attachInteractionHandlers(Canvas canvas) {
        canvas.addEventHandler(MouseEvent.MOUSE_MOVED, event -> updateHover(event, canvas));
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> selectAgent(event, canvas));
    }

    private void updateHover(MouseEvent event, Canvas canvas) {
        RenderSnapshot snapshot = engine.getLatestSnapshot();
        if (snapshot == null) {
            return;
        }
        int tileX = toTile(event.getX(), canvas.getWidth(), snapshot.getWidth());
        int tileY = toTile(event.getY(), canvas.getHeight(), snapshot.getHeight());
        int idx = MathUtil.index(tileX, tileY, snapshot.getWidth());
        tileHoverPanel.update(tileX, tileY, snapshot.getFood()[idx], snapshot.getHazard()[idx], snapshot.getCrowding()[idx]);
    }

    private void selectAgent(MouseEvent event, Canvas canvas) {
        RenderSnapshot snapshot = engine.getLatestSnapshot();
        if (snapshot == null) {
            return;
        }
        int tileX = toTile(event.getX(), canvas.getWidth(), snapshot.getWidth());
        int tileY = toTile(event.getY(), canvas.getHeight(), snapshot.getHeight());
        long closestId = findAgentAt(snapshot, tileX, tileY);
        AgentState agent = closestId >= 0 ? engine.findAgentById(closestId) : null;
        agentInspectorPanel.update(agent);
    }

    private long findAgentAt(RenderSnapshot snapshot, int tileX, int tileY) {
        int count = snapshot.getAgentCount();
        int[] xs = snapshot.getAgentX();
        int[] ys = snapshot.getAgentY();
        long[] ids = snapshot.getAgentId();
        for (int i = 0; i < count; i++) {
            if (xs[i] == tileX && ys[i] == tileY) {
                return ids[i];
            }
        }
        return -1;
    }

    private int toTile(double mouseCoord, double canvasSize, int worldSize) {
        double normalized = mouseCoord / canvasSize;
        int tile = (int) (normalized * worldSize);
        return Math.max(0, Math.min(worldSize - 1, tile));
    }

    @Override
    public void stop() {
        renderLoop.stop();
        engine.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
