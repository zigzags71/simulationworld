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
        metricsFirst.setActiveSignalsCount(2);

        TelemetryEvent first = engine.buildTelemetryEvent(metricsFirst);

        AgentTickMetrics metricsSecond = new AgentTickMetrics();
        metricsSecond.incrementSignalsEmitted();
        metricsSecond.setActiveSignalsCount(1);

        TelemetryEvent second = engine.buildTelemetryEvent(metricsSecond);

        assertEquals(2, first.getSignalsEmittedThisTick());
        assertEquals(1, first.getTotalFollowMoves());
        assertEquals(2, first.getActiveSignalsCount());
        assertEquals(3, second.getTotalSignalsEmitted());
        assertEquals(1, second.getTotalFollowMoves());
        assertTrue(second.getTotalSignalsEmitted() >= first.getSignalsEmittedThisTick() + second.getSignalsEmittedThisTick());
    }
}
