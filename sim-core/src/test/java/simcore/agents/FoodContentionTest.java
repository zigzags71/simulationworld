package simcore.agents;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import simcore.config.SimConfig;
import simcore.world.WorldGrid;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        WorldGrid world = new WorldGrid(2, 1, 1L, food, hazard, water);
        Outcome firstOutcome = runContentionScenario(world, 123L);

        assertEquals(0.01f, world.getFoodField()[0], 1e-6f);
        assertEquals(1, firstOutcome.successCount);
        assertEquals(1, firstOutcome.failureCount);

        WorldGrid worldCopy = new WorldGrid(2, 1, 1L, new float[]{0.025f, 0f}, new float[]{0f, 0f}, new boolean[]{false, false});
        Outcome secondOutcome = runContentionScenario(worldCopy, 123L);

        assertEquals(1, secondOutcome.successCount);
        assertEquals(1, secondOutcome.failureCount);
    }

    @Test
    void foodStockDoesNotGoNegative() {
        float[] food = new float[]{0.01f};
        float[] hazard = new float[]{0f};
        boolean[] water = new boolean[]{false};
        WorldGrid world = new WorldGrid(1, 1, 2L, food, hazard, water);
        AgentSystem system = new AgentSystem(world, 5L, 0, null);
        system.spawnAgents(world, 0, 0, 1, 1, new Random(2));
        AgentState agent = system.getAgents().get(0);
        agent.applyTick(0f, -0.9f, 0f);

        system.tick(world, 0);
        system.tick(world, 1);

        float foodAfterTicks = world.getFoodField()[0];
        assertEquals(food[0], foodAfterTicks, 1e-6f);
        assertTrue(foodAfterTicks >= 0f);
    }

    private Outcome runContentionScenario(WorldGrid world, long seed) {
        AgentSystem system = new AgentSystem(world, seed, 0, null);
        system.spawnAgents(world, 0, 0, 1, 2, new Random(1));

        Map<AgentId, Float> hungerBeforeTick = new HashMap<>();
        for (AgentState agent : system.getAgents()) {
            agent.moveTo(0, 0);
            agent.applyTick(0f, -0.9f, 0f);
            hungerBeforeTick.put(agent.getId(), agent.getHunger());
        }

        system.tick(world, 0);

        long successCount = system.getAgents().stream()
                .filter(a -> a.getHunger() > hungerBeforeTick.get(a.getId()))
                .count();
        long failureCount = system.getAgents().stream()
                .filter(a -> a.getHunger() < hungerBeforeTick.get(a.getId()))
                .count();

        return new Outcome(successCount, failureCount);
    }

    private static final class Outcome {
        final long successCount;
        final long failureCount;

        Outcome(long successCount, long failureCount) {
            this.successCount = successCount;
            this.failureCount = failureCount;
        }
    }
}
