package simcore.events;

public class TelemetryEvent {
    private final long tickIndex;
    private final int population;
    private final float meanPredictionError;
    private final int deathsThisTick;
    private final int totalDeaths;
    private final int signalsEmittedThisTick;
    private final int followMovesThisTick;
    private final int activeSignalsCount;
    private final long totalSignalsEmitted;
    private final long totalFollowMoves;
    private final float meanEnergy;
    private final float meanHunger;
    private final float meanStress;
    private final float meanHazard;
    private final int emitterCount;
    private final int spawnerCount;
    private final float estimatedFoodPerTick;
    private final float estimatedMaxEatersPerTick;

    public TelemetryEvent(long tickIndex, int population, float meanPredictionError, int deathsThisTick, int totalDeaths,
                          int signalsEmittedThisTick, int followMovesThisTick,
                          int activeSignalsCount,
                          long totalSignalsEmitted, long totalFollowMoves,
                          float meanEnergy, float meanHunger, float meanStress, float meanHazard,
                          int emitterCount, int spawnerCount,
                          float estimatedFoodPerTick, float estimatedMaxEatersPerTick) {
        this.tickIndex = tickIndex;
        this.population = population;
        this.meanPredictionError = meanPredictionError;
        this.deathsThisTick = deathsThisTick;
        this.totalDeaths = totalDeaths;
        this.signalsEmittedThisTick = signalsEmittedThisTick;
        this.followMovesThisTick = followMovesThisTick;
        this.activeSignalsCount = activeSignalsCount;
        this.totalSignalsEmitted = totalSignalsEmitted;
        this.totalFollowMoves = totalFollowMoves;
        this.meanEnergy = meanEnergy;
        this.meanHunger = meanHunger;
        this.meanStress = meanStress;
        this.meanHazard = meanHazard;
        this.emitterCount = emitterCount;
        this.spawnerCount = spawnerCount;
        this.estimatedFoodPerTick = estimatedFoodPerTick;
        this.estimatedMaxEatersPerTick = estimatedMaxEatersPerTick;
    }

    public long getTickIndex() {
        return tickIndex;
    }

    public int getPopulation() {
        return population;
    }

    public float getMeanPredictionError() {
        return meanPredictionError;
    }

    public int getDeathsThisTick() {
        return deathsThisTick;
    }

    public int getTotalDeaths() {
        return totalDeaths;
    }

    public int getSignalsEmittedThisTick() {
        return signalsEmittedThisTick;
    }

    public int getFollowMovesThisTick() {
        return followMovesThisTick;
    }

    public int getActiveSignalsCount() {
        return activeSignalsCount;
    }

    public long getTotalSignalsEmitted() {
        return totalSignalsEmitted;
    }

    public long getTotalFollowMoves() {
        return totalFollowMoves;
    }

    public float getMeanEnergy() {
        return meanEnergy;
    }

    public float getMeanHunger() {
        return meanHunger;
    }

    public float getMeanStress() {
        return meanStress;
    }

    public float getMeanHazard() {
        return meanHazard;
    }

    public int getEmitterCount() {
        return emitterCount;
    }

    public int getSpawnerCount() {
        return spawnerCount;
    }

    public float getEstimatedFoodPerTick() {
        return estimatedFoodPerTick;
    }

    public float getEstimatedMaxEatersPerTick() {
        return estimatedMaxEatersPerTick;
    }
}
