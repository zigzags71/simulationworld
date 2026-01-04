package simcore.agents;

import org.junit.jupiter.api.Test;
import simcore.config.SimConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentStateMetabolismTest {

    @Test
    void clampsAndDetectsDeath() {
        AgentState agent = AgentState.forTest(new AgentId(1), 0, 0, SimConfig.INITIAL_ENERGY,
                SimConfig.INITIAL_HUNGER, SimConfig.INITIAL_STRESS, 0f);

        agent.applyTick(-2.0f, -2.0f, 2.0f);

        assertEquals(0f, agent.getEnergy());
        assertEquals(0f, agent.getHunger());
        assertEquals(1f, agent.getStress());
        assertTrue(agent.isDead());
    }
}
