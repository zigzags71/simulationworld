package simcore.world.signals;

import org.junit.jupiter.api.Test;
import simcore.agents.ActionExecutor;
import simcore.agents.AgentId;
import simcore.agents.AgentState;
import simcore.agents.AgentTickMetrics;
import simcore.config.SimConfig;
import simcore.rules.ActionType;
import simcore.rules.OutcomeVector;
import simcore.util.BinningUtil;
import simcore.world.WorldGrid;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class SignalFieldEmitterTest {

    @Test
    void onlyOneActiveSignalPerEmitter() {
        SignalField field = new SignalField(4, 4);

        Signal first = field.addSignal(1, 1, 1, 0.8f, 5, 0, 1L, 0.3f, 10L, 0L);
        Signal second = field.addSignal(1, 2, 1, 0.8f, 5, 0, 2L, 0.3f, 10L, 0L);

        assertNotNull(first);
        assertNull(second);
        assertEquals(1, field.getSignals().size());
    }

    @Test
    void signalExpiryAllowsNewBroadcast() {
        SignalField field = new SignalField(4, 4);

        Signal first = field.addSignal(1, 1, 1, 0.8f, 1, 0, 1L, 0.3f, 5L, 0L);
        assertNotNull(first);

        field.tickDecay();
        assertTrue(field.getSignals().isEmpty());

        Signal second = field.addSignal(1, 2, 1, 0.8f, 1, 0, 2L, 0.3f, 5L, 1L);
        assertNotNull(second);
    }

    @Test
    void agentsDoNotFollowTheirOwnSignals() {
        float[] food = new float[9];
        float[] hazard = new float[9];
        boolean[] water = new boolean[9];
        WorldGrid world = new WorldGrid(3, 3, 11L, food, hazard, water);
        world.addEmitter(1, 1, 1, 0.1f, true);
        world.getSignalField().addSignal(1, 1, BinningUtil.bin01(0.5f, SimConfig.SIGNAL_STRENGTH_BINS),
                SimConfig.SIGNAL_BASE_CONFIDENCE, SimConfig.SIGNAL_TTL_TICKS, 0, 7L, 0.4f, 1L, 0L);
        AgentState agent = AgentState.forTest(new AgentId(7), 0, 0, SimConfig.INITIAL_ENERGY,
                SimConfig.INITIAL_HUNGER, SimConfig.INITIAL_STRESS, 0f);
        AgentTickMetrics metrics = new AgentTickMetrics();
        ActionExecutor executor = new ActionExecutor(new Random(11L));
        executor.setMetrics(metrics);

        OutcomeVector delta = executor.execute(ActionType.FOLLOW_SIGNAL, agent, world, 0, 0);

        assertEquals(0, metrics.getFollowMovesThisTick());
        assertEquals(-SimConfig.MOVE_ENERGY_COST, delta.getDeltaEnergy());
    }

    @Test
    void followersShareEmitterSignal() {
        float[] food = new float[25];
        float[] hazard = new float[25];
        boolean[] water = new boolean[25];
        WorldGrid world = new WorldGrid(5, 5, 13L, food, hazard, water);
        world.addEmitter(2, 2, 1, 0.1f, true);
        world.getSignalField().addSignal(2, 2, BinningUtil.bin01(0.5f, SimConfig.SIGNAL_STRENGTH_BINS),
                SimConfig.SIGNAL_BASE_CONFIDENCE, SimConfig.SIGNAL_TTL_TICKS, 0, 3L, 0.4f, 1L, 0L);
        AgentState agentA = AgentState.forTest(new AgentId(4), 0, 0, SimConfig.INITIAL_ENERGY,
                SimConfig.INITIAL_HUNGER, SimConfig.INITIAL_STRESS, 0f);
        AgentState agentB = AgentState.forTest(new AgentId(5), 0, 1, SimConfig.INITIAL_ENERGY,
                SimConfig.INITIAL_HUNGER, SimConfig.INITIAL_STRESS, 0f);
        ActionExecutor executor = new ActionExecutor(new Random(13L));

        executor.execute(ActionType.FOLLOW_SIGNAL, agentA, world, 0, 0);
        executor.execute(ActionType.FOLLOW_SIGNAL, agentB, world, 1, 0);

        assertTrue(agentA.getX() > 0 || agentA.getY() > 0);
        assertTrue(agentB.getX() > 0 || agentB.getY() > 1);
    }

    @Test
    void concurrentBroadcastsFromSameEmitterAreRejected() {
        float[] food = new float[9];
        float[] hazard = new float[9];
        boolean[] water = new boolean[9];
        WorldGrid world = new WorldGrid(3, 3, 17L, food, hazard, water);
        world.addEmitter(1, 1, 1, 0.1f, true);
        AgentState agentA = AgentState.forTest(new AgentId(8), 1, 1, SimConfig.INITIAL_ENERGY,
                SimConfig.INITIAL_HUNGER, SimConfig.INITIAL_STRESS, 0f);
        AgentState agentB = AgentState.forTest(new AgentId(9), 1, 1, SimConfig.INITIAL_ENERGY,
                SimConfig.INITIAL_HUNGER, SimConfig.INITIAL_STRESS, 0f);
        AgentTickMetrics metrics = new AgentTickMetrics();
        ActionExecutor executor = new ActionExecutor(new Random(17L));
        executor.setMetrics(metrics);

        OutcomeVector first = executor.execute(ActionType.BROADCAST_SIGNAL, agentA, world, 0, 0);
        OutcomeVector second = executor.execute(ActionType.BROADCAST_SIGNAL, agentB, world, 1, 0);

        assertEquals(-SimConfig.SIGNAL_BROADCAST_COST_ENERGY, first.getDeltaEnergy());
        assertEquals(0f, second.getDeltaEnergy());
        assertEquals(1, world.getSignalField().getSignals().size());
        assertEquals(1, metrics.getSignalsEmittedThisTick());
    }
}
