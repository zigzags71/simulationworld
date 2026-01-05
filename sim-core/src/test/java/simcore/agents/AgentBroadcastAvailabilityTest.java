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

        Method buildContext = AgentSystem.class.getDeclaredMethod("buildContext", AgentState.class, WorldGrid.class, int[].class, long.class);
        buildContext.setAccessible(true);
        ContextKey contextWithActive = (ContextKey) buildContext.invoke(system, agent, world, crowding, 0L);
        int affordanceWithActive = contextWithActive.getFoodAffordance();
        assertEquals(0, affordanceWithActive & (1 << 5));

        List<Rule> rules = new ArrayList<>();
        rules.add(new Rule(1, RuleType.NORMAL, contextWithActive, ActionType.BROADCAST_SIGNAL, OutcomeVector.zero(), 1f));
        rules.add(new Rule(2, RuleType.NORMAL, contextWithActive, ActionType.IDLE, OutcomeVector.zero(), 1f));
        Rule chosenBlocked = RuleSelector.choose(RuleSelector.applicable(rules, contextWithActive), agent, new Random(5L));
        assertNotEquals(ActionType.BROADCAST_SIGNAL, chosenBlocked.getAction());

        world.getSignalField().tickDecay();
        world.getSignalField().tickDecay();
        ContextKey contextAfterExpiry = (ContextKey) buildContext.invoke(system, agent, world, crowding, 2L);
        int affordanceAfterExpiry = contextAfterExpiry.getFoodAffordance();
        List<Rule> refreshedRules = new ArrayList<>();
        refreshedRules.add(new Rule(3, RuleType.NORMAL, contextAfterExpiry, ActionType.BROADCAST_SIGNAL, OutcomeVector.zero(), 1f));
        refreshedRules.add(new Rule(4, RuleType.NORMAL, contextAfterExpiry, ActionType.IDLE, OutcomeVector.zero(), 1f));
        Rule chosenAllowed = RuleSelector.choose(RuleSelector.applicable(refreshedRules, contextAfterExpiry), agent, new Random(7L));
        assertNotEquals(0, affordanceAfterExpiry & (1 << 5));
        assertEquals(ActionType.BROADCAST_SIGNAL, chosenAllowed.getAction());
    }
}
