package simcore.rules;

import org.junit.jupiter.api.Test;
import simcore.agents.AgentId;
import simcore.agents.AgentState;
import simcore.config.SimConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleSelectorTest {

    @Test
    void higherTrustDominatesSelection() {
        AgentState agent = new AgentState(new AgentId(1), 0, 0, SimConfig.INITIAL_ENERGY, 0);
        agent.applyTick(0f, -0.4f, 0.1f);
        ContextKey key = new ContextKey(0, 0, 0, 0, 0, 0, 0, 0);
        List<Rule> rules = new ArrayList<>();
        rules.add(new Rule(1, RuleType.NORMAL, key, ActionType.EAT, OutcomeVector.zero(), 0.9f));
        rules.add(new Rule(2, RuleType.NORMAL, key, ActionType.MOVE, OutcomeVector.zero(), 0.1f));

        Random random = new Random(42L);
        int eatCount = 0;
        int moveCount = 0;
        for (int i = 0; i < 500; i++) {
            Rule chosen = RuleSelector.choose(rules, agent, random);
            if (chosen.getAction() == ActionType.EAT) {
                eatCount++;
            } else {
                moveCount++;
            }
        }

        assertTrue(eatCount > moveCount * 2, "Eat rule should dominate due to trust weighting");
    }
}
