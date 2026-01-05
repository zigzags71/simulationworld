package simcore.events;

public class TelemetryEvent {
    private final long tickIndex;
    private final int population;
    private final float meanPredictionError;
    private final int deathsThisTick;
    private final int totalDeaths;
    private final int signalsEmittedThisTick;
    private final int followMovesThisTick;
    private final float meanEnergy;
    private final float meanHunger;
    private final float meanStress;
    private final float meanHazard;

    public TelemetryEvent(long tickIndex, int population, float meanPredictionError, int deathsThisTick, int totalDeaths,
                          int signalsEmittedThisTick, int followMovesThisTick,
                          float meanEnergy, float meanHunger, float meanStress, float meanHazard) {
        this.tickIndex = tickIndex;
        this.population = population;
        this.meanPredictionError = meanPredictionError;
        this.deathsThisTick = deathsThisTick;
        this.totalDeaths = totalDeaths;
        this.signalsEmittedThisTick = signalsEmittedThisTick;
        this.followMovesThisTick = followMovesThisTick;
        this.meanEnergy = meanEnergy;
        this.meanHunger = meanHunger;
        this.meanStress = meanStress;
        this.meanHazard = meanHazard;
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
}
