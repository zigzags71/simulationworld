package simcore.agents;

import org.junit.jupiter.api.Test;
import simcore.config.SimConfig;
import simcore.events.EventBus;
import simcore.events.SimulationEvent;
import simcore.rules.ActionType;
import simcore.rules.ContextKey;
import simcore.rules.OutcomeVector;
import simcore.rules.Rule;
import simcore.rules.RuleType;
import simcore.world.WorldGrid;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SocialCreditRewardTest {

    @SuppressWarnings("unchecked")
    @Test
    void broadcasterEarnsCreditWhenFollowLeadsToFood() throws Exception {
        float[] food = new float[9];
        food[4] = 0.2f; // tile (1,1)
        float[] hazard = new float[9];
        boolean[] water = new boolean[9];
        WorldGrid world = new WorldGrid(3, 3, 3L, food, hazard, water);
        world.addEmitter(1, 1, 1, 0.1f, true);

        AgentSystem system = new AgentSystem(world, 5L, 0, new EventBus<SimulationEvent>());

        Field agentsField = AgentSystem.class.getDeclaredField("agents");
        agentsField.setAccessible(true);
        List<AgentState> agents = (List<AgentState>) agentsField.get(system);

        AgentState broadcaster = AgentState.forTest(new AgentId(10), 1, 1, 1f, 1f, 0f, 0f);
        AgentState follower = AgentState.forTest(new AgentId(11), 1, 0, 0.5f, 0.5f, 0f, 0f);

        broadcaster.addRule(new Rule(1, RuleType.NORMAL, new ContextKey(0, 0, 0, 0, 0, 0, 0, (1 << 2) | (1 << 3)),
                ActionType.BROADCAST_SIGNAL, OutcomeVector.zero(), 1f));
        follower.addRule(new Rule(2, RuleType.NORMAL, new ContextKey(0, 0, 0, 0, 0, 0, 0, 0), ActionType.FOLLOW_SIGNAL,
                OutcomeVector.zero(), 1f));
        follower.addRule(new Rule(3, RuleType.NORMAL, new ContextKey(0, 0, 0, 0, 0, 0, 0, 1), ActionType.EAT,
                new OutcomeVector(SimConfig.FOOD_TO_ENERGY_GAIN * SimConfig.FOOD_CONSUME_RATE,
                        SimConfig.FOOD_TO_HUNGER_GAIN * SimConfig.FOOD_CONSUME_RATE, 0f), 1f));
        follower.addRule(new Rule(4, RuleType.NORMAL, new ContextKey(0, 0, 0, 0, 0, 0, 0, 0), ActionType.IDLE,
                OutcomeVector.zero(), 0.1f));

        agents.add(broadcaster);
        agents.add(follower);

        system.tick(world, 0);
        system.tick(world, 1);
        system.tick(world, 2);

        assertTrue(broadcaster.getSocialCredit() > 0f);
    }
}
