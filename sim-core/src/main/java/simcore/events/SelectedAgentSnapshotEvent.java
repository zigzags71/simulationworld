package simcore.events;

public class SelectedAgentSnapshotEvent implements SimulationEvent {
    private final long agentId;
    private final float energy;
    private final float hunger;
    private final float stress;
    private final float predictionError;
    private final long tick;

    public SelectedAgentSnapshotEvent(long agentId, float energy, float hunger, float stress, float predictionError, long tick) {
        this.agentId = agentId;
        this.energy = energy;
        this.hunger = hunger;
        this.stress = stress;
        this.predictionError = predictionError;
        this.tick = tick;
    }

    public long getAgentId() {
        return agentId;
    }

    public float getEnergy() {
        return energy;
    }

    public float getHunger() {
        return hunger;
    }

    public float getStress() {
        return stress;
    }

    public float getPredictionError() {
        return predictionError;
    }

    public long getTick() {
        return tick;
    }
}
