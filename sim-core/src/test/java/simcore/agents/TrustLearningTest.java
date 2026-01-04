package simcore.agents;

import org.junit.jupiter.api.Test;
import simcore.config.SimConfig;
import simcore.rules.ActionType;
import simcore.rules.ContextKey;
import simcore.rules.OutcomeVector;
import simcore.rules.Rule;
import simcore.rules.RuleType;
import simcore.world.WorldGrid;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TrustLearningTest {

    @Test
    void trustMovesUpOnAccuratePredictionsAndDownOnErrors() {
        WorldGrid world = new WorldGrid(1, 1, new float[]{0.2f}, new float[]{0.1f}, new boolean[]{false});
        AgentSystem system = new AgentSystem(world, 99L, 0);
        Rule rule = new Rule(1, RuleType.NORMAL, new ContextKey(0, 0, 0, 0, 0, 0, 0, 0), ActionType.IDLE, OutcomeVector.zero(), 0.5f);

        system.applyTrustUpdate(rule, SimConfig.ERROR_SUCCESS_THRESHOLD / 2f, 1);
        float afterSuccess = rule.getTrust();
        system.applyTrustUpdate(rule, SimConfig.ERROR_SUCCESS_THRESHOLD * 4f, 2);
        float afterFailure = rule.getTrust();

        assertTrue(afterSuccess > 0.5f, "Trust should increase after low error");
        assertTrue(afterFailure < afterSuccess, "Trust should decay after high error");
    }
}
