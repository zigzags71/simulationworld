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
    private int totalDeaths;

    public AgentSystem(WorldGrid world, long seed) {
        this.random = new Random(seed);
        this.agents = new ArrayList<>(SimConfig.NUM_AGENTS);
        this.movementPolicy = new RandomWalkMovement(seed + 7);
        this.nourishmentPolicy = new BasicNourishmentPolicy();
        spawnAgents(world);
    }

    private void spawnAgents(WorldGrid world) {
        int w = world.getWidth();
        int h = world.getHeight();
        boolean[] water = world.getWaterMask();
        int spawned = 0;
        while (spawned < SimConfig.NUM_AGENTS) {
            int x = random.nextInt(w);
            int y = random.nextInt(h);
            if (!water[MathUtil.index(x, y, w)]) {
                AgentState agent = new AgentState(new AgentId(spawned), x, y, SimConfig.INITIAL_ENERGY, 0);
                agents.add(agent);
                spawned++;
            }
        }
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
