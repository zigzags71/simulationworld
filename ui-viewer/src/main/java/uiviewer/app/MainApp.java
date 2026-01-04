package uiviewer.app;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import simcore.config.MapGenConfig;
import simcore.config.SimConfig;
import simcore.events.TelemetryBus;
import simcore.sim.SimulationEngine;
import simcore.sim.commands.BrushType;
import simcore.sim.commands.PlaceFieldBrushCommand;
import simcore.snapshot.RenderSnapshot;
import simcore.util.MathUtil;
import uiviewer.render.Camera;
import uiviewer.render.CanvasRenderer;
import uiviewer.render.OverlayMode;
import uiviewer.render.RenderLoop;
import uiviewer.render.SelectionState;
import uiviewer.ui.AgentInspectorPanel;
import uiviewer.ui.MonitorBar;
import uiviewer.ui.TileHoverPanel;
import java.util.function.Consumer;

public class MainApp extends Application {
    private SimulationEngine engine;
    private RenderLoop renderLoop;
    private MonitorBar monitorBar;
    private TileHoverPanel tileHoverPanel;
    private AgentInspectorPanel agentInspectorPanel;
    private Camera camera;
    private SelectionState selectionState;
    private boolean panning;
    private double lastPanX;
    private double lastPanY;
    private BrushType currentBrush = BrushType.FOOD;
    private int brushRadius = 1;
    private Button startButton;
    private Button pauseResumeButton;
    private Slider foodSlider;
    private Slider hazardSlider;
    private Slider patchinessSlider;
    private Slider waterSlider;
    private TextField seedField;

    @Override
    public void start(Stage primaryStage) {
        long seed = Long.getLong("sim.seed", SimConfig.DEFAULT_SEED);
        TelemetryBus telemetryBus = new TelemetryBus();
        engine = new SimulationEngine(MapGenConfig.defaults().withSeed(seed), telemetryBus);
        monitorBar = new MonitorBar(telemetryBus);
        tileHoverPanel = new TileHoverPanel();
        agentInspectorPanel = new AgentInspectorPanel();
        camera = new Camera(SimConfig.WORLD_W, SimConfig.WORLD_H);
        selectionState = new SelectionState();

        Canvas canvas = new Canvas(1024, 768);
        CanvasRenderer renderer = new CanvasRenderer(canvas);
        renderLoop = new RenderLoop(engine, renderer, camera, selectionState) {
            @Override
            public void handle(long now) {
                super.handle(now);
                monitorBar.markFrameRendered(now);
            }
        };

        VBox leftPanel = buildOverlayPanel();
        VBox controlPanel = buildControlPanel();
        VBox rightPanel = buildInspectorPanel();

        BorderPane root = new BorderPane();
        root.setTop(monitorBar);
        VBox left = new VBox(leftPanel, controlPanel);
        VBox.setVgrow(controlPanel, Priority.ALWAYS);
        root.setLeft(left);
        root.setCenter(canvas);
        root.setRight(rightPanel);

        attachInteractionHandlers(canvas);

        Scene scene = new Scene(root, 1280, 900);
        primaryStage.setTitle("Simulation Viewer");
        primaryStage.setScene(scene);
        primaryStage.show();

        engine.start();
        engine.pause();
        renderLoop.start();
    }

    private VBox buildOverlayPanel() {
        ToggleButton food = new ToggleButton("Food");
        ToggleButton hazard = new ToggleButton("Hazard");
        ToggleButton crowding = new ToggleButton("Crowding");
        ToggleButton culture = new ToggleButton("Culture");
        ToggleButton agentsToggle = new ToggleButton("Show Agents");
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
        agentsToggle.setSelected(true);
        agentsToggle.selectedProperty().addListener((obs, old, val) -> renderLoop.setShowAgents(val));
        VBox box = new VBox(8, food, hazard, crowding, culture, agentsToggle);
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

    private VBox buildControlPanel() {
        Label header = new Label("Map Controls");
        header.setStyle("-fx-font-weight: bold;");
        seedField = new TextField(String.valueOf(SimConfig.DEFAULT_SEED));
        seedField.setPrefColumnCount(10);
        VBox foodControl = createSliderControl("Food Richness", SimConfig.DEFAULT_FOOD_RICHNESS, slider -> foodSlider = slider);
        VBox hazardControl = createSliderControl("Hazard Baseline", SimConfig.DEFAULT_HAZARD_BASELINE, slider -> hazardSlider = slider);
        VBox patchinessControl = createSliderControl("Patchiness", SimConfig.DEFAULT_PATCHINESS, slider -> patchinessSlider = slider);
        VBox waterControl = createSliderControl("Water Ratio", SimConfig.DEFAULT_WATER_RATIO, slider -> waterSlider = slider);

        Button generateButton = new Button("Generate");
        generateButton.setOnAction(e -> regenerateFromUI());
        Button resetButton = new Button("Reset");
        resetButton.setOnAction(e -> resetDefaults());
        startButton = new Button("Start");
        startButton.setOnAction(e -> startSimulation());
        pauseResumeButton = new Button("Resume");
        pauseResumeButton.setOnAction(e -> togglePause());

        VBox brushBox = buildBrushPanel();

        VBox box = new VBox(10,
                header,
                labeledField("Seed", seedField),
                foodControl,
                hazardControl,
                patchinessControl,
                waterControl,
                new HBox(8, generateButton, resetButton),
                new HBox(8, startButton, pauseResumeButton),
                new Separator(),
                brushBox);
        box.setPadding(new Insets(8));
        return box;
    }

    private VBox buildBrushPanel() {
        Label brushHeader = new Label("Brush");
        brushHeader.setStyle("-fx-font-weight: bold;");
        ToggleGroup typeGroup = new ToggleGroup();
        ToggleButton food = new ToggleButton("Food Brush");
        ToggleButton hazard = new ToggleButton("Hazard Brush");
        ToggleButton erase = new ToggleButton("Eraser");
        food.setToggleGroup(typeGroup);
        hazard.setToggleGroup(typeGroup);
        erase.setToggleGroup(typeGroup);
        food.setSelected(true);
        typeGroup.selectedToggleProperty().addListener((obs, old, val) -> {
            if (val == food) {
                currentBrush = BrushType.FOOD;
            } else if (val == hazard) {
                currentBrush = BrushType.HAZARD;
            } else if (val == erase) {
                currentBrush = BrushType.ERASE;
            }
        });

        ToggleGroup sizeGroup = new ToggleGroup();
        ToggleButton small = new ToggleButton("Small");
        ToggleButton medium = new ToggleButton("Medium");
        ToggleButton large = new ToggleButton("Large");
        small.setToggleGroup(sizeGroup);
        medium.setToggleGroup(sizeGroup);
        large.setToggleGroup(sizeGroup);
        small.setSelected(true);
        small.setOnAction(e -> brushRadius = 1);
        medium.setOnAction(e -> brushRadius = 3);
        large.setOnAction(e -> brushRadius = 7);

        HBox typeRow = new HBox(6, food, hazard, erase);
        HBox sizeRow = new HBox(6, small, medium, large);
        typeRow.setAlignment(Pos.CENTER_LEFT);
        sizeRow.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(6, brushHeader, new Label("Tool"), typeRow, new Label("Size"), sizeRow);
        return box;
    }

    private VBox createSliderControl(String label, double initial, Consumer<Slider> assign) {
        Label caption = new Label(label);
        Label valueLabel = new Label(String.format("%.2f", initial));
        Slider slider = new Slider(0, 1, initial);
        slider.setMajorTickUnit(0.25);
        slider.setBlockIncrement(0.01);
        slider.valueProperty().addListener((obs, old, val) -> valueLabel.setText(String.format("%.2f", val.doubleValue())));
        assign.accept(slider);
        VBox.setMargin(caption, new Insets(4, 0, 0, 0));
        return new VBox(2, caption, slider, valueLabel);
    }

    private HBox labeledField(String label, Control field) {
        Label lbl = new Label(label);
        HBox box = new HBox(6, lbl, field);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void attachInteractionHandlers(Canvas canvas) {
        canvas.addEventHandler(MouseEvent.MOUSE_MOVED, this::updateHover);
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleClick);
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, this::beginPan);
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::dragPan);
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> panning = false);
        canvas.addEventHandler(ScrollEvent.SCROLL, this::handleScroll);
    }

    private void updateHover(MouseEvent event) {
        RenderSnapshot snapshot = engine.getLatestSnapshot();
        if (snapshot == null) {
            return;
        }
        int[] coords = toTile(event.getX(), event.getY(), snapshot);
        int tileX = coords[0];
        int tileY = coords[1];
        int idx = MathUtil.index(tileX, tileY, snapshot.getWidth());
        tileHoverPanel.updateHover(tileX, tileY, snapshot.getFood()[idx], snapshot.getHazard()[idx], snapshot.getCrowding()[idx],
                snapshot.getAgentCounts()[idx]);
    }

    private void handleClick(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }
        RenderSnapshot snapshot = engine.getLatestSnapshot();
        if (snapshot == null) {
            return;
        }
        int[] coords = toTile(event.getX(), event.getY(), snapshot);
        int tileX = coords[0];
        int tileY = coords[1];
        int idx = MathUtil.index(tileX, tileY, snapshot.getWidth());
        selectionState.setSelectedTile(tileX, tileY);
        tileHoverPanel.updateSelection(tileX, tileY, snapshot.getFood()[idx], snapshot.getHazard()[idx], snapshot.getCrowding()[idx],
                snapshot.getAgentCounts()[idx]);
        engine.queueBrushCommand(new PlaceFieldBrushCommand(currentBrush, tileX, tileY, brushRadius, System.nanoTime()));
        double worldX = camera.screenToWorldX(event.getX());
        double worldY = camera.screenToWorldY(event.getY());
        long closestId = findAgentAt(snapshot, tileX, tileY, worldX, worldY);
        selectionState.setSelectedAgentId(closestId);
        agentInspectorPanel.update(snapshot, closestId);
    }

    private void beginPan(MouseEvent event) {
        if (event.getButton() == MouseButton.SECONDARY || event.getButton() == MouseButton.MIDDLE) {
            panning = true;
            lastPanX = event.getX();
            lastPanY = event.getY();
        }
    }

    private void dragPan(MouseEvent event) {
        if (!panning) {
            return;
        }
        double dx = event.getX() - lastPanX;
        double dy = event.getY() - lastPanY;
        camera.pan(dx, dy);
        lastPanX = event.getX();
        lastPanY = event.getY();
    }

    private void handleScroll(ScrollEvent event) {
        double factor = event.getDeltaY() > 0 ? 1.1 : 0.9;
        double width = extractCanvasWidth(event);
        double height = extractCanvasHeight(event);
        camera.zoomAt(factor, event.getX(), event.getY(), width, height);
    }

    private double extractCanvasWidth(ScrollEvent event) {
        Object src = event.getSource();
        if (src instanceof Canvas canvas) {
            return canvas.getWidth();
        }
        return 1024;
    }

    private double extractCanvasHeight(ScrollEvent event) {
        Object src = event.getSource();
        if (src instanceof Canvas canvas) {
            return canvas.getHeight();
        }
        return 768;
    }

    private int[] toTile(double mouseX, double mouseY, RenderSnapshot snapshot) {
        double worldX = camera.screenToWorldX(mouseX);
        double worldY = camera.screenToWorldY(mouseY);
        int tileX = MathUtil.clamp((int) worldX, 0, snapshot.getWidth() - 1);
        int tileY = MathUtil.clamp((int) worldY, 0, snapshot.getHeight() - 1);
        return new int[]{tileX, tileY};
    }

    private long findAgentAt(RenderSnapshot snapshot, int tileX, int tileY, double worldX, double worldY) {
        int count = snapshot.getAgentCount();
        int[] xs = snapshot.getAgentX();
        int[] ys = snapshot.getAgentY();
        long[] ids = snapshot.getAgentId();
        long bestId = -1;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            if (xs[i] == tileX && ys[i] == tileY) {
                double dx = (xs[i] + 0.5) - worldX;
                double dy = (ys[i] + 0.5) - worldY;
                double dist = dx * dx + dy * dy;
                if (dist < bestDist) {
                    bestDist = dist;
                    bestId = ids[i];
                }
            }
        }
        return bestId;
    }

    private void regenerateFromUI() {
        engine.pause();
        MapGenConfig config = MapGenConfig.defaults()
                .withSeed(parseSeed())
                .withFoodRichness((float) foodSlider.getValue())
                .withHazardBaseline((float) hazardSlider.getValue())
                .withPatchiness((float) patchinessSlider.getValue())
                .withWaterRatio((float) waterSlider.getValue());
        engine.regenerate(config);
        selectionState.clearTile();
        selectionState.clearAgent();
        tileHoverPanel.clearSelection();
        agentInspectorPanel.clear();
        startButton.setDisable(false);
        pauseResumeButton.setText("Resume");
    }

    private void resetDefaults() {
        seedField.setText(String.valueOf(SimConfig.DEFAULT_SEED));
        foodSlider.setValue(SimConfig.DEFAULT_FOOD_RICHNESS);
        hazardSlider.setValue(SimConfig.DEFAULT_HAZARD_BASELINE);
        patchinessSlider.setValue(SimConfig.DEFAULT_PATCHINESS);
        waterSlider.setValue(SimConfig.DEFAULT_WATER_RATIO);
        regenerateFromUI();
    }

    private void startSimulation() {
        engine.resume();
        startButton.setDisable(true);
        pauseResumeButton.setText("Pause");
    }

    private void togglePause() {
        if (pauseResumeButton.getText().equals("Pause")) {
            engine.pause();
            pauseResumeButton.setText("Resume");
        } else {
            engine.resume();
            pauseResumeButton.setText("Pause");
        }
    }

    private long parseSeed() {
        try {
            return Long.parseLong(seedField.getText());
        } catch (NumberFormatException e) {
            seedField.setText(String.valueOf(SimConfig.DEFAULT_SEED));
            return SimConfig.DEFAULT_SEED;
        }
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
