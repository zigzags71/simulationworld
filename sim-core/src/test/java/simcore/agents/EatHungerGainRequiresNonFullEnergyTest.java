package simcore.agents;

import org.junit.jupiter.api.Test;
import simcore.config.SimConfig;
import simcore.rules.ActionType;
import simcore.rules.OutcomeVector;
import simcore.world.WorldGrid;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EatHungerGainRequiresNonFullEnergyTest {

    @Test
    void hungerGainBlockedWhenEnergyFull() {
        float[] food = new float[]{0.2f};
        float[] hazard = new float[]{0f};
        boolean[] water = new boolean[]{false};
        WorldGrid world = new WorldGrid(1, 1, 10L, food, hazard, water);
        ActionExecutor executor = new ActionExecutor(new Random(1L));
        executor.beginTick(1);
        OutcomeVector[] deltas = new OutcomeVector[1];
        AgentState agent = AgentState.forTest(new AgentId(1), 0, 0, 1f, 0.1f, 0f, 0f);

        executor.execute(ActionType.EAT, agent, world, 0, 0);
        executor.resolveEatRequests(world, deltas, null);

        OutcomeVector delta = deltas[0];
        assertEquals(0f, delta.getDeltaHunger(), 1e-6f);
        assertTrue(delta.getDeltaEnergy() > 0f);
    }

    @Test
    void hungerGainAppliedWhenEnergyNotFull() {
        float[] food = new float[]{0.2f};
        float[] hazard = new float[]{0f};
        boolean[] water = new boolean[]{false};
        WorldGrid world = new WorldGrid(1, 1, 11L, food, hazard, water);
        ActionExecutor executor = new ActionExecutor(new Random(2L));
        executor.beginTick(1);
        OutcomeVector[] deltas = new OutcomeVector[1];
        AgentState agent = AgentState.forTest(new AgentId(2), 0, 0, 0.5f, 0.1f, 0f, 0f);

        executor.execute(ActionType.EAT, agent, world, 0, 0);
        executor.resolveEatRequests(world, deltas, null);

        OutcomeVector delta = deltas[0];
        assertTrue(delta.getDeltaHunger() > 0f);
        assertTrue(delta.getDeltaEnergy() > 0f);
        assertEquals(SimConfig.FOOD_TO_HUNGER_GAIN * SimConfig.FOOD_CONSUME_RATE, delta.getDeltaHunger(), 1e-6f);
    }
}
