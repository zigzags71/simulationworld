package simcore.sim;

import org.junit.jupiter.api.Test;
import simcore.agents.AgentTickMetrics;
import simcore.config.MapGenConfig;
import simcore.events.TelemetryEvent;
import simcore.events.TelemetryBus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationEngineTelemetryTest {

    @Test
    void totalsAccumulateAcrossTelemetryEvents() {
        TelemetryBus bus = new TelemetryBus();
        SimulationEngine engine = new SimulationEngine(MapGenConfig.defaults(), bus);

        AgentTickMetrics metricsFirst = new AgentTickMetrics();
        metricsFirst.incrementSignalsEmitted();
        metricsFirst.incrementSignalsEmitted();
        metricsFirst.incrementFollowMoves();

        TelemetryEvent first = engine.buildTelemetryEvent(metricsFirst);

        AgentTickMetrics metricsSecond = new AgentTickMetrics();
        metricsSecond.incrementSignalsEmitted();

        TelemetryEvent second = engine.buildTelemetryEvent(metricsSecond);

        assertEquals(2, first.getSignalsEmittedThisTick());
        assertEquals(1, first.getTotalFollowMoves());
        assertEquals(3, second.getTotalSignalsEmitted());
        assertEquals(1, second.getTotalFollowMoves());
        assertTrue(second.getTotalSignalsEmitted() >= first.getSignalsEmittedThisTick() + second.getSignalsEmittedThisTick());
    }
}
