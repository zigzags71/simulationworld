package simcore.events;

import simcore.config.SimConfig;
import simcore.rules.ActionType;

import java.io.PrintStream;
import java.util.function.LongSupplier;

public class SimulationLogger {
    private final PrintStream out;
    private final LongSupplier selectedAgentSupplier;
    private long lastRegionLogTick = -SimConfig.LOG_THROTTLE_TICKS;

    public SimulationLogger(EventBus<SimulationEvent> bus, LongSupplier selectedAgentSupplier) {
        this(bus, selectedAgentSupplier, System.out);
    }

    public SimulationLogger(EventBus<SimulationEvent> bus, LongSupplier selectedAgentSupplier, PrintStream out) {
        this.out = out;
        this.selectedAgentSupplier = selectedAgentSupplier;
        bus.subscribe(this::handleEvent);
    }

    private void handleEvent(SimulationEvent event) {
        if (event instanceof AgentDiedEvent died) {
            if (SimConfig.LOG_EVENTS_ENABLED) {
                out.printf("[tick=%d] agent %d died at (%d,%d)%n", died.getTick(), died.getAgentId(), died.getX(), died.getY());
            }
            return;
        }
        if (event instanceof AgentDeselectedEvent deselected) {
            if (SimConfig.LOG_SELECTED_AGENT_ENABLED) {
                out.printf("[tick=%d] selected agent cleared%n", deselected.getTick());
            }
            return;
        }
        if (event instanceof AgentEatAttemptEvent eat) {
            if (SimConfig.LOG_SELECTED_AGENT_ENABLED && eat.getAgentId() == selectedAgentSupplier.getAsLong()) {
                out.printf("[tick=%d] agent %d eat tile=%d success=%s consumed=%.4f%n", eat.getTick(), eat.getAgentId(),
                        eat.getTileIndex(), eat.isSuccess(), eat.getConsumedAmount());
            }
            return;
        }
        if (event instanceof RuleExecutedEvent exec) {
            if (SimConfig.LOG_SELECTED_AGENT_ENABLED && exec.getAgentId() == selectedAgentSupplier.getAsLong()) {
                ActionType action = exec.getAction();
                out.printf("[tick=%d] agent %d rule %d action=%s trust=%.3f predErr=%.4f%n", exec.getTick(), exec.getAgentId(),
                        exec.getRuleId(), action, exec.getTrust(), exec.getPredictionError());
            }
            return;
        }
        if (event instanceof SelectedAgentSnapshotEvent snap) {
            if (SimConfig.LOG_SELECTED_AGENT_ENABLED && snap.getAgentId() == selectedAgentSupplier.getAsLong()) {
                out.printf("[tick=%d] agent %d energy=%.3f hunger=%.3f stress=%.3f predErr=%.4f social=%.3f%n",
                        snap.getTick(), snap.getAgentId(), snap.getEnergy(), snap.getHunger(), snap.getStress(),
                        snap.getPredictionError(), snap.getSocialCredit());
            }
            return;
        }
        if (event instanceof SelectedRegionSnapshotEvent region) {
            if (SimConfig.LOG_SELECTED_REGION_ENABLED && shouldLogRegion(region.getTick())) {
                out.printf("[tick=%d] region (%d,%d)-(%d,%d) agents=%d avgEnergy=%.3f avgHunger=%.3f avgStress=%.3f avgPredErr=%.4f%n",
                        region.getTick(), region.getMinX(), region.getMinY(), region.getMaxX(), region.getMaxY(),
                        region.getAgentCount(), region.getAvgEnergy(), region.getAvgHunger(), region.getAvgStress(),
                        region.getAvgPredictionError());
            }
        }
    }

    private boolean shouldLogRegion(long tick) {
        if (tick - lastRegionLogTick >= SimConfig.LOG_THROTTLE_TICKS) {
            lastRegionLogTick = tick;
            return true;
        }
        return false;
    }
}
