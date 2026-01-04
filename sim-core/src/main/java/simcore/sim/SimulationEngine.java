package simcore.sim;

import simcore.agents.AgentState;
import simcore.agents.AgentSystem;
import simcore.config.SimConfig;
import simcore.events.TelemetryBus;
import simcore.events.TelemetryEvent;
import simcore.snapshot.RenderSnapshot;
import simcore.snapshot.SnapshotBuffer;
import simcore.util.ColorUtil;
import simcore.util.MathUtil;
import simcore.world.WorldGrid;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class SimulationEngine {
    private final WorldGrid world;
    private final AgentSystem agents;
    private final SnapshotBuffer snapshotBuffer;
    private final TelemetryBus telemetryBus;
    private final TickLoop tickLoop;
    private final ExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private long tickIndex = 0;

    public SimulationEngine(long seed, TelemetryBus telemetryBus) {
        this.world = WorldGrid.generate(seed);
        this.agents = new AgentSystem(world, seed + 99);
        this.snapshotBuffer = new SnapshotBuffer(SimConfig.WORLD_W, SimConfig.WORLD_H, SimConfig.NUM_AGENTS);
        this.telemetryBus = telemetryBus;
        this.tickLoop = new TickLoop(SimConfig.TICK_RATE, this::step);
        this.executorService = Executors.newSingleThreadExecutor(r -> new Thread(r, "sim-tick"));
        writeSnapshot();
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            executorService.submit(tickLoop);
        }
    }

    public void stop() {
        running.set(false);
        tickLoop.stop();
        executorService.shutdownNow();
    }

    public RenderSnapshot getLatestSnapshot() {
        return snapshotBuffer.getLatest();
    }

    private void step() {
        if (!running.get()) {
            return;
        }
        agents.tick(world);
        tickIndex++;
        writeSnapshot();
        float meanPredictionError = computeMeanPredictionError(agents.getAgents());
        telemetryBus.publish(new TelemetryEvent(tickIndex, agents.getAgents().size(), meanPredictionError));
    }

    private float computeMeanPredictionError(List<AgentState> agentsList) {
        float sum = 0f;
        for (AgentState agent : agentsList) {
            sum += agent.getPredictionError();
        }
        return agentsList.isEmpty() ? 0f : sum / agentsList.size();
    }

    private void writeSnapshot() {
        RenderSnapshot back = snapshotBuffer.beginWrite();
        System.arraycopy(world.getFoodField(), 0, back.getFood(), 0, back.getFood().length);
        System.arraycopy(world.getHazardField(), 0, back.getHazard(), 0, back.getHazard().length);
        int[] crowdCounts = new int[back.getCrowding().length];
        List<AgentState> agentStates = agents.getAgents();
        int index = 0;
        for (AgentState agent : agentStates) {
            back.getAgentX()[index] = agent.getX();
            back.getAgentY()[index] = agent.getY();
            back.getAgentColorARGB()[index] = ColorUtil.colorFromSeed(agent.getCultureId());
            back.getAgentId()[index] = agent.getId().value();
            int idx = MathUtil.index(agent.getX(), agent.getY(), world.getWidth());
            crowdCounts[idx] += 1;
            index++;
        }
        for (int i = index; i < back.getAgentX().length; i++) {
            back.getAgentX()[i] = -1;
            back.getAgentY()[i] = -1;
            back.getAgentColorARGB()[i] = 0;
            back.getAgentId()[i] = -1;
        }
        for (int i = 0; i < crowdCounts.length; i++) {
            back.getCrowding()[i] = crowdCounts[i] / 5f;
        }
        snapshotBuffer.setAgentCount(agentStates.size());
        snapshotBuffer.publish(tickIndex);
    }

    public TelemetryBus getTelemetryBus() {
        return telemetryBus;
    }

    public WorldGrid getWorld() {
        return world;
    }

    public AgentState findAgentById(long id) {
        return agents.findAgentById(id);
    }
}
