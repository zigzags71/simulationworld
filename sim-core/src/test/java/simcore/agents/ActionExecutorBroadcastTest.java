package simcore.agents;

import org.junit.jupiter.api.Test;
import simcore.config.SimConfig;
import simcore.rules.ActionType;
import simcore.rules.OutcomeVector;
import simcore.world.WorldGrid;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionExecutorBroadcastTest {

    @Test
    void broadcastCostsOnlyWhenSignalCreated() {
        float[] food = new float[9];
        float[] hazard = new float[9];
        boolean[] water = new boolean[9];
        WorldGrid world = new WorldGrid(3, 3, 1L, food, hazard, water);
        AgentState agent = AgentState.forTest(new AgentId(1), 1, 1, SimConfig.INITIAL_ENERGY,
                SimConfig.INITIAL_HUNGER, SimConfig.INITIAL_STRESS, 0f);
        ActionExecutor executor = new ActionExecutor(new Random(1L));

        OutcomeVector delta = executor.execute(ActionType.BROADCAST_SIGNAL, agent, world, 0, 0);

        assertEquals(OutcomeVector.zero().getDeltaEnergy(), delta.getDeltaEnergy());
        assertEquals(OutcomeVector.zero().getDeltaHunger(), delta.getDeltaHunger());
        assertTrue(world.getSignalField().getSignals().isEmpty());
    }

    @Test
    void broadcastAppliesCostWhenSignalCreated() {
        float[] food = new float[9];
        float[] hazard = new float[9];
        boolean[] water = new boolean[9];
        WorldGrid world = new WorldGrid(3, 3, 2L, food, hazard, water);
        world.addEmitter(1, 1, 1, 0.1f, true);
        AgentState agent = AgentState.forTest(new AgentId(2), 1, 1, SimConfig.INITIAL_ENERGY,
                SimConfig.INITIAL_HUNGER, SimConfig.INITIAL_STRESS, 0f);
        ActionExecutor executor = new ActionExecutor(new Random(2L));

        OutcomeVector delta = executor.execute(ActionType.BROADCAST_SIGNAL, agent, world, 0, 0);

        assertEquals(-SimConfig.SIGNAL_BROADCAST_COST_ENERGY, delta.getDeltaEnergy());
        assertEquals(-SimConfig.SIGNAL_BROADCAST_COST_HUNGER, delta.getDeltaHunger());
        assertEquals(1, world.getSignalField().getSignals().size());
    }
}
