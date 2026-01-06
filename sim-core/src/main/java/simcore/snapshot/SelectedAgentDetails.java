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
    private final float socialCredit;
    private final boolean awareness;
    private final int firstNameId;
    private final int surnameId;
    private final int cultureId;
    private final RuleView[] rules;

    public SelectedAgentDetails(long agentId, int x, int y, int ageTicks, float energy, float hunger, float stress, float predictionError,
                                float socialCredit, boolean awareness, int firstNameId, int surnameId, int cultureId, RuleView[] rules) {
        this.agentId = agentId;
        this.x = x;
        this.y = y;
        this.ageTicks = ageTicks;
        this.energy = energy;
        this.hunger = hunger;
        this.stress = stress;
        this.predictionError = predictionError;
        this.socialCredit = socialCredit;
        this.awareness = awareness;
        this.firstNameId = firstNameId;
        this.surnameId = surnameId;
        this.cultureId = cultureId;
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

    public float getSocialCredit() {
        return socialCredit;
    }

    public boolean isAwareness() {
        return awareness;
    }

    public int getFirstNameId() {
        return firstNameId;
    }

    public int getSurnameId() {
        return surnameId;
    }

    public int getCultureId() {
        return cultureId;
    }

    public RuleView[] getRules() {
        return rules;
    }
}
