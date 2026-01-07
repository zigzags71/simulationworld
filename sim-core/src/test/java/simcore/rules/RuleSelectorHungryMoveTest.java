package simcore.rules;

import org.junit.jupiter.api.Test;
import simcore.agents.AgentId;
import simcore.agents.AgentState;
import simcore.config.SimConfig;
import simcore.rules.OutcomeVector;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleSelectorHungryMoveTest {

    // Intent: EAT should not be considered available when only trace food is present.
    @Test
    void eatNotAvailableWhenOnlyTraceFood() {
        ContextKey key = new ContextKey(0, 0, 0, 0, 0, 0, 0, 1);
        Rule eatRule = new Rule(1L, RuleType.NORMAL, key, ActionType.EAT, OutcomeVector.zero(), 0.5f);
        Rule moveRule = new Rule(2L, RuleType.NORMAL, key, ActionType.MOVE, OutcomeVector.zero(), 0.5f);

        List<Rule> applicable = RuleSelector.applicable(List.of(eatRule, moveRule), key);

        assertTrue(applicable.stream().noneMatch(rule -> rule.getAction() == ActionType.EAT));
    }

    // Intent: Hungry agents should choose MOVE when no EAT rule is available, regardless of trust values.
    @Test
    void hungryForcesMoveWhenNoEatCandidate() {
        ContextKey key = new ContextKey(0, 0, 0, 0, 0, 0, 0, 0);
        Rule idleRule = new Rule(1L, RuleType.NORMAL, key, ActionType.IDLE, OutcomeVector.zero(), 0.95f);
        Rule moveRule = new Rule(2L, RuleType.NORMAL, key, ActionType.MOVE, OutcomeVector.zero(), 0.10f);
        AgentState agent = AgentState.forTest(new AgentId(1), 0, 0, SimConfig.INITIAL_ENERGY, 0.3f, 0f, 0f);

        Rule chosen = RuleSelector.choose(List.of(idleRule, moveRule), agent, new Random(1L));

        assertEquals(ActionType.MOVE, chosen.getAction());
    }
}
