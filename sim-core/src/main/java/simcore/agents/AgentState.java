package simcore.agents;

public class AgentState {
    private final AgentId id;
    private int x;
    private int y;
    private int age;
    private float energy;
    private float hunger;
    private float stress;
    private float predictionError;
    private boolean awarenessFlag;
    private int cultureId;

    public AgentState(AgentId id, int x, int y, float energy, int cultureId) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.age = 0;
        this.energy = energy;
        this.hunger = 0f;
        this.stress = 0f;
        this.predictionError = 0f;
        this.awarenessFlag = false;
        this.cultureId = cultureId;
    }

    public AgentId getId() {
        return id;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getAge() {
        return age;
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

    public boolean isAwarenessFlag() {
        return awarenessFlag;
    }

    public int getCultureId() {
        return cultureId;
    }

    public void updateStats(float energyDelta, float hungerDelta, float stressDelta, float predictionDelta) {
        this.age += 1;
        this.energy = clampMetric(energy + energyDelta);
        this.hunger = clampMetric(hunger + hungerDelta);
        this.stress = clampMetric(stress + stressDelta);
        this.predictionError = clampMetric(predictionError + predictionDelta);
        if (predictionError > 0.7f) {
            awarenessFlag = true;
        }
    }

    public void moveTo(int newX, int newY) {
        this.x = newX;
        this.y = newY;
    }

    private float clampMetric(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
