package simcore.agents;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import simcore.config.SimConfig;
import simcore.world.WorldGrid;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FoodContentionTest {

    @AfterEach
    void resetConfig() {
        SimConfig.LOG_SELECTED_AGENT_ENABLED = false;
        SimConfig.LOG_EVENTS_ENABLED = false;
        SimConfig.LOG_SELECTED_REGION_ENABLED = false;
    }

    @Test
    void onlyOneAgentConsumesWhenFoodIsLimited() {
        float[] food = new float[]{0.025f, 0f};
        float[] hazard = new float[]{0f, 0f};
        boolean[] water = new boolean[]{false, false};
        WorldGrid world = new WorldGrid(2, 1, food, hazard, water);
        AgentSystem system = new AgentSystem(world, 123L, 0, null);
        system.spawnAgents(world, 0, 0, 1, 2, new Random(1));
        for (AgentState agent : system.getAgents()) {
            agent.moveTo(0, 0);
            agent.applyTick(0f, -0.9f, 0f);
        }

        system.tick(world, 0);

        assertEquals(2, system.getAgents().size());
        assertEquals(0.01f, world.getFoodField()[0], 1e-6f);

        long successCount = system.getAgents().stream().filter(a -> a.getEnergy() > SimConfig.INITIAL_ENERGY).count();
        long failureCount = system.getAgents().stream().filter(a -> a.getEnergy() < SimConfig.INITIAL_ENERGY).count();

        WorldGrid worldCopy = new WorldGrid(2, 1, new float[]{0.025f, 0f}, new float[]{0f, 0f}, new boolean[]{false, false});
        AgentSystem second = new AgentSystem(worldCopy, 123L, 0, null);
        second.spawnAgents(worldCopy, 0, 0, 1, 2, new Random(1));
        for (AgentState agent : second.getAgents()) {
            agent.moveTo(0, 0);
            agent.applyTick(0f, -0.9f, 0f);
        }
        second.tick(worldCopy, 0);

        long secondSuccess = second.getAgents().stream().filter(a -> a.getEnergy() > SimConfig.INITIAL_ENERGY).count();
        long secondFailure = second.getAgents().stream().filter(a -> a.getEnergy() < SimConfig.INITIAL_ENERGY).count();

        assertEquals(1, successCount);
        assertEquals(1, failureCount);
        assertEquals(1, secondSuccess);
        assertEquals(1, secondFailure);
    }
}
