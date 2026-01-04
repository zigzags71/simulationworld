package simcore.events;

public class TelemetryEvent {
    private final long tickIndex;
    private final int population;
    private final float meanPredictionError;
    private final int deathsThisTick;
    private final int totalDeaths;
    private final float meanEnergy;
    private final float meanHunger;
    private final float meanStress;
    private final float meanHazard;

    public TelemetryEvent(long tickIndex, int population, float meanPredictionError, int deathsThisTick, int totalDeaths,
                          float meanEnergy, float meanHunger, float meanStress, float meanHazard) {
        this.tickIndex = tickIndex;
        this.population = population;
        this.meanPredictionError = meanPredictionError;
        this.deathsThisTick = deathsThisTick;
        this.totalDeaths = totalDeaths;
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
