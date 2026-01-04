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
    private final int[] agentAge;
    private final float[] agentEnergy;
    private final float[] agentHunger;
    private final float[] agentStress;
    private final float[] agentPredictionError;
    private final boolean[] agentAwareness;
    private final int[] agentCultureId;
    private final int[] agentCounts;
    private final int agentCount;
    private final long tickIndex;
    private final SelectedAgentDetails selectedAgentDetails;

    public RenderSnapshot(int width, int height, float[] food, float[] hazard, float[] crowding,
                          int[] agentX, int[] agentY, int[] agentColorARGB, long[] agentId,
                          int[] agentAge, float[] agentEnergy, float[] agentHunger, float[] agentStress,
                          float[] agentPredictionError, boolean[] agentAwareness, int[] agentCultureId,
                          int[] agentCounts, int agentCount, long tickIndex, SelectedAgentDetails selectedAgentDetails) {
        this.width = width;
        this.height = height;
        this.food = food;
        this.hazard = hazard;
        this.crowding = crowding;
        this.agentX = agentX;
        this.agentY = agentY;
        this.agentColorARGB = agentColorARGB;
        this.agentId = agentId;
        this.agentAge = agentAge;
        this.agentEnergy = agentEnergy;
        this.agentHunger = agentHunger;
        this.agentStress = agentStress;
        this.agentPredictionError = agentPredictionError;
        this.agentAwareness = agentAwareness;
        this.agentCultureId = agentCultureId;
        this.agentCounts = agentCounts;
        this.agentCount = agentCount;
        this.tickIndex = tickIndex;
        this.selectedAgentDetails = selectedAgentDetails;
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

    public int[] getAgentAge() {
        return agentAge;
    }

    public float[] getAgentEnergy() {
        return agentEnergy;
    }

    public float[] getAgentHunger() {
        return agentHunger;
    }

    public float[] getAgentStress() {
        return agentStress;
    }

    public float[] getAgentPredictionError() {
        return agentPredictionError;
    }

    public boolean[] getAgentAwareness() {
        return agentAwareness;
    }

    public int[] getAgentCultureId() {
        return agentCultureId;
    }

    public int[] getAgentCounts() {
        return agentCounts;
    }

    public int getAgentCount() {
        return agentCount;
    }

    public long getTickIndex() {
        return tickIndex;
    }

    public SelectedAgentDetails getSelectedAgentDetails() {
        return selectedAgentDetails;
    }
}
