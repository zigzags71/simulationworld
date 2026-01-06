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
    private float socialCredit;
    private int awarenessLevel;
    private final int firstNameId;
    private final int surnameId;
    private int cultureId;
    private final List<Rule> rulebook;
    private long nextRuleId;
    private long lastFollowSignalId = -1;
    private long lastFollowRuleId = -1;
    private long lastFollowOriginAgentId = -1;
    private long lastFollowTick = -1;
    private long lastSuccessfulEatTick = -1;
    private long lastBroadcastTick = -1;
    private int followLockTargetX;
    private int followLockTargetY;
    private long followLockUntilTick = -1;
    private int foodLockTargetX;
    private int foodLockTargetY;
    private long foodLockUntilTick = -1;
    private int lastX;
    private int lastY;

    public AgentState(AgentId id, int x, int y, float energy, int firstNameId, int surnameId, int cultureId) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.lastX = x;
        this.lastY = y;
        this.ageTicks = 0;
        this.energy = energy;
        this.hunger = SimConfig.INITIAL_HUNGER;
        this.stress = SimConfig.INITIAL_STRESS;
        this.socialCredit = SimConfig.INITIAL_SOCIAL_CREDIT;
        this.predictionError = 0f;
        this.awarenessLevel = 0;
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
        state.socialCredit = SimConfig.INITIAL_SOCIAL_CREDIT;
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

    public void setStress(float stress) {
        this.stress = clampMetric(stress);
    }

    public float getPredictionError() {
        return predictionError;
    }

    public float getSocialCredit() {
        return socialCredit;
    }

    public boolean isAwarenessFlag() {
        return awarenessLevel > 0;
    }

    public int getAwarenessLevel() {
        return awarenessLevel;
    }

    public void setAwarenessLevel(int awarenessLevel) {
        this.awarenessLevel = clampAwarenessLevel(awarenessLevel);
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

    public long getLastFollowOriginAgentId() {
        return lastFollowOriginAgentId;
    }

    public void setFollowMemory(long signalId, long ruleId, long originAgentId, long tick) {
        this.lastFollowSignalId = signalId;
        this.lastFollowRuleId = ruleId;
        this.lastFollowOriginAgentId = originAgentId;
        this.lastFollowTick = tick;
    }

    public void clearFollowMemory() {
        this.lastFollowSignalId = -1;
        this.lastFollowRuleId = -1;
        this.lastFollowOriginAgentId = -1;
        this.lastFollowTick = -1;
    }

    public long getLastSuccessfulEatTick() {
        return lastSuccessfulEatTick;
    }

    public void setLastSuccessfulEatTick(long tick) {
        this.lastSuccessfulEatTick = tick;
    }

    public long getLastBroadcastTick() {
        return lastBroadcastTick;
    }

    public void setLastBroadcastTick(long tick) {
        this.lastBroadcastTick = tick;
    }

    public void addSocialCredit(float delta) {
        this.socialCredit = clampMetric(socialCredit + delta);
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

    void updateAwarenessLevelFromStress(float threshold1, float threshold2, float threshold3, float hysteresis) {
        int currentLevel = awarenessLevel;
        int targetLevel;
        if (stress > threshold3) {
            targetLevel = 3;
        } else if (stress > threshold2) {
            targetLevel = 2;
        } else if (stress > threshold1) {
            targetLevel = 1;
        } else {
            targetLevel = 0;
        }

        int newLevel = currentLevel;
        if (targetLevel > currentLevel) {
            newLevel = targetLevel;
        } else {
            if (currentLevel == 3) {
                newLevel = (stress < (threshold3 - hysteresis)) ? 2 : 3;
            } else if (currentLevel == 2) {
                newLevel = (stress < (threshold2 - hysteresis)) ? 1 : 2;
            } else if (currentLevel == 1) {
                newLevel = (stress < (threshold1 - hysteresis)) ? 0 : 1;
            } else {
                newLevel = 0;
            }
        }
        setAwarenessLevel(newLevel);
    }

    public void updatePredictionError(float observedError) {
        float alpha = MathUtil.clamp01(SimConfig.PRED_ERROR_EMA_ALPHA);
        this.predictionError = clampMetric((1f - alpha) * predictionError + alpha * observedError);
    }

    public void moveTo(int newX, int newY) {
        this.x = newX;
        this.y = newY;
    }

    public void snapshotLastPos() {
        this.lastX = x;
        this.lastY = y;
    }

    public int getLastX() {
        return lastX;
    }

    public int getLastY() {
        return lastY;
    }

    public boolean hasFollowLock(long tick) {
        return tick <= followLockUntilTick;
    }

    public void setFollowLock(int tx, int ty, long untilTick) {
        this.followLockTargetX = tx;
        this.followLockTargetY = ty;
        this.followLockUntilTick = untilTick;
    }

    public void clearFollowLock() {
        this.followLockUntilTick = -1;
    }

    public int getFollowLockTargetX() {
        return followLockTargetX;
    }

    public int getFollowLockTargetY() {
        return followLockTargetY;
    }

    public boolean hasFoodLock(long tick) {
        return tick <= foodLockUntilTick;
    }

    public void setFoodLock(int tx, int ty, long untilTick) {
        this.foodLockTargetX = tx;
        this.foodLockTargetY = ty;
        this.foodLockUntilTick = untilTick;
    }

    public void clearFoodLock() {
        this.foodLockUntilTick = -1;
    }

    public int getFoodLockTargetX() {
        return foodLockTargetX;
    }

    public int getFoodLockTargetY() {
        return foodLockTargetY;
    }

    public void applyNutrition(float hungerGain, float energyGain) {
        this.hunger = clampMetric(hunger + hungerGain);
        this.energy = clampMetric(energy + energyGain);
    }

    public boolean isDead() {
        return energy <= 0f;
    }

    private float clampMetric(float value) {
        return MathUtil.clamp01(value);
    }

    private int clampAwarenessLevel(int value) {
        return Math.max(0, Math.min(3, value));
    }
}
