package simcore.events;

public class AgentDeselectedEvent implements SimulationEvent {
    private final long tick;

    public AgentDeselectedEvent(long tick) {
        this.tick = tick;
    }

    public long getTick() {
        return tick;
    }
}
