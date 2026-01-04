package simcore.events;

public class TelemetryEvent {
    private final long tickIndex;
    private final int population;
    private final float meanPredictionError;

    public TelemetryEvent(long tickIndex, int population, float meanPredictionError) {
        this.tickIndex = tickIndex;
        this.population = population;
        this.meanPredictionError = meanPredictionError;
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
}
