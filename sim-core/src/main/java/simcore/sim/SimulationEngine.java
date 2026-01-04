package simcore.sim;

import simcore.agents.AgentState;
import simcore.agents.AgentSystem;
import simcore.agents.AgentTickMetrics;
import simcore.config.MapGenConfig;
import simcore.config.SimConfig;
import simcore.events.AgentDeselectedEvent;
import simcore.events.EventBus;
import simcore.events.SelectedAgentSnapshotEvent;
import simcore.events.SelectedRegionSnapshotEvent;
import simcore.events.SimulationEvent;
import simcore.events.SimulationLogger;
import simcore.events.TelemetryBus;
import simcore.events.TelemetryEvent;
import simcore.rules.Rule;
import simcore.sim.commands.PlaceFieldBrushCommand;
import simcore.sim.commands.SpawnAgentsCommand;
import simcore.sim.commands.SetSelectedAgentCommand;
import simcore.sim.commands.SetSelectedRegionCommand;
import simcore.snapshot.RenderSnapshot;
import simcore.snapshot.RuleView;
import simcore.snapshot.SelectedAgentDetails;
import simcore.snapshot.SnapshotBuffer;
import simcore.util.ColorUtil;
import simcore.util.FieldBrushApplier;
import simcore.util.MathUtil;
import simcore.world.WorldGrid;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class SimulationEngine {
    private WorldGrid world;
    private AgentSystem agents;
    private final SnapshotBuffer snapshotBuffer;
    private final TelemetryBus telemetryBus;
    private final EventBus<SimulationEvent> eventBus;
    private final SimulationLogger logger;
    private final TickLoop tickLoop;
    private final ExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean loopStarted = new AtomicBoolean(false);
    private final ConcurrentLinkedQueue<PlaceFieldBrushCommand> brushQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<SpawnAgentsCommand> spawnQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<SetSelectedAgentCommand> selectionQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<SetSelectedRegionCommand> regionQueue = new ConcurrentLinkedQueue<>();
    private MapGenConfig mapGenConfig;
    private long selectedAgentId = -1;
    private int regionStartX = -1;
    private int regionStartY = -1;
    private int regionEndX = -1;
    private int regionEndY = -1;
    private boolean regionActive;
    private long lastRegionEventTick = -SimConfig.LOG_THROTTLE_TICKS;
    private long tickIndex = 0;

    public SimulationEngine(long seed, TelemetryBus telemetryBus) {
        this(MapGenConfig.defaults().withSeed(seed), telemetryBus);
    }

    public SimulationEngine(MapGenConfig mapGenConfig, TelemetryBus telemetryBus) {
        this.mapGenConfig = mapGenConfig;
        this.world = WorldGrid.generate(mapGenConfig);
        this.eventBus = new EventBus<>();
        this.agents = new AgentSystem(world, mapGenConfig.getSeed() + 99, SimConfig.NUM_AGENTS, eventBus);
        this.snapshotBuffer = new SnapshotBuffer(SimConfig.WORLD_W, SimConfig.WORLD_H, SimConfig.MAX_RENDERED_AGENTS);
        this.telemetryBus = telemetryBus;
        this.logger = new SimulationLogger(eventBus, this::getSelectedAgentId);
        this.tickLoop = new TickLoop(SimConfig.TICK_RATE, this::step);
        this.executorService = Executors.newSingleThreadExecutor(r -> new Thread(r, "sim-tick"));
        writeSnapshot();
    }

    public void start() {
        if (loopStarted.compareAndSet(false, true)) {
            executorService.submit(tickLoop);
        }
        running.set(true);
    }

    public void stop() {
        running.set(false);
        tickLoop.stop();
        executorService.shutdownNow();
    }

    public void pause() {
        running.set(false);
    }

    public void resume() {
        running.set(true);
    }

    public RenderSnapshot getLatestSnapshot() {
        return snapshotBuffer.getLatest();
    }

    public void regenerate(MapGenConfig newConfig) {
        running.set(false);
        this.mapGenConfig = newConfig;
        this.world = WorldGrid.generate(newConfig);
        this.agents = new AgentSystem(world, newConfig.getSeed() + 99, SimConfig.NUM_AGENTS, eventBus);
        this.tickIndex = 0;
        brushQueue.clear();
        spawnQueue.clear();
        selectionQueue.clear();
        regionQueue.clear();
        selectedAgentId = -1;
        regionActive = false;
        lastRegionEventTick = -SimConfig.LOG_THROTTLE_TICKS;
        writeSnapshot();
    }

    public void reset() {
        regenerate(MapGenConfig.defaults());
    }

    public void queueBrushCommand(PlaceFieldBrushCommand command) {
        brushQueue.add(command);
    }

    public void queueSpawnCommand(SpawnAgentsCommand command) {
        spawnQueue.add(command);
    }

    public void queueSelectedAgentCommand(SetSelectedAgentCommand command) {
        selectionQueue.add(command);
    }

    public void queueSelectedRegionCommand(SetSelectedRegionCommand command) {
        regionQueue.add(command);
    }

    private void step() {
        boolean dirty = applyBrushCommands();
        dirty |= applySpawnCommands();
        dirty |= applySelectionCommands();
        dirty |= applyRegionCommands();
        AgentTickMetrics metrics = null;
        long currentTick = tickIndex;
        if (running.get()) {
            metrics = agents.tick(world, currentTick);
            tickIndex++;
            dirty = true;
            dirty |= ensureSelectionValid(currentTick);
            emitSelectedAgentSnapshot(currentTick);
            emitRegionSnapshot(currentTick);
        }
        if (!running.get() && selectedAgentId >= 0) {
            emitSelectedAgentSnapshot(currentTick);
        }
        if (dirty) {
            writeSnapshot();
        }
        if (running.get() && metrics != null) {
            telemetryBus.publish(new TelemetryEvent(
                    tickIndex,
                    metrics.getPopulation(),
                    metrics.getMeanPredictionError(),
                    metrics.getDeathsThisTick(),
                    metrics.getTotalDeaths(),
                    metrics.getMeanEnergy(),
                    metrics.getMeanHunger(),
                    metrics.getMeanStress(),
                    metrics.getMeanHazard()));
        }
    }

    private void emitSelectedAgentSnapshot(long currentTick) {
        if (!SimConfig.LOG_SELECTED_AGENT_ENABLED || selectedAgentId < 0) {
            return;
        }
        AgentState agent = agents.findAgentById(selectedAgentId);
        if (agent != null) {
            eventBus.publish(new SelectedAgentSnapshotEvent(agent.getId().value(), agent.getEnergy(), agent.getHunger(),
                    agent.getStress(), agent.getPredictionError(), currentTick));
        }
    }

    private void emitRegionSnapshot(long currentTick) {
        if (!SimConfig.LOG_SELECTED_REGION_ENABLED || !regionActive) {
            return;
        }
        if (currentTick - lastRegionEventTick < SimConfig.LOG_THROTTLE_TICKS) {
            return;
        }
        lastRegionEventTick = currentTick;
        int minX = Math.min(regionStartX, regionEndX);
        int maxX = Math.max(regionStartX, regionEndX);
        int minY = Math.min(regionStartY, regionEndY);
        int maxY = Math.max(regionStartY, regionEndY);
        minX = Math.max(0, Math.min(minX, world.getWidth() - 1));
        maxX = Math.max(0, Math.min(maxX, world.getWidth() - 1));
        minY = Math.max(0, Math.min(minY, world.getHeight() - 1));
        maxY = Math.max(0, Math.min(maxY, world.getHeight() - 1));
        List<AgentState> agentStates = agents.getAgents();
        float energySum = 0f;
        float hungerSum = 0f;
        float stressSum = 0f;
        float errorSum = 0f;
        int count = 0;
        for (AgentState agent : agentStates) {
            int x = agent.getX();
            int y = agent.getY();
            if (x >= minX && x <= maxX && y >= minY && y <= maxY) {
                energySum += agent.getEnergy();
                hungerSum += agent.getHunger();
                stressSum += agent.getStress();
                errorSum += agent.getPredictionError();
                count++;
            }
        }
        float avgEnergy = count == 0 ? 0f : energySum / count;
        float avgHunger = count == 0 ? 0f : hungerSum / count;
        float avgStress = count == 0 ? 0f : stressSum / count;
        float avgError = count == 0 ? 0f : errorSum / count;
        eventBus.publish(new SelectedRegionSnapshotEvent(minX, minY, maxX, maxY, count, avgEnergy, avgHunger, avgStress,
                avgError, currentTick));
    }

    private void writeSnapshot() {
        RenderSnapshot back = snapshotBuffer.beginWrite();
        System.arraycopy(world.getFoodField(), 0, back.getFood(), 0, back.getFood().length);
        System.arraycopy(world.getHazardField(), 0, back.getHazard(), 0, back.getHazard().length);
        Arrays.fill(back.getAgentCounts(), 0);
        List<AgentState> agentStates = agents.getAgents();
        int index = 0;
        int capacity = back.getAgentX().length;
        SelectedAgentDetails selectedDetails = null;
        for (AgentState agent : agentStates) {
            if (index >= capacity) {
                break;
            }
            back.getAgentX()[index] = agent.getX();
            back.getAgentY()[index] = agent.getY();
            back.getAgentColorARGB()[index] = ColorUtil.colorFromSeed(agent.getCultureId());
            back.getAgentId()[index] = agent.getId().value();
            back.getAgentAge()[index] = agent.getAgeTicks();
            back.getAgentEnergy()[index] = agent.getEnergy();
            back.getAgentHunger()[index] = agent.getHunger();
            back.getAgentStress()[index] = agent.getStress();
            back.getAgentPredictionError()[index] = agent.getPredictionError();
            back.getAgentAwareness()[index] = agent.isAwarenessFlag();
            back.getAgentCultureId()[index] = agent.getCultureId();
            int idx = MathUtil.index(agent.getX(), agent.getY(), world.getWidth());
            back.getAgentCounts()[idx] += 1;
            if (agent.getId().value() == selectedAgentId) {
                selectedDetails = buildSelectedAgentDetails(agent);
            }
            index++;
        }
        for (int i = index; i < capacity; i++) {
            back.getAgentX()[i] = -1;
            back.getAgentY()[i] = -1;
            back.getAgentColorARGB()[i] = 0;
            back.getAgentId()[i] = -1;
            back.getAgentAge()[i] = 0;
            back.getAgentEnergy()[i] = 0f;
            back.getAgentHunger()[i] = 0f;
            back.getAgentStress()[i] = 0f;
            back.getAgentPredictionError()[i] = 0f;
            back.getAgentAwareness()[i] = false;
            back.getAgentCultureId()[i] = 0;
        }
        for (int i = 0; i < back.getCrowding().length; i++) {
            back.getCrowding()[i] = back.getAgentCounts()[i] / 5f;
        }
        snapshotBuffer.setAgentCount(Math.min(agentStates.size(), capacity));
        snapshotBuffer.setSelectedAgentDetails(selectedDetails);
        snapshotBuffer.publish(tickIndex);
    }

    public TelemetryBus getTelemetryBus() {
        return telemetryBus;
    }

    AgentSystem getAgentSystem() {
        return agents;
    }

    long getSelectedAgentIdValue() {
        return selectedAgentId;
    }

    void stepOnce() {
        step();
    }

    private long getSelectedAgentId() {
        return selectedAgentId;
    }

    private boolean applyBrushCommands() {
        boolean changed = false;
        PlaceFieldBrushCommand command;
        float foodBaseline = 0.25f + mapGenConfig.getFoodRichness() * 0.75f;
        float hazardBaseline = 0.2f + mapGenConfig.getHazardBaseline() * 0.6f;
        while ((command = brushQueue.poll()) != null) {
            changed |= FieldBrushApplier.apply(command, world.getFoodField(), world.getHazardField(), world.getWidth(), world.getHeight(),
                    foodBaseline, hazardBaseline);
        }
        return changed;
    }

    private boolean applySpawnCommands() {
        boolean changed = false;
        SpawnAgentsCommand command;
        while ((command = spawnQueue.poll()) != null) {
            Random rand = new Random(command.getSeed());
            int spawned = agents.spawnAgents(world, command.getCenterX(), command.getCenterY(), command.getRadius(), command.getCount(), rand);
            changed |= spawned > 0;
        }
        return changed;
    }

    private boolean applySelectionCommands() {
        boolean changed = false;
        SetSelectedAgentCommand command;
        while ((command = selectionQueue.poll()) != null) {
            selectedAgentId = command.getAgentId();
            changed = true;
        }
        return changed;
    }

    private boolean applyRegionCommands() {
        boolean changed = false;
        SetSelectedRegionCommand command;
        while ((command = regionQueue.poll()) != null) {
            regionStartX = command.getStartX();
            regionStartY = command.getStartY();
            regionEndX = command.getEndX();
            regionEndY = command.getEndY();
            regionActive = regionStartX >= 0 && regionStartY >= 0 && regionEndX >= 0 && regionEndY >= 0;
            changed = true;
        }
        return changed;
    }

    private boolean ensureSelectionValid(long currentTick) {
        if (selectedAgentId < 0) {
            return false;
        }
        AgentState agent = agents.findAgentById(selectedAgentId);
        if (agent == null) {
            selectedAgentId = -1;
            eventBus.publish(new AgentDeselectedEvent(currentTick));
            return true;
        }
        return false;
    }

    private SelectedAgentDetails buildSelectedAgentDetails(AgentState agent) {
        List<Rule> rules = agent.getRulebook();
        RuleView[] views = new RuleView[rules.size()];
        for (int i = 0; i < rules.size(); i++) {
            Rule rule = rules.get(i);
            views[i] = new RuleView(rule.getRuleId(), rule.getType(), rule.getContextKey().toString(), rule.getAction(),
                    rule.getTrust(), rule.getUses(), rule.getSuccesses(), rule.getLastUsedTick(), rule.getLastError());
        }
        return new SelectedAgentDetails(agent.getId().value(), agent.getX(), agent.getY(), agent.getAgeTicks(), agent.getEnergy(), agent.getHunger(),
                agent.getStress(), agent.getPredictionError(), agent.isAwarenessFlag(), views);
    }
}
