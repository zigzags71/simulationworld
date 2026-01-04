package simcore.snapshot;

import simcore.rules.ActionType;
import simcore.rules.RuleType;

public class RuleView {
    private final long ruleId;
    private final RuleType type;
    private final String contextSummary;
    private final ActionType action;
    private final float trust;
    private final int uses;
    private final int successes;
    private final long lastUsedTick;
    private final float lastError;

    public RuleView(long ruleId, RuleType type, String contextSummary, ActionType action, float trust, int uses, int successes,
                    long lastUsedTick, float lastError) {
        this.ruleId = ruleId;
        this.type = type;
        this.contextSummary = contextSummary;
        this.action = action;
        this.trust = trust;
        this.uses = uses;
        this.successes = successes;
        this.lastUsedTick = lastUsedTick;
        this.lastError = lastError;
    }

    public long getRuleId() {
        return ruleId;
    }

    public RuleType getType() {
        return type;
    }

    public String getContextSummary() {
        return contextSummary;
    }

    public ActionType getAction() {
        return action;
    }

    public float getTrust() {
        return trust;
    }

    public int getUses() {
        return uses;
    }

    public int getSuccesses() {
        return successes;
    }

    public long getLastUsedTick() {
        return lastUsedTick;
    }

    public float getLastError() {
        return lastError;
    }
}
