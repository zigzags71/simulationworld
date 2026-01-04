package simcore.agents;

import simcore.config.SimConfig;
import simcore.rules.Rule;
import simcore.util.MathUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AgentState {
    private final AgentId id;
    private int x;
    private int y;
    private int ageTicks;
    private float energy;
    private float hunger;
    private float stress;
    private float predictionError;
    private final boolean awarenessFlag;
    private final int firstNameId;
    private final int surnameId;
    private int cultureId;
    private final List<Rule> rulebook;
    private long nextRuleId;
    private long lastFollowSignalId = -1;
    private long lastFollowRuleId = -1;
    private long lastFollowTick = -1;

    public AgentState(AgentId id, int x, int y, float energy, int firstNameId, int surnameId, int cultureId) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.ageTicks = 0;
        this.energy = energy;
        this.hunger = SimConfig.INITIAL_HUNGER;
        this.stress = SimConfig.INITIAL_STRESS;
        this.predictionError = 0f;
        this.awarenessFlag = false;
        this.firstNameId = firstNameId;
        this.surnameId = surnameId;
        this.cultureId = cultureId;
        this.rulebook = new ArrayList<>();
        this.nextRuleId = 1;
    }

    public static AgentState forTest(AgentId id, int x, int y, float energy, float hunger, float stress, float predictionError) {
        AgentState state = new AgentState(id, x, y, energy, 0, 0, 0);
        state.ageTicks = 0;
        state.energy = state.clampMetric(energy);
        state.hunger = state.clampMetric(hunger);
        state.stress = state.clampMetric(stress);
        state.predictionError = state.clampMetric(predictionError);
        return state;
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

    public int getFirstNameId() {
        return firstNameId;
    }

    public int getSurnameId() {
        return surnameId;
    }

    public int getCultureId() {
        return cultureId;
    }

    public List<Rule> getRulebook() {
        return Collections.unmodifiableList(rulebook);
    }

    public void addRule(Rule rule) {
        this.rulebook.add(rule);
    }

    public long getLastFollowSignalId() {
        return lastFollowSignalId;
    }

    public long getLastFollowRuleId() {
        return lastFollowRuleId;
    }

    public long getLastFollowTick() {
        return lastFollowTick;
    }

    public void setFollowMemory(long signalId, long ruleId, long tick) {
        this.lastFollowSignalId = signalId;
        this.lastFollowRuleId = ruleId;
        this.lastFollowTick = tick;
    }

    public void clearFollowMemory() {
        this.lastFollowSignalId = -1;
        this.lastFollowRuleId = -1;
        this.lastFollowTick = -1;
    }

    public long allocateRuleId() {
        return nextRuleId++;
    }

    public void applyTick(float energyDelta, float hungerDelta, float stressDelta) {
        this.ageTicks += 1;
        this.energy = clampMetric(energy + energyDelta);
        this.hunger = clampMetric(hunger + hungerDelta);
        this.stress = clampMetric(stress + stressDelta);
    }

    public void updatePredictionError(float observedError) {
        float alpha = MathUtil.clamp01(SimConfig.PRED_ERROR_EMA_ALPHA);
        this.predictionError = clampMetric((1f - alpha) * predictionError + alpha * observedError);
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
