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
    private final Random random;

    public AgentSystem(WorldGrid world, long seed) {
        this.random = new Random(seed);
        this.agents = new ArrayList<>(SimConfig.NUM_AGENTS);
        this.movementPolicy = new RandomWalkMovement(seed + 7);
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
                AgentState agent = new AgentState(new AgentId(spawned), x, y, 1.0f, 0);
                agents.add(agent);
                spawned++;
            }
        }
    }

    public void tick(WorldGrid world) {
        for (AgentState agent : agents) {
            movementPolicy.move(agent, world);
            agent.updateStats(-0.001f, 0.001f, -0.0005f, random.nextFloat() * 0.002f);
        }
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
