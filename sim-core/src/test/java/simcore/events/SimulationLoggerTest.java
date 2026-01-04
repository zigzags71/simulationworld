package simcore.events;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import simcore.config.SimConfig;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimulationLoggerTest {

    @AfterEach
    void resetConfig() {
        SimConfig.LOG_SELECTED_AGENT_ENABLED = false;
        SimConfig.LOG_EVENTS_ENABLED = false;
        SimConfig.LOG_SELECTED_REGION_ENABLED = false;
        SimConfig.LOG_THROTTLE_TICKS = 20;
    }

    @Test
    void regionLoggingIsThrottled() {
        SimConfig.LOG_SELECTED_REGION_ENABLED = true;
        SimConfig.LOG_THROTTLE_TICKS = 3;
        EventBus<SimulationEvent> bus = new EventBus<>();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new SimulationLogger(bus, () -> -1L, new PrintStream(baos));

        for (int tick = 0; tick < 6; tick++) {
            bus.publish(new SelectedRegionSnapshotEvent(0, 0, 1, 1, 0, 0f, 0f, 0f, 0f, tick));
        }

        long lineCount = java.util.Arrays.stream(baos.toString().split(System.lineSeparator()))
                .filter(s -> !s.isBlank())
                .count();
        assertEquals(2, lineCount);
    }
}
