package simcore.agents;

import org.junit.jupiter.api.Test;
import simcore.config.SimConfig;
import simcore.rules.ActionType;
import simcore.rules.OutcomeVector;
import simcore.util.BinningUtil;
import simcore.world.WorldGrid;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActionExecutorMetricsTest {

    @Test
    void broadcastIncrementsSignalCounterOnFirstTick() {
        float[] food = new float[9];
        float[] hazard = new float[9];
        boolean[] water = new boolean[9];
        WorldGrid world = new WorldGrid(3, 3, 5L, food, hazard, water);
        world.addEmitter(1, 1, 1, 0.2f, true);
        AgentState agent = AgentState.forTest(new AgentId(1), 1, 1, SimConfig.INITIAL_ENERGY,
                SimConfig.INITIAL_HUNGER, SimConfig.INITIAL_STRESS, 0f);
        AgentTickMetrics metrics = new AgentTickMetrics();
        ActionExecutor executor = new ActionExecutor(new Random(5L));
        executor.setMetrics(metrics);

        OutcomeVector delta = executor.execute(ActionType.BROADCAST_SIGNAL, agent, world, 0, 0);

        assertEquals(1, metrics.getSignalsEmittedThisTick());
        assertEquals(-SimConfig.SIGNAL_BROADCAST_COST_ENERGY, delta.getDeltaEnergy());
    }

    @Test
    void followIncrementsCounterWhenSignalChosen() {
        float[] food = new float[9];
        float[] hazard = new float[9];
        boolean[] water = new boolean[9];
        WorldGrid world = new WorldGrid(3, 3, 7L, food, hazard, water);
        world.getSignalField().addSignal(2, 1, BinningUtil.bin01(0.2f, SimConfig.SIGNAL_STRENGTH_BINS),
                SimConfig.SIGNAL_BASE_CONFIDENCE, SimConfig.SIGNAL_TTL_TICKS, 0, 3L, 0.5f, 1L, 0L);
        AgentState agent = AgentState.forTest(new AgentId(2), 1, 1, SimConfig.INITIAL_ENERGY,
                SimConfig.INITIAL_HUNGER, SimConfig.INITIAL_STRESS, 0f);
        AgentTickMetrics metrics = new AgentTickMetrics();
        ActionExecutor executor = new ActionExecutor(new Random(7L));
        executor.setMetrics(metrics);

        OutcomeVector delta = executor.execute(ActionType.FOLLOW_SIGNAL, agent, world, 0, 0);

        assertEquals(1, metrics.getFollowMovesThisTick());
        assertEquals(-SimConfig.MOVE_ENERGY_COST, delta.getDeltaEnergy());
    }
}
