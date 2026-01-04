package simcore.agents;

import simcore.config.SimConfig;
import simcore.util.MathUtil;
import simcore.world.WorldGrid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class AgentSystem {
    private final List<AgentState> agents;
    private final MovementPolicy movementPolicy;
    private final NourishmentPolicy nourishmentPolicy;
    private final Random random;
    private long nextId;
    private int totalDeaths;

    public AgentSystem(WorldGrid world, long seed) {
        this(world, seed, SimConfig.NUM_AGENTS);
    }

    public AgentSystem(WorldGrid world, long seed, int initialPopulation) {
        this.random = new Random(seed);
        this.agents = new ArrayList<>(Math.max(initialPopulation, 16));
        this.movementPolicy = new RandomWalkMovement(seed + 7);
        this.nourishmentPolicy = new BasicNourishmentPolicy();
        this.nextId = 0;
        spawnInitialAgents(world, initialPopulation);
    }

    public AgentTickMetrics tick(WorldGrid world) {
        AgentTickMetrics metrics = new AgentTickMetrics();
        int width = world.getWidth();
        float[] hazard = world.getHazardField();
        for (int i = agents.size() - 1; i >= 0; i--) {
            AgentState agent = agents.get(i);
            movementPolicy.move(agent, world);
            int idx = MathUtil.index(agent.getX(), agent.getY(), width);
            float hazardHere = hazard[idx];
            float energyDelta = -SimConfig.ENERGY_DRAIN_PER_TICK - hazardHere * SimConfig.HAZARD_ENERGY_DRAIN_PER_TICK;
            float hungerDelta = -SimConfig.HUNGER_DRAIN_PER_TICK;
            float stressDelta = hazardHere * SimConfig.HAZARD_STRESS_GAIN_PER_TICK - SimConfig.STRESS_RECOVERY_PER_TICK;
            float predictionDelta = (random.nextFloat() - 0.5f) * SimConfig.PREDICTION_ERROR_JITTER;
            agent.applyTick(energyDelta, hungerDelta, stressDelta, predictionDelta);
            nourishmentPolicy.applyNutrition(agent, world);
            if (agent.isDead()) {
                agents.remove(i);
                totalDeaths++;
                metrics.markDeath();
                continue;
            }
            metrics.accumulate(agent, hazardHere);
        }
        metrics.setTotalDeaths(totalDeaths);
        return metrics;
    }

    private void spawnInitialAgents(WorldGrid world, int initialPopulation) {
        int w = world.getWidth();
        int h = world.getHeight();
        boolean[] water = world.getWaterMask();
        int spawned = 0;
        while (spawned < initialPopulation) {
            int x = random.nextInt(w);
            int y = random.nextInt(h);
            if (!water[MathUtil.index(x, y, w)]) {
                AgentState agent = new AgentState(new AgentId(nextId++), x, y, SimConfig.INITIAL_ENERGY, spawned);
                agents.add(agent);
                spawned++;
            }
        }
    }

    public int spawnAgents(WorldGrid world, int centerX, int centerY, int radius, int count, Random rand) {
        if (count <= 0 || radius <= 0) {
            return 0;
        }
        int width = world.getWidth();
        int height = world.getHeight();
        boolean[] water = world.getWaterMask();
        int minX = Math.max(0, centerX - radius);
        int maxX = Math.min(width - 1, centerX + radius);
        int minY = Math.max(0, centerY - radius);
        int maxY = Math.min(height - 1, centerY + radius);
        int radiusSq = radius * radius;
        List<Integer> candidates = new ArrayList<>();
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                int dx = x - centerX;
                int dy = y - centerY;
                if (dx * dx + dy * dy <= radiusSq) {
                    int idx = MathUtil.index(x, y, width);
                    if (!water[idx]) {
                        candidates.add(idx);
                    }
                }
            }
        }
        if (candidates.isEmpty()) {
            return 0;
        }
        Collections.shuffle(candidates, rand);
        int spawned = 0;
        int cultureBase = rand.nextInt(Integer.MAX_VALUE);
        for (int i = 0; i < candidates.size() && spawned < count; i++) {
            int idx = candidates.get(i);
            int x = idx % width;
            int y = idx / width;
            AgentState agent = new AgentState(new AgentId(nextId++), x, y, SimConfig.INITIAL_ENERGY, cultureBase + spawned);
            agents.add(agent);
            spawned++;
        }
        return spawned;
    }

    public List<AgentState> getAgents() {
        return Collections.unmodifiableList(agents);
    }

    public AgentState findAgentById(long id) {
        for (AgentState agent : agents) {
            if (agent.getId().value() == id) {
                return agent;
            }
        }
        return null;
    }
}
