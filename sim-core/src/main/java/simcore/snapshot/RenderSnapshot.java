package simcore.snapshot;

/**
 * Immutable-ish snapshot for rendering. Arrays are reused by SnapshotBuffer and should not be mutated by consumers.
 */
public class RenderSnapshot {
    private final int width;
    private final int height;
    private final float[] food;
    private final float[] hazard;
    private final float[] crowding;
    private final int[] agentX;
    private final int[] agentY;
    private final int[] agentColorARGB;
    private final long[] agentId;
    private final int agentCount;
    private final long tickIndex;

    public RenderSnapshot(int width, int height, float[] food, float[] hazard, float[] crowding,
                          int[] agentX, int[] agentY, int[] agentColorARGB, long[] agentId, int agentCount, long tickIndex) {
        this.width = width;
        this.height = height;
        this.food = food;
        this.hazard = hazard;
        this.crowding = crowding;
        this.agentX = agentX;
        this.agentY = agentY;
        this.agentColorARGB = agentColorARGB;
        this.agentId = agentId;
        this.agentCount = agentCount;
        this.tickIndex = tickIndex;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public float[] getFood() {
        return food;
    }

    public float[] getHazard() {
        return hazard;
    }

    public float[] getCrowding() {
        return crowding;
    }

    public int[] getAgentX() {
        return agentX;
    }

    public int[] getAgentY() {
        return agentY;
    }

    public int[] getAgentColorARGB() {
        return agentColorARGB;
    }

    public long[] getAgentId() {
        return agentId;
    }

    public int getAgentCount() {
        return agentCount;
    }

    public long getTickIndex() {
        return tickIndex;
    }
}
