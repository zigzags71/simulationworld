package simcore.snapshot;

public class SelectedAgentDetails {
    private final long agentId;
    private final int x;
    private final int y;
    private final int ageTicks;
    private final float energy;
    private final float hunger;
    private final float stress;
    private final float predictionError;
    private final boolean awareness;
    private final RuleView[] rules;

    public SelectedAgentDetails(long agentId, int x, int y, int ageTicks, float energy, float hunger, float stress, float predictionError,
                                boolean awareness, RuleView[] rules) {
        this.agentId = agentId;
        this.x = x;
        this.y = y;
        this.ageTicks = ageTicks;
        this.energy = energy;
        this.hunger = hunger;
        this.stress = stress;
        this.predictionError = predictionError;
        this.awareness = awareness;
        this.rules = rules;
    }

    public long getAgentId() {
        return agentId;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getAgeTicks() {
        return ageTicks;
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

    public boolean isAwareness() {
        return awareness;
    }

    public RuleView[] getRules() {
        return rules;
    }
}
