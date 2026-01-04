package simcore.agents;

import org.junit.jupiter.api.Test;
import simcore.config.SimConfig;
import simcore.world.WorldGrid;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BasicNourishmentPolicyTest {

    @Test
    void consumesFoodAndRestoresStatsWithinBounds() {
        float[] food = new float[]{0.1f, 0f, 0f, 0f};
        float[] hazard = new float[]{0f, 0f, 0f, 0f};
        boolean[] water = new boolean[]{false, false, false, false};
        WorldGrid world = new WorldGrid(2, 2, food, hazard, water);
        AgentState agent = new AgentState(new AgentId(5), 0, 0, SimConfig.INITIAL_ENERGY, 0);
        agent.applyTick(0f, -0.6f, 0f, 0f); // lower hunger below threshold

        BasicNourishmentPolicy policy = new BasicNourishmentPolicy();
        policy.applyNutrition(agent, world);

        float expectedFood = 0.1f - Math.min(0.1f, SimConfig.FOOD_CONSUME_RATE);
        assertEquals(expectedFood, food[0], 1e-6f);
        assertEquals(Math.min(1f, SimConfig.INITIAL_HUNGER - 0.6f + SimConfig.FOOD_CONSUME_RATE * SimConfig.FOOD_TO_HUNGER_GAIN),
                agent.getHunger(), 1e-6f);
        assertEquals(Math.min(1f, SimConfig.INITIAL_ENERGY + SimConfig.FOOD_CONSUME_RATE * SimConfig.FOOD_TO_ENERGY_GAIN),
                agent.getEnergy(), 1e-6f);
    }
}
