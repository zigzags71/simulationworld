package simcore.agents;

import org.junit.jupiter.api.Test;
import simcore.config.SimConfig;
import simcore.events.EventBus;
import simcore.events.SimulationEvent;
import simcore.rules.ActionType;
import simcore.rules.ContextKey;
import simcore.rules.OutcomeVector;
import simcore.rules.Rule;
import simcore.rules.RuleSelector;
import simcore.rules.RuleType;
import simcore.util.MathUtil;
import simcore.world.WorldGrid;
import simcore.world.objects.FoodEmitter;
import simcore.agents.AgentId;
import simcore.agents.AgentTickMetrics;
import simcore.agents.ActionExecutor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AgentBroadcastAvailabilityTest {

    @Test
    void broadcastBlockedWhileEmitterHasActiveSignal() throws Exception {
        float[] food = new float[9];
        float[] hazard = new float[9];
        boolean[] water = new boolean[9];
        WorldGrid world = new WorldGrid(3, 3, 21L, food, hazard, water);
        FoodEmitter emitter = world.addEmitter(1, 1, 1, 0.1f, true);
        world.getSignalField().addSignal(1, 1, 1, SimConfig.SIGNAL_BASE_CONFIDENCE, 2, 0, 5L, 0.2f, emitter.getId(), 0L);

        AgentState agent = AgentState.forTest(new AgentId(2), 1, 1, SimConfig.INITIAL_ENERGY,
                SimConfig.INITIAL_HUNGER, SimConfig.INITIAL_STRESS, 0f);
        AgentSystem system = new AgentSystem(world, 3L, 0, new EventBus<SimulationEvent>());
        int[] crowding = new int[food.length];
        crowding[MathUtil.index(1, 1, world.getWidth())] = 1;

        ContextKey contextWithActive = invokeBuildContext(system, agent, world, crowding, 0L);
        int affordanceWithActive = contextWithActive.getFoodAffordance();
        assertEquals(0, affordanceWithActive & (1 << 5));

        List<Rule> rules = new ArrayList<>();
        rules.add(new Rule(1, RuleType.NORMAL, contextWithActive, ActionType.BROADCAST_SIGNAL, OutcomeVector.zero(), 1f));
        rules.add(new Rule(2, RuleType.NORMAL, contextWithActive, ActionType.IDLE, OutcomeVector.zero(), 1f));
        Rule chosenBlocked = RuleSelector.choose(RuleSelector.applicable(rules, contextWithActive), agent, new Random(5L));
        assertNotEquals(ActionType.BROADCAST_SIGNAL, chosenBlocked.getAction());

        world.getSignalField().tickDecay();
        world.getSignalField().tickDecay();
        ContextKey contextAfterExpiry = invokeBuildContext(system, agent, world, crowding, 2L);
        int affordanceAfterExpiry = contextAfterExpiry.getFoodAffordance();
        List<Rule> refreshedRules = new ArrayList<>();
        refreshedRules.add(new Rule(3, RuleType.NORMAL, contextAfterExpiry, ActionType.BROADCAST_SIGNAL, OutcomeVector.zero(), 1f));
        refreshedRules.add(new Rule(4, RuleType.NORMAL, contextAfterExpiry, ActionType.IDLE, OutcomeVector.zero(), 1f));
        Rule chosenAllowed = RuleSelector.choose(RuleSelector.applicable(refreshedRules, contextAfterExpiry), agent, new Random(7L));
        assertNotEquals(0, affordanceAfterExpiry & (1 << 5));
        assertEquals(ActionType.BROADCAST_SIGNAL, chosenAllowed.getAction());
    }

    @Test
    void broadcastAllowedWithinDetectRadius() throws Exception {
        int size = 20;
        float[] food = new float[size * size];
        float[] hazard = new float[food.length];
        boolean[] water = new boolean[food.length];
        WorldGrid world = new WorldGrid(size, size, 7L, food, hazard, water);
        FoodEmitter emitter = world.addEmitter(10, 10, 1, 0.1f, true);
        AgentState agent = AgentState.forTest(new AgentId(3), 10, 17, SimConfig.INITIAL_ENERGY,
                SimConfig.INITIAL_HUNGER, SimConfig.INITIAL_STRESS, 0f);

        AgentSystem system = new AgentSystem(world, 3L, 0, new EventBus<SimulationEvent>());
        int[] crowding = new int[food.length];
        crowding[MathUtil.index(agent.getX(), agent.getY(), world.getWidth())] = 1;

        ContextKey key = invokeBuildContext(system, agent, world, crowding, 0L);
        int affordance = key.getFoodAffordance();
        AgentTickMetrics metrics = new AgentTickMetrics();
        ActionExecutor executor = new ActionExecutor(new Random(5L));
        executor.setMetrics(metrics);

        OutcomeVector delta = executor.execute(ActionType.BROADCAST_SIGNAL, agent, world, 0, 0L);

        assertNotEquals(0, affordance & (1 << 2));
        assertNotEquals(0, affordance & (1 << 5));
        assertNotEquals(OutcomeVector.zero(), delta);
        assertEquals(1, world.getSignalField().getSignals().size());
        assertEquals(1, metrics.getSignalsEmittedThisTick());
        assertEquals(emitter.getId(), world.getSignalField().getSignals().get(0).getEmitterId());
    }

    @Test
    void broadcastDisallowedOutsideDetectRadius() throws Exception {
        int size = 20;
        float[] food = new float[size * size];
        float[] hazard = new float[food.length];
        boolean[] water = new boolean[food.length];
        WorldGrid world = new WorldGrid(size, size, 8L, food, hazard, water);
        world.addEmitter(10, 10, 1, 0.1f, true);
        AgentState agent = AgentState.forTest(new AgentId(4), 0, 0, SimConfig.INITIAL_ENERGY,
                SimConfig.INITIAL_HUNGER, SimConfig.INITIAL_STRESS, 0f);
        AgentSystem system = new AgentSystem(world, 4L, 0, new EventBus<SimulationEvent>());
        int[] crowding = new int[food.length];
        crowding[MathUtil.index(agent.getX(), agent.getY(), world.getWidth())] = 1;

        ContextKey key = invokeBuildContext(system, agent, world, crowding, 0L);
        int affordance = key.getFoodAffordance();

        ActionExecutor executor = new ActionExecutor(new Random(6L));
        OutcomeVector delta = executor.execute(ActionType.BROADCAST_SIGNAL, agent, world, 0, 0L);

        assertEquals(0, affordance & (1 << 2));
        assertEquals(0, affordance & (1 << 5));
        assertEquals(0, world.getSignalField().getSignals().size());
        assertEquals(OutcomeVector.zero(), delta);
    }

    private ContextKey invokeBuildContext(AgentSystem system, AgentState agent, WorldGrid world, int[] crowding, long tick) throws Exception {
        Method buildContext = AgentSystem.class.getDeclaredMethod("buildContext", AgentState.class, WorldGrid.class, int[].class, long.class);
        buildContext.setAccessible(true);
        return (ContextKey) buildContext.invoke(system, agent, world, crowding, tick);
    }
}
