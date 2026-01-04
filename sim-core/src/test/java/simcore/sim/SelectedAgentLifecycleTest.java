package simcore.sim;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import simcore.agents.AgentState;
import simcore.config.MapGenConfig;
import simcore.config.SimConfig;
import simcore.events.TelemetryBus;
import simcore.sim.commands.SetSelectedAgentCommand;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SelectedAgentLifecycleTest {

    @AfterEach
    void resetConfig() {
        SimConfig.LOG_SELECTED_AGENT_ENABLED = false;
        SimConfig.LOG_EVENTS_ENABLED = false;
        SimConfig.LOG_SELECTED_REGION_ENABLED = false;
    }

    @Test
    void clearsSelectionWhenAgentDies() {
        SimulationEngine engine = new SimulationEngine(MapGenConfig.defaults(), new TelemetryBus());
        AgentState target = engine.getAgentSystem().getAgents().get(0);
        engine.queueSelectedAgentCommand(new SetSelectedAgentCommand(target.getId().value()));
        engine.resume();
        engine.stepOnce();
        assertEquals(target.getId().value(), engine.getSelectedAgentIdValue());

        target.applyTick(-2f, -2f, 1f);
        engine.stepOnce();

        assertEquals(-1, engine.getSelectedAgentIdValue());
    }
}
