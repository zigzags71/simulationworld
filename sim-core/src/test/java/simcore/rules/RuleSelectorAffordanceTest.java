package simcore.rules;

import org.junit.jupiter.api.Test;
import simcore.agents.AgentId;
import simcore.agents.AgentState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleSelectorAffordanceTest {

    private List<Rule> buildRuleSet(int affordanceBits) {
        ContextKey key = new ContextKey(0, 0, 0, 0, 0, 0, 0, affordanceBits);
        List<Rule> rules = new ArrayList<>();
        rules.add(new Rule(1, RuleType.NORMAL, key, ActionType.EAT, OutcomeVector.zero(), 1.0f));
        rules.add(new Rule(2, RuleType.NORMAL, key, ActionType.FOLLOW_SIGNAL, OutcomeVector.zero(), 0.6f));
        rules.add(new Rule(3, RuleType.NORMAL, key, ActionType.BROADCAST_SIGNAL, OutcomeVector.zero(), 0.6f));
        rules.add(new Rule(4, RuleType.NORMAL, key, ActionType.MOVE, OutcomeVector.zero(), 0.9f));
        rules.add(new Rule(5, RuleType.NORMAL, key, ActionType.IDLE, OutcomeVector.zero(), 0.7f));
        return rules;
    }

    @Test
    void disallowedActionsAreFilteredOut() {
        AgentState agent = AgentState.forTest(new AgentId(5), 0, 0, 1f, 0.5f, 0f, 0f);
        List<Rule> rules = buildRuleSet(0);
        ContextKey context = new ContextKey(0, 0, 0, 0, 0, 0, 0, 0);

        for (int i = 0; i < 50; i++) {
            Rule chosen = RuleSelector.choose(RuleSelector.applicable(rules, context), agent, new Random(42L + i));
            assertNotNull(chosen);
            assertTrue(chosen.getAction() == ActionType.MOVE || chosen.getAction() == ActionType.IDLE);
        }
    }

    @Test
    void eatAllowedWhenFoodPresent() {
        AgentState agent = AgentState.forTest(new AgentId(6), 0, 0, 0.4f, 0.4f, 0f, 0f);
        List<Rule> rules = buildRuleSet(1);
        ContextKey context = new ContextKey(0, 0, 0, 0, 0, 0, 0, 1);

        Rule chosen = RuleSelector.choose(RuleSelector.applicable(rules, context), agent, new Random(7L));
        assertEquals(ActionType.EAT, chosen.getAction());
    }

    @Test
    void followBlockedWhenFoodAvailable() {
        AgentState agent = AgentState.forTest(new AgentId(7), 0, 0, 0.4f, 0.2f, 0f, 0f);
        int affordance = (1) | (1 << 1) | (1 << 4);
        List<Rule> rules = buildRuleSet(affordance);
        ContextKey context = new ContextKey(0, 0, 0, 0, 0, 0, 0, affordance);

        for (int i = 0; i < 30; i++) {
            Rule chosen = RuleSelector.choose(RuleSelector.applicable(rules, context), agent, new Random(9L + i));
            assertNotEquals(ActionType.FOLLOW_SIGNAL, chosen.getAction());
        }
    }

    @Test
    void followAllowedWhenOnlyTraceFoodAndSignalPresent() {
        AgentState agent = AgentState.forTest(new AgentId(9), 0, 0, 0.4f, 0.2f, 0f, 0f);
        int affordance = (1) | (1 << 1);
        List<Rule> rules = buildRuleSet(affordance);
        ContextKey context = new ContextKey(0, 0, 0, 0, 0, 0, 0, affordance);

        List<Rule> applicable = RuleSelector.applicable(rules, context);

        assertTrue(applicable.stream().anyMatch(rule -> rule.getAction() == ActionType.FOLLOW_SIGNAL));
        assertTrue(applicable.stream().anyMatch(rule -> rule.getAction() == ActionType.EAT));
    }

    @Test
    void broadcastRequiresCooldownAndEmitter() {
        AgentState agent = AgentState.forTest(new AgentId(8), 0, 0, 0.6f, 0.6f, 0f, 0f);
        int affordanceWithoutCooldown = 1 << 2;
        List<Rule> rules = buildRuleSet(affordanceWithoutCooldown);
        ContextKey context = new ContextKey(0, 0, 0, 0, 0, 0, 0, affordanceWithoutCooldown);

        for (int i = 0; i < 20; i++) {
            Rule chosen = RuleSelector.choose(RuleSelector.applicable(rules, context), agent, new Random(11L + i));
            assertNotEquals(ActionType.BROADCAST_SIGNAL, chosen.getAction());
        }

        int affordanceWithCooldown = (1 << 2) | (1 << 3);
        List<Rule> readyRules = buildRuleSet(affordanceWithCooldown);
        ContextKey readyContext = new ContextKey(0, 0, 0, 0, 0, 0, 0, affordanceWithCooldown);
        Rule chosenReady = RuleSelector.choose(RuleSelector.applicable(readyRules, readyContext), agent, new Random(13L));
        assertEquals(ActionType.BROADCAST_SIGNAL, chosenReady.getAction());
    }

    @Test
    void moveIdleDroppedWhenBroadcastAvailable() {
        AgentState agent = AgentState.forTest(new AgentId(10), 0, 0, 0.6f, 0.6f, 0f, 0f);
        int affordanceWithBroadcast = (1 << 2) | (1 << 3);
        List<Rule> rules = buildRuleSet(affordanceWithBroadcast);
        ContextKey context = new ContextKey(0, 0, 0, 0, 0, 0, 0, affordanceWithBroadcast);

        List<Rule> applicable = RuleSelector.applicable(rules, context);

        assertTrue(applicable.stream().allMatch(rule -> rule.getAction() == ActionType.BROADCAST_SIGNAL));
    }
}
