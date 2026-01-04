package simcore.events;

public class SelectedRegionSnapshotEvent implements SimulationEvent {
    private final int minX;
    private final int minY;
    private final int maxX;
    private final int maxY;
    private final int agentCount;
    private final float avgEnergy;
    private final float avgHunger;
    private final float avgStress;
    private final float avgPredictionError;
    private final long tick;

    public SelectedRegionSnapshotEvent(int minX, int minY, int maxX, int maxY, int agentCount, float avgEnergy,
                                       float avgHunger, float avgStress, float avgPredictionError, long tick) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.agentCount = agentCount;
        this.avgEnergy = avgEnergy;
        this.avgHunger = avgHunger;
        this.avgStress = avgStress;
        this.avgPredictionError = avgPredictionError;
        this.tick = tick;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getAgentCount() {
        return agentCount;
    }

    public float getAvgEnergy() {
        return avgEnergy;
    }

    public float getAvgHunger() {
        return avgHunger;
    }

    public float getAvgStress() {
        return avgStress;
    }

    public float getAvgPredictionError() {
        return avgPredictionError;
    }

    public long getTick() {
        return tick;
    }
}
