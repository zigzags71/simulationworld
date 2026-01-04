package simcore.events;

import simcore.rules.ActionType;

public class RuleExecutedEvent implements SimulationEvent {
    private final long agentId;
    private final long ruleId;
    private final ActionType action;
    private final float trust;
    private final float predictionError;
    private final long tick;

    public RuleExecutedEvent(long agentId, long ruleId, ActionType action, float trust, float predictionError, long tick) {
        this.agentId = agentId;
        this.ruleId = ruleId;
        this.action = action;
        this.trust = trust;
        this.predictionError = predictionError;
        this.tick = tick;
    }

    public long getAgentId() {
        return agentId;
    }

    public long getRuleId() {
        return ruleId;
    }

    public ActionType getAction() {
        return action;
    }

    public float getTrust() {
        return trust;
    }

    public float getPredictionError() {
        return predictionError;
    }

    public long getTick() {
        return tick;
    }
}
