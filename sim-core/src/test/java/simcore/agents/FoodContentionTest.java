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

    private static final float FLOAT_EPSILON = 0.01f;

    @AfterEach
    void resetConfig() {
        SimConfig.LOG_SELECTED_AGENT_ENABLED = false;
        SimConfig.LOG_EVENTS_ENABLED = false;
        SimConfig.LOG_SELECTED_REGION_ENABLED = false;
    }

    @Test
    void limitedFoodIsSharedDeterministically() {
        float[] food = new float[]{0.025f, 0f};
        float[] hazard = new float[]{0f, 0f};
        boolean[] water = new boolean[]{false, false};
        WorldGrid world = new WorldGrid(2, 1, 1L, food, hazard, water);
        Outcome firstOutcome = runContentionScenario(world, 123L);

        float remainingFood = world.getFoodField()[0];
        assertTrue(remainingFood >= 0f);
        assertTrue(remainingFood <= FLOAT_EPSILON);
        assertEquals(2, firstOutcome.successCount);
        assertEquals(0, firstOutcome.failureCount);

        WorldGrid worldCopy = new WorldGrid(2, 1, 1L, new float[]{0.025f, 0f}, new float[]{0f, 0f}, new boolean[]{false, false});
        Outcome secondOutcome = runContentionScenario(worldCopy, 123L);

        assertEquals(2, secondOutcome.successCount);
        assertEquals(0, secondOutcome.failureCount);
        assertEquals(firstOutcome.hungerAfter, secondOutcome.hungerAfter);
    }

    @Test
    void foodStockDoesNotGoNegative() {
        float[] food = new float[]{0.02f};
        float[] hazard = new float[]{0f};
        boolean[] water = new boolean[]{false};
        WorldGrid world = new WorldGrid(1, 1, 2L, food, hazard, water);
        AgentSystem system = new AgentSystem(world, 5L, 0, null);
        system.spawnAgents(world, 0, 0, 1, 2, new Random(2));
        for (AgentState agent : system.getAgents()) {
            agent.applyTick(0f, -0.9f, 0f);
        }

        system.tick(world, 0);
        system.tick(world, 1);

        float foodAfterTicks = world.getFoodField()[0];
        assertTrue(foodAfterTicks >= 0f);
        assertTrue(foodAfterTicks <= FLOAT_EPSILON);
    }

    @Test
    void partialConsumptionDrainsTileToZero() {
        float[] food = new float[]{0.01f};
        float[] hazard = new float[]{0f};
        boolean[] water = new boolean[]{false};
        WorldGrid world = new WorldGrid(1, 1, 2L, food, hazard, water);
        AgentSystem system = new AgentSystem(world, 7L, 0, null);
        system.spawnAgents(world, 0, 0, 1, 1, new Random(3));
        AgentState agent = system.getAgents().get(0);
        agent.applyTick(0f, -0.9f, 0f);
        float hungerBeforeTick = agent.getHunger();

        system.tick(world, 0);

        float expectedHunger = Math.min(1f, hungerBeforeTick - SimConfig.HUNGER_DRAIN_PER_TICK + 0.01f * SimConfig.FOOD_TO_HUNGER_GAIN);
        float remainingFood = world.getFoodField()[0];
        assertTrue(remainingFood >= 0f);
        assertTrue(remainingFood <= FLOAT_EPSILON);
        assertEquals(expectedHunger, agent.getHunger(), 1e-6f);
        assertTrue(agent.getHunger() > hungerBeforeTick);
    }

    private Outcome runContentionScenario(WorldGrid world, long seed) {
        AgentSystem system = new AgentSystem(world, seed, 0, null);
        system.spawnAgents(world, 0, 0, 1, 2, new Random(1));

        Map<AgentId, Float> hungerBeforeTick = new HashMap<>();
        float normalizedHunger = 0.10f;
        for (AgentState agent : system.getAgents()) {
            agent.moveTo(0, 0);
            agent.applyTick(0f, normalizedHunger - agent.getHunger(), 0f);
            hungerBeforeTick.put(agent.getId(), agent.getHunger());
        }

        system.tick(world, 0);

        Map<AgentId, Float> hungerAfterTick = new HashMap<>();
        long successCount = 0;
        long failureCount = 0;
        for (AgentState agent : system.getAgents()) {
            hungerAfterTick.put(agent.getId(), agent.getHunger());
            float before = hungerBeforeTick.get(agent.getId());
            if (agent.getHunger() > before) {
                successCount++;
            } else if (agent.getHunger() < before) {
                failureCount++;
            }
        }

        return new Outcome(successCount, failureCount, hungerAfterTick);
    }

    private static final class Outcome {
        final long successCount;
        final long failureCount;
        final Map<AgentId, Float> hungerAfter;

        Outcome(long successCount, long failureCount, Map<AgentId, Float> hungerAfter) {
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.hungerAfter = hungerAfter;
        }
    }
}
