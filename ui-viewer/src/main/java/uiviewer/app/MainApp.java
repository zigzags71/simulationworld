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
import simcore.sim.commands.PlaceEmitterCommand;
import simcore.sim.commands.SpawnAgentsCommand;
import simcore.sim.commands.SetSelectedAgentCommand;
import simcore.sim.commands.SetSelectedRegionCommand;
import simcore.sim.commands.RemoveEmitterCommand;
import simcore.snapshot.RenderSnapshot;
import simcore.util.MathUtil;
import uiviewer.config.UIConfig;
import uiviewer.render.Camera;
import uiviewer.render.CanvasRenderer;
import uiviewer.render.OverlayMode;
import uiviewer.render.RenderLoop;
import uiviewer.render.SelectionState;
import uiviewer.ui.AgentInspectorPanel;
import uiviewer.ui.MonitorBar;
import uiviewer.ui.RegionInspectorPanel;
import uiviewer.ui.TileHoverPanel;
import java.util.function.Consumer;

public class MainApp extends Application {
    private enum ToolMode {SELECT, FOOD, HAZARD, ERASE, AGENT, EMITTER}

    private SimulationEngine engine;
    private RenderLoop renderLoop;
    private MonitorBar monitorBar;
    private TileHoverPanel tileHoverPanel;
    private AgentInspectorPanel agentInspectorPanel;
    private RegionInspectorPanel regionInspectorPanel;
    private Camera camera;
    private SelectionState selectionState;
    private boolean panning;
    private double lastPanX;
    private double lastPanY;
    private BrushType currentBrush = BrushType.FOOD;
    private ToolMode currentTool = ToolMode.SELECT;
    private int brushRadius = 1;
    private int agentSpawnCount = UIConfig.DEFAULT_AGENT_SPAWN;
    private boolean painting;
    private boolean emitterRemoving;
    private boolean selectingRegion;
    private long lastPaintMillis;
    private Button startButton;
    private Button pauseResumeButton;
    private Slider foodSlider;
    private Slider hazardSlider;
    private Slider patchinessSlider;
    private Slider waterSlider;
    private Slider agentSpawnSlider;
    private TextField seedField;

    @Override
    public void start(Stage primaryStage) {
        long seed = Long.getLong("sim.seed", SimConfig.DEFAULT_SEED);
        TelemetryBus telemetryBus = new TelemetryBus();
        engine = new SimulationEngine(MapGenConfig.defaults().withSeed(seed), telemetryBus);
        monitorBar = new MonitorBar(telemetryBus);
        tileHoverPanel = new TileHoverPanel();
        agentInspectorPanel = new AgentInspectorPanel();
        regionInspectorPanel = new RegionInspectorPanel();
        camera = new Camera(SimConfig.WORLD_W, SimConfig.WORLD_H);
        selectionState = new SelectionState();

        Canvas canvas = new Canvas(1024, 768);
        CanvasRenderer renderer = new CanvasRenderer(canvas);
        renderLoop = new RenderLoop(engine, renderer, camera, selectionState);
        renderLoop.setAfterRender((snapshot, now) -> {
            monitorBar.markFrameRendered(now);
            regionInspectorPanel.update(snapshot, selectionState);
            agentInspectorPanel.update(snapshot, selectionState.getSelectedAgentId());
        });

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
        regionInspectorPanel.setOnClearSelection(() -> {
            selectionState.clearRegion();
            selectionState.clearTile();
            selectionState.clearAgent();
            engine.queueSelectedAgentCommand(new SetSelectedAgentCommand(-1));
            engine.queueSelectedRegionCommand(new SetSelectedRegionCommand(-1, -1, -1, -1));
            tileHoverPanel.clearSelection();
            agentInspectorPanel.clear();
            regionInspectorPanel.clear();
        });
        regionInspectorPanel.setOnAgentSelected(this::selectAgentFromRegionList);
        box.getChildren().addAll(tileHoverPanel, agentInspectorPanel, regionInspectorPanel);
        VBox.setVgrow(agentInspectorPanel, Priority.ALWAYS);
        VBox.setVgrow(regionInspectorPanel, Priority.ALWAYS);
        agentInspectorPanel.setPrefHeight(300);
        return box;
    }

    private VBox buildControlPanel() {
        Label header = new Label("Map Controls");
        header.setStyle("-fx-font-weight: bold;");
        seedField = new TextField(String.valueOf(SimConfig.DEFAULT_SEED));
        seedField.setPrefColumnCount(10);
        VBox foodControl = createSliderControl("Food Richness", SimConfig.DEFAULT_FOOD_RICHNESS, slider -> foodSlider = slider, null);
        VBox hazardControl = createSliderControl("Hazard Baseline", SimConfig.DEFAULT_HAZARD_BASELINE, slider -> hazardSlider = slider, null);
        VBox patchinessControl = createSliderControl("Patchiness", SimConfig.DEFAULT_PATCHINESS, slider -> patchinessSlider = slider,
                "Controls blob size/smoothing of generated fields. Higher values create larger contiguous regions; lower values create smaller speckled variation.");
        VBox waterControl = createSliderControl("Water Ratio", SimConfig.DEFAULT_WATER_RATIO, slider -> waterSlider = slider,
                "Fraction of tiles generated as water (unwalkable). Higher values create more water/obstacles.");

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
        ToggleButton select = new ToggleButton("Select");
        ToggleButton food = new ToggleButton("Food Brush");
        ToggleButton hazard = new ToggleButton("Hazard Brush");
        ToggleButton erase = new ToggleButton("Eraser");
        ToggleButton agent = new ToggleButton("Agent Brush");
        ToggleButton emitter = new ToggleButton("Emitter");
        select.setToggleGroup(typeGroup);
        food.setToggleGroup(typeGroup);
        hazard.setToggleGroup(typeGroup);
        erase.setToggleGroup(typeGroup);
        agent.setToggleGroup(typeGroup);
        emitter.setToggleGroup(typeGroup);
        select.setSelected(true);
        typeGroup.selectedToggleProperty().addListener((obs, old, val) -> {
            if (val == food) {
                currentTool = ToolMode.FOOD;
                currentBrush = BrushType.FOOD;
            } else if (val == hazard) {
                currentTool = ToolMode.HAZARD;
                currentBrush = BrushType.HAZARD;
            } else if (val == erase) {
                currentTool = ToolMode.ERASE;
                currentBrush = BrushType.ERASE;
            } else if (val == agent) {
                currentTool = ToolMode.AGENT;
            } else if (val == emitter) {
                currentTool = ToolMode.EMITTER;
            } else {
                currentTool = ToolMode.SELECT;
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

        Label spawnLabel = new Label("Spawn per spray");
        Label spawnValue = new Label(String.valueOf(agentSpawnCount));
        agentSpawnSlider = new Slider(UIConfig.MIN_AGENT_SPAWN, UIConfig.MAX_AGENT_SPAWN, UIConfig.DEFAULT_AGENT_SPAWN);
        agentSpawnSlider.setMajorTickUnit(5);
        agentSpawnSlider.setMinorTickCount(4);
        agentSpawnSlider.setBlockIncrement(1);
        agentSpawnSlider.setSnapToTicks(true);
        agentSpawnSlider.valueProperty().addListener((obs, old, val) -> {
            agentSpawnCount = val.intValue();
            spawnValue.setText(String.valueOf(agentSpawnCount));
        });
        agentSpawnSlider.disableProperty().bind(agent.selectedProperty().not());

        VBox spawnBox = new VBox(4, spawnLabel, agentSpawnSlider, spawnValue);

        HBox typeRow = new HBox(6, select, food, hazard);
        HBox typeRow2 = new HBox(6, agent, erase, emitter);
        HBox sizeRow = new HBox(6, small, medium, large);
        typeRow.setAlignment(Pos.CENTER_LEFT);
        typeRow2.setAlignment(Pos.CENTER_LEFT);
        sizeRow.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(6, brushHeader, new Label("Tool"), typeRow, typeRow2, new Label("Size"), sizeRow, spawnBox);
        return box;
    }

    private VBox createSliderControl(String label, double initial, Consumer<Slider> assign, String tooltip) {
        Label caption = new Label(label);
        if (tooltip != null && !tooltip.isEmpty()) {
            Tooltip tip = new Tooltip(tooltip);
            caption.setTooltip(tip);
        }
        Label valueLabel = new Label(String.format("%.2f", initial));
        Slider slider = new Slider(0, 1, initial);
        if (tooltip != null && !tooltip.isEmpty()) {
            slider.setTooltip(new Tooltip(tooltip));
        }
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
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handlePress);
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleDrag);
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, this::handleRelease);
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

    private void handlePress(MouseEvent event) {
        if (currentTool == ToolMode.EMITTER) {
            if (event.getButton() == MouseButton.MIDDLE) {
                panning = true;
                lastPanX = event.getX();
                lastPanY = event.getY();
                return;
            }
            if (event.getButton() != MouseButton.PRIMARY && event.getButton() != MouseButton.SECONDARY) {
                return;
            }
            RenderSnapshot snapshot = engine.getLatestSnapshot();
            if (snapshot == null) {
                return;
            }
            int[] coords = toTile(event.getX(), event.getY(), snapshot);
            int tileX = coords[0];
            int tileY = coords[1];
            emitterRemoving = event.isShiftDown() || event.getButton() == MouseButton.SECONDARY;
            startEmitterPainting(tileX, tileY);
            return;
        }
        if (event.getButton() == MouseButton.SECONDARY || event.getButton() == MouseButton.MIDDLE) {
            panning = true;
            lastPanX = event.getX();
            lastPanY = event.getY();
            return;
        }
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
        if (currentTool == ToolMode.SELECT) {
            selectingRegion = true;
            selectionState.beginRegion(tileX, tileY);
            selectionState.setSelectedTile(tileX, tileY);
        } else {
            startPainting(tileX, tileY);
        }
    }

    private void handleDrag(MouseEvent event) {
        if (panning && (event.isSecondaryButtonDown() || event.isMiddleButtonDown())) {
            double dx = event.getX() - lastPanX;
            double dy = event.getY() - lastPanY;
            camera.pan(dx, dy);
            lastPanX = event.getX();
            lastPanY = event.getY();
            return;
        }
        RenderSnapshot snapshot = engine.getLatestSnapshot();
        if (snapshot == null) {
            return;
        }
        int[] coords = toTile(event.getX(), event.getY(), snapshot);
        int tileX = coords[0];
        int tileY = coords[1];
        if (currentTool == ToolMode.EMITTER) {
            if (painting && (event.isPrimaryButtonDown() || event.isSecondaryButtonDown())) {
                applyEmitterIfDue(tileX, tileY);
            }
            return;
        }
        if (selectingRegion && event.isPrimaryButtonDown()) {
            selectionState.updateRegionEnd(tileX, tileY);
            regionInspectorPanel.update(snapshot, selectionState);
        } else if (painting && event.isPrimaryButtonDown()) {
            applyBrushIfDue(tileX, tileY);
        }
    }

    private void handleRelease(MouseEvent event) {
        if (event.getButton() == MouseButton.SECONDARY || event.getButton() == MouseButton.MIDDLE) {
            panning = false;
            return;
        }
        RenderSnapshot snapshot = engine.getLatestSnapshot();
        if (snapshot != null) {
            int[] coords = toTile(event.getX(), event.getY(), snapshot);
            int tileX = coords[0];
            int tileY = coords[1];
            if (selectingRegion) {
                selectionState.updateRegionEnd(tileX, tileY);
                updateSelectionPanels(snapshot, tileX, tileY, event);
                regionInspectorPanel.update(snapshot, selectionState);
            }
        }
        selectingRegion = false;
        painting = false;
    }

    private void handleScroll(ScrollEvent event) {
        double factor = event.getDeltaY() > 0 ? 1.1 : 0.9;
        double width = extractCanvasWidth(event);
        double height = extractCanvasHeight(event);
        camera.zoomAt(factor, event.getX(), event.getY(), width, height);
    }

    private void startPainting(int tileX, int tileY) {
        painting = true;
        lastPaintMillis = 0;
        applyBrushIfDue(tileX, tileY);
    }

    private void startEmitterPainting(int tileX, int tileY) {
        painting = true;
        lastPaintMillis = 0;
        applyEmitterIfDue(tileX, tileY);
    }

    private void applyBrushIfDue(int tileX, int tileY) {
        long now = System.currentTimeMillis();
        if (now - lastPaintMillis < UIConfig.BRUSH_INTERVAL_MS) {
            return;
        }
        lastPaintMillis = now;
        if (currentTool == ToolMode.AGENT) {
            engine.queueSpawnCommand(new SpawnAgentsCommand(tileX, tileY, brushRadius, agentSpawnCount, System.nanoTime()));
            return;
        }
        BrushType type = switch (currentTool) {
            case FOOD -> BrushType.FOOD;
            case HAZARD -> BrushType.HAZARD;
            case ERASE -> BrushType.ERASE;
            default -> currentBrush;
        };
        engine.queueBrushCommand(new PlaceFieldBrushCommand(type, tileX, tileY, brushRadius, System.nanoTime()));
    }

    private void applyEmitterIfDue(int tileX, int tileY) {
        long now = System.currentTimeMillis();
        if (now - lastPaintMillis < UIConfig.BRUSH_INTERVAL_MS) {
            return;
        }
        lastPaintMillis = now;
        if (emitterRemoving) {
            engine.queueRemoveEmitterCommand(new RemoveEmitterCommand(tileX, tileY));
        } else {
            engine.queuePlaceEmitterCommand(new PlaceEmitterCommand(tileX, tileY, SimConfig.EMITTER_DEFAULT_RADIUS,
                    SimConfig.EMITTER_DEFAULT_STRENGTH, true));
        }
    }

    private void updateSelectionPanels(RenderSnapshot snapshot, int tileX, int tileY, MouseEvent event) {
        int idx = MathUtil.index(tileX, tileY, snapshot.getWidth());
        selectionState.setSelectedTile(tileX, tileY);
        tileHoverPanel.updateSelection(tileX, tileY, snapshot.getFood()[idx], snapshot.getHazard()[idx], snapshot.getCrowding()[idx],
                snapshot.getAgentCounts()[idx]);
        double worldX = camera.screenToWorldX(event.getX());
        double worldY = camera.screenToWorldY(event.getY());
        long closestId = findAgentAt(snapshot, tileX, tileY, worldX, worldY);
        selectionState.setSelectedAgentId(closestId);
        engine.queueSelectedAgentCommand(new SetSelectedAgentCommand(closestId));
        sendRegionSelection();
        agentInspectorPanel.update(snapshot, closestId);
    }

    private void selectAgentFromRegionList(long agentId) {
        selectionState.setSelectedAgentId(agentId);
        engine.queueSelectedAgentCommand(new SetSelectedAgentCommand(agentId));
        RenderSnapshot snapshot = engine.getLatestSnapshot();
        if (snapshot != null) {
            int count = snapshot.getAgentCount();
            long[] ids = snapshot.getAgentId();
            int[] xs = snapshot.getAgentX();
            int[] ys = snapshot.getAgentY();
            for (int i = 0; i < count; i++) {
                if (ids[i] == agentId) {
                    selectionState.setSelectedTile(xs[i], ys[i]);
                    int idx = MathUtil.index(xs[i], ys[i], snapshot.getWidth());
                    tileHoverPanel.updateSelection(xs[i], ys[i], snapshot.getFood()[idx], snapshot.getHazard()[idx],
                            snapshot.getCrowding()[idx], snapshot.getAgentCounts()[idx]);
                    break;
                }
            }
            agentInspectorPanel.update(snapshot, agentId);
        }
    }

    private void sendRegionSelection() {
        if (selectionState.hasRegion()) {
            engine.queueSelectedRegionCommand(new SetSelectedRegionCommand(selectionState.getRegionStartX(),
                    selectionState.getRegionStartY(), selectionState.getRegionEndX(), selectionState.getRegionEndY()));
        } else {
            engine.queueSelectedRegionCommand(new SetSelectedRegionCommand(-1, -1, -1, -1));
        }
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
        selectionState.clearRegion();
        sendRegionSelection();
        tileHoverPanel.clearSelection();
        agentInspectorPanel.clear();
        regionInspectorPanel.clear();
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
