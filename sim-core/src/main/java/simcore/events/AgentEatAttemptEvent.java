package simcore.events;

public class AgentEatAttemptEvent implements SimulationEvent {
    private final long agentId;
    private final int tileIndex;
    private final boolean success;
    private final float consumedAmount;
    private final long tick;

    public AgentEatAttemptEvent(long agentId, int tileIndex, boolean success, float consumedAmount, long tick) {
        this.agentId = agentId;
        this.tileIndex = tileIndex;
        this.success = success;
        this.consumedAmount = consumedAmount;
        this.tick = tick;
    }

    public long getAgentId() {
        return agentId;
    }

    public int getTileIndex() {
        return tileIndex;
    }

    public boolean isSuccess() {
        return success;
    }

    public float getConsumedAmount() {
        return consumedAmount;
    }

    public long getTick() {
        return tick;
    }
}
