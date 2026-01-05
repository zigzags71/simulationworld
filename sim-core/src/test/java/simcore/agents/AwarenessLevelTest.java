package simcore.agents;

import org.junit.jupiter.api.Test;
import simcore.config.SimConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AwarenessLevelTest {

    @Test
    void thresholdsIncrease() {
        AgentState agent = AgentState.forTest(new AgentId(1), 0, 0, SimConfig.INITIAL_ENERGY,
                SimConfig.INITIAL_HUNGER, SimConfig.INITIAL_STRESS, 0f);

        agent.applyTick(0f, 0f, SimConfig.AWARE_T1 + 0.01f - agent.getStress());
        agent.updateAwarenessLevelFromStress(SimConfig.AWARE_T1, SimConfig.AWARE_T2, SimConfig.AWARE_T3,
                SimConfig.AWARE_HYST);
        assertEquals(1, agent.getAwarenessLevel());

        agent.applyTick(0f, 0f, SimConfig.AWARE_T2 + 0.01f - agent.getStress());
        agent.updateAwarenessLevelFromStress(SimConfig.AWARE_T1, SimConfig.AWARE_T2, SimConfig.AWARE_T3,
                SimConfig.AWARE_HYST);
        assertEquals(2, agent.getAwarenessLevel());

        agent.applyTick(0f, 0f, SimConfig.AWARE_T3 + 0.01f - agent.getStress());
        agent.updateAwarenessLevelFromStress(SimConfig.AWARE_T1, SimConfig.AWARE_T2, SimConfig.AWARE_T3,
                SimConfig.AWARE_HYST);
        assertEquals(3, agent.getAwarenessLevel());
    }

    @Test
    void hysteresisPreventsDrop() {
        AgentState agent = AgentState.forTest(new AgentId(2), 0, 0, SimConfig.INITIAL_ENERGY,
                SimConfig.INITIAL_HUNGER, SimConfig.INITIAL_STRESS, 0f);

        agent.applyTick(0f, 0f, SimConfig.AWARE_T2 + 0.02f - agent.getStress());
        agent.updateAwarenessLevelFromStress(SimConfig.AWARE_T1, SimConfig.AWARE_T2, SimConfig.AWARE_T3,
                SimConfig.AWARE_HYST);
        assertEquals(2, agent.getAwarenessLevel());

        float aboveHysteresis = SimConfig.AWARE_T2 - SimConfig.AWARE_HYST / 2f;
        agent.applyTick(0f, 0f, aboveHysteresis - agent.getStress());
        agent.updateAwarenessLevelFromStress(SimConfig.AWARE_T1, SimConfig.AWARE_T2, SimConfig.AWARE_T3,
                SimConfig.AWARE_HYST);
        assertEquals(2, agent.getAwarenessLevel());

        float belowHysteresis = SimConfig.AWARE_T2 - SimConfig.AWARE_HYST - 0.01f;
        agent.applyTick(0f, 0f, belowHysteresis - agent.getStress());
        agent.updateAwarenessLevelFromStress(SimConfig.AWARE_T1, SimConfig.AWARE_T2, SimConfig.AWARE_T3,
                SimConfig.AWARE_HYST);
        assertEquals(1, agent.getAwarenessLevel());
    }
}
