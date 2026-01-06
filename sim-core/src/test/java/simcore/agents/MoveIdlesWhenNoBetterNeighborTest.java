package simcore.agents;

import org.junit.jupiter.api.Test;
import simcore.config.SimConfig;
import simcore.rules.ActionType;
import simcore.rules.OutcomeVector;
import simcore.world.WorldGrid;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoveIdlesWhenNoBetterNeighborTest {

    @Test
    void staysInPlaceWhenNeighborsAreNotBetter() {
        float[] food = new float[]{
                0.6f, 0.6f, 0.6f,
                0.6f, 0.8f, 0.6f,
                0.6f, 0.6f, 0.6f
        };
        float[] hazard = new float[9];
        boolean[] water = new boolean[9];
        WorldGrid world = new WorldGrid(3, 3, 21L, food, hazard, water);
        AgentState agent = AgentState.forTest(new AgentId(1), 1, 1, SimConfig.INITIAL_ENERGY,
                SimConfig.INITIAL_HUNGER, SimConfig.INITIAL_STRESS, 0f);
        ActionExecutor executor = new ActionExecutor(new Random(3L));

        OutcomeVector delta = executor.execute(ActionType.MOVE, agent, world, 0, 0);

        assertEquals(1, agent.getX());
        assertEquals(1, agent.getY());
        OutcomeVector expectedIdle = new OutcomeVector(0f, 0f, -SimConfig.IDLE_STRESS_RECOVERY_BONUS);
        assertEquals(expectedIdle, delta);
        assertTrue(agent.getHunger() <= SimConfig.INITIAL_HUNGER);
        assertTrue(agent.getEnergy() <= SimConfig.INITIAL_ENERGY);
    }
}
