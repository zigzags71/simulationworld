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
        AgentState agent = AgentState.forTest(new AgentId(1), 0, 0, SimConfig.INITIAL_ENERGY,
                SimConfig.INITIAL_HUNGER, SimConfig.INITIAL_STRESS, 0f);
        agent.applyTick(0f, -0.4f, 0.1f);
        ContextKey key = new ContextKey(0, 0, 0, 0, 0, 0, 0, 1);
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

    // Intent: starving agents should prefer MOVE over IDLE when no food is available so they explore instead of freezing.
    @Test
    void starvingAgentsPreferMoveOverIdle() {
        AgentState agent = AgentState.forTest(new AgentId(2), 0, 0, SimConfig.INITIAL_ENERGY,
                0f, SimConfig.INITIAL_STRESS, 0f);
        ContextKey key = new ContextKey(0, 0, 0, 0, 0, 0, 0, 1);
        List<Rule> rules = new ArrayList<>();
        rules.add(new Rule(10, RuleType.NORMAL, key, ActionType.MOVE, OutcomeVector.zero(), 0.5f));
        rules.add(new Rule(11, RuleType.NORMAL, key, ActionType.IDLE, OutcomeVector.zero(), 0.5f));

        Random random = new Random(123L);
        int moveCount = 0;
        int idleCount = 0;
        for (int i = 0; i < 500; i++) {
            Rule chosen = RuleSelector.choose(rules, agent, random);
            if (chosen.getAction() == ActionType.MOVE) {
                moveCount++;
            } else if (chosen.getAction() == ActionType.IDLE) {
                idleCount++;
            }
        }

        assertTrue(moveCount > idleCount, "Starving agents should explore instead of idling");
    }
}
