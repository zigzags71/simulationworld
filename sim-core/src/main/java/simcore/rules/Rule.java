package simcore.rules;

public class Rule {
    private final long ruleId;
    private final RuleType type;
    private final ContextKey contextKey;
    private final ActionType action;
    private final OutcomeVector expected;
    private float trust;
    private int uses;
    private int successes;
    private long lastUsedTick;
    private float lastError;

    public Rule(long ruleId, RuleType type, ContextKey contextKey, ActionType action, OutcomeVector expected, float trust) {
        this.ruleId = ruleId;
        this.type = type;
        this.contextKey = contextKey;
        this.action = action;
        this.expected = expected;
        this.trust = trust;
        this.uses = 0;
        this.successes = 0;
        this.lastUsedTick = -1;
        this.lastError = 0f;
    }

    public long getRuleId() {
        return ruleId;
    }

    public RuleType getType() {
        return type;
    }

    public ContextKey getContextKey() {
        return contextKey;
    }

    public ActionType getAction() {
        return action;
    }

    public OutcomeVector getExpected() {
        return expected;
    }

    public float getTrust() {
        return trust;
    }

    public void setTrust(float trust) {
        this.trust = trust;
    }

    public int getUses() {
        return uses;
    }

    public void incrementUses() {
        this.uses += 1;
    }

    public int getSuccesses() {
        return successes;
    }

    public void incrementSuccesses() {
        this.successes += 1;
    }

    public long getLastUsedTick() {
        return lastUsedTick;
    }

    public void setLastUsedTick(long lastUsedTick) {
        this.lastUsedTick = lastUsedTick;
    }

    public float getLastError() {
        return lastError;
    }

    public void setLastError(float lastError) {
        this.lastError = lastError;
    }
}
