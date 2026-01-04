package simcore.events;

public class AgentDiedEvent implements SimulationEvent {
    private final long agentId;
    private final long tick;
    private final int x;
    private final int y;

    public AgentDiedEvent(long agentId, long tick, int x, int y) {
        this.agentId = agentId;
        this.tick = tick;
        this.x = x;
        this.y = y;
    }

    public long getAgentId() {
        return agentId;
    }

    public long getTick() {
        return tick;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
