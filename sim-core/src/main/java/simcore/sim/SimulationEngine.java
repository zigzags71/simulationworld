package simcore.sim;

import simcore.agents.AgentState;
import simcore.agents.AgentSystem;
import simcore.agents.AgentTickMetrics;
import simcore.config.MapGenConfig;
import simcore.config.SimConfig;
import simcore.events.TelemetryBus;
import simcore.events.TelemetryEvent;
import simcore.sim.commands.PlaceFieldBrushCommand;
import simcore.snapshot.RenderSnapshot;
import simcore.snapshot.SnapshotBuffer;
import simcore.util.ColorUtil;
import simcore.util.FieldBrushApplier;
import simcore.util.MathUtil;
import simcore.world.WorldGrid;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class SimulationEngine {
    private WorldGrid world;
    private AgentSystem agents;
    private final SnapshotBuffer snapshotBuffer;
    private final TelemetryBus telemetryBus;
    private final TickLoop tickLoop;
    private final ExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean loopStarted = new AtomicBoolean(false);
    private final ConcurrentLinkedQueue<PlaceFieldBrushCommand> brushQueue = new ConcurrentLinkedQueue<>();
    private MapGenConfig mapGenConfig;
    private long tickIndex = 0;

    public SimulationEngine(long seed, TelemetryBus telemetryBus) {
        this(MapGenConfig.defaults().withSeed(seed), telemetryBus);
    }

    public SimulationEngine(MapGenConfig mapGenConfig, TelemetryBus telemetryBus) {
        this.mapGenConfig = mapGenConfig;
        this.world = WorldGrid.generate(mapGenConfig);
        this.agents = new AgentSystem(world, mapGenConfig.getSeed() + 99);
        this.snapshotBuffer = new SnapshotBuffer(SimConfig.WORLD_W, SimConfig.WORLD_H, SimConfig.NUM_AGENTS);
        this.telemetryBus = telemetryBus;
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
        this.agents = new AgentSystem(world, newConfig.getSeed() + 99);
        this.tickIndex = 0;
        brushQueue.clear();
        writeSnapshot();
    }

    public void reset() {
        regenerate(MapGenConfig.defaults());
    }

    public void queueBrushCommand(PlaceFieldBrushCommand command) {
        brushQueue.add(command);
    }

    private void step() {
        boolean dirty = applyBrushCommands();
        AgentTickMetrics metrics = null;
        if (running.get()) {
            metrics = agents.tick(world);
            tickIndex++;
            dirty = true;
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

    private void writeSnapshot() {
        RenderSnapshot back = snapshotBuffer.beginWrite();
        System.arraycopy(world.getFoodField(), 0, back.getFood(), 0, back.getFood().length);
        System.arraycopy(world.getHazardField(), 0, back.getHazard(), 0, back.getHazard().length);
        Arrays.fill(back.getAgentCounts(), 0);
        List<AgentState> agentStates = agents.getAgents();
        int index = 0;
        for (AgentState agent : agentStates) {
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
            index++;
        }
        for (int i = index; i < back.getAgentX().length; i++) {
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
        snapshotBuffer.setAgentCount(agentStates.size());
        snapshotBuffer.publish(tickIndex);
    }

    public TelemetryBus getTelemetryBus() {
        return telemetryBus;
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
}
