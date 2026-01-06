package simcore.events;

import simcore.agents.DeathCause;

public class AgentDiedEvent implements SimulationEvent {
    private final long agentId;
    private final long tick;
    private final int x;
    private final int y;
    private final DeathCause cause;

    public AgentDiedEvent(long agentId, long tick, int x, int y, DeathCause cause) {
        this.agentId = agentId;
        this.tick = tick;
        this.x = x;
        this.y = y;
        this.cause = cause;
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

    public DeathCause getCause() {
        return cause;
    }
}
