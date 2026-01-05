package simcore.agents;

import org.junit.jupiter.api.Test;
import simcore.config.SimConfig;
import simcore.rules.OutcomeVector;
import simcore.util.BinningUtil;
import simcore.util.MathUtil;
import simcore.world.WorldGrid;
import simcore.world.signals.SignalField;
import simcore.rules.ActionType;

import simcore.agents.AgentId;
import simcore.agents.AgentTickMetrics;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatternLockTest {

    @Test
    void followLockKeepsTargetAcrossTicks() {
        float[] food = new float[25];
        float[] hazard = new float[25];
        boolean[] water = new boolean[25];
        WorldGrid world = new WorldGrid(5, 5, 11L, food, hazard, water);
        SignalField field = world.getSignalField();
        field.addSignal(4, 2, BinningUtil.bin01(0.3f, SimConfig.SIGNAL_STRENGTH_BINS), SimConfig.SIGNAL_BASE_CONFIDENCE,
                SimConfig.SIGNAL_TTL_TICKS, 0, 7L, 0.2f, 1L, 0L);
        field.addSignal(1, 1, BinningUtil.bin01(0.2f, SimConfig.SIGNAL_STRENGTH_BINS), SimConfig.SIGNAL_BASE_CONFIDENCE,
                SimConfig.SIGNAL_TTL_TICKS, 0, 9L, 0.1f, 2L, 0L);
        field.addSignal(2, 4, BinningUtil.bin01(0.9f, SimConfig.SIGNAL_STRENGTH_BINS), SimConfig.SIGNAL_BASE_CONFIDENCE,
                SimConfig.SIGNAL_TTL_TICKS, 0, 3L, 0.9f, 3L, 0L);

        AgentState agent = AgentState.forTest(new AgentId(5), 2, 2, SimConfig.INITIAL_ENERGY, SimConfig.INITIAL_HUNGER,
                SimConfig.INITIAL_STRESS, 0f);
        AgentTickMetrics metrics = new AgentTickMetrics();
        ActionExecutor executor = new ActionExecutor(new Random(3L));
        executor.setMetrics(metrics);

        OutcomeVector firstMove = executor.execute(ActionType.FOLLOW_SIGNAL, agent, world, 0, 0);

        assertEquals(3, agent.getX());
        assertEquals(2, agent.getY());
        assertTrue(agent.hasFollowLock(0));
        assertEquals(4, agent.getFollowLockTargetX());
        assertEquals(2, agent.getFollowLockTargetY());
        assertEquals(1, metrics.getFollowMovesThisTick());

        field.addSignal(0, 2, BinningUtil.bin01(0.9f, SimConfig.SIGNAL_STRENGTH_BINS), SimConfig.SIGNAL_VERIFIED_CONFIDENCE,
                SimConfig.SIGNAL_TTL_TICKS, 0, 11L, 0.9f, 4L, 1L);

        executor.setMetrics(metrics);
        OutcomeVector secondMove = executor.execute(ActionType.FOLLOW_SIGNAL, agent, world, 0, 1);

        assertEquals(4, agent.getX());
        assertEquals(2, agent.getY());
        assertTrue(secondMove.getDeltaEnergy() < 0f);
        assertEquals(1, metrics.getFollowMovesThisTick());
    }

    @Test
    void foodLockMovesAvoidBacktrackingTowardTarget() {
        float[] food = new float[100];
        float[] hazard = new float[100];
        boolean[] water = new boolean[100];
        int width = 10;
        int startX = 2;
        int targetX = startX + 3;
        int targetY = 2;
        food[targetY * width + targetX] = 0.5f;
        WorldGrid world = new WorldGrid(width, 10, 13L, food, hazard, water);
        AgentState agent = AgentState.forTest(new AgentId(1), startX, targetY, SimConfig.INITIAL_ENERGY,
                SimConfig.INITIAL_HUNGER, SimConfig.INITIAL_STRESS, 0f);
        ActionExecutor executor = new ActionExecutor(new Random(2L));
        agent.setFoodLock(targetX, targetY, SimConfig.PATTERN_FOOD_LOCK_TICKS);

        int previousX = agent.getX();
        for (int t = 0; t < 3; t++) {
            OutcomeVector delta = executor.executeFoodLockMoveIfActive(agent, world, t);
            assertNotEquals(0f, delta.getDeltaEnergy());
            assertTrue(agent.getX() >= previousX);
            previousX = agent.getX();
        }
        assertEquals(targetX, agent.getX());
        assertEquals(targetY, agent.getY());
    }

    @Test
    void directedMoveAvoidsWaterTiles() {
        float[] food = new float[9];
        float[] hazard = new float[9];
        boolean[] water = new boolean[9];
        int width = 3;
        water[4] = true;
        food[4] = SimConfig.FOOD_MIN_TO_EAT;
        WorldGrid world = new WorldGrid(width, 3, 17L, food, hazard, water);
        AgentState agent = AgentState.forTest(new AgentId(1), 0, 0, SimConfig.INITIAL_ENERGY, SimConfig.INITIAL_HUNGER,
                SimConfig.INITIAL_STRESS, 0f);
        ActionExecutor executor = new ActionExecutor(new Random(7L));
        agent.setFoodLock(1, 1, SimConfig.PATTERN_FOOD_LOCK_TICKS);

        OutcomeVector delta = executor.executeFoodLockMoveIfActive(agent, world, 0);

        assertEquals(1, agent.getX());
        assertEquals(0, agent.getY());
        assertEquals(0f, hazard[MathUtil.index(agent.getX(), agent.getY(), width)]);
        assertEquals(-SimConfig.MOVE_ENERGY_COST, delta.getDeltaEnergy());
        assertTrue(agent.hasFoodLock(0));
    }
}
