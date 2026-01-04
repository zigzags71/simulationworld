package simcore.agents;

import simcore.config.SimConfig;
import simcore.util.MathUtil;

public class AgentState {
    private final AgentId id;
    private int x;
    private int y;
    private int ageTicks;
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
        this.ageTicks = 0;
        this.energy = energy;
        this.hunger = SimConfig.INITIAL_HUNGER;
        this.stress = SimConfig.INITIAL_STRESS;
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

    public boolean isAwarenessFlag() {
        return awarenessFlag;
    }

    public int getCultureId() {
        return cultureId;
    }

    public void applyTick(float energyDelta, float hungerDelta, float stressDelta, float predictionDelta) {
        this.ageTicks += 1;
        this.energy = clampMetric(energy + energyDelta);
        this.hunger = clampMetric(hunger + hungerDelta);
        this.stress = clampMetric(stress + stressDelta);
        this.predictionError = clampMetric(predictionError + predictionDelta);
        if (predictionError > SimConfig.AWARENESS_THRESHOLD) {
            awarenessFlag = true;
        }
    }

    public void moveTo(int newX, int newY) {
        this.x = newX;
        this.y = newY;
    }

    public void applyNutrition(float hungerGain, float energyGain) {
        this.hunger = clampMetric(hunger + hungerGain);
        this.energy = clampMetric(energy + energyGain);
    }

    public boolean isDead() {
        return energy <= 0f || hunger <= 0f || stress >= 1f;
    }

    private float clampMetric(float value) {
        return MathUtil.clamp01(value);
    }
}
