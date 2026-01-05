package simcore.rules;

import simcore.agents.AgentState;
import simcore.config.SimConfig;
import simcore.util.MathUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class RuleSelector {
    private RuleSelector() {
    }

    public static List<Rule> applicable(List<Rule> rules, ContextKey key) {
        List<Rule> out = new ArrayList<>();
        List<Rule> normals = new ArrayList<>();
        for (Rule rule : rules) {
            if (rule.getType() != RuleType.NORMAL) {
                continue;
            }
            normals.add(rule);
            float dist = rule.getContextKey().distanceTo(key);
            if (dist <= SimConfig.RULE_MATCH_DISTANCE) {
                out.add(rule);
            }
        }
        if (!out.isEmpty()) {
            return out;
        }
        if (!normals.isEmpty()) {
            return normals;
        }
        return rules;
    }

    public static Rule choose(List<Rule> candidates, AgentState agent, Random random) {
        if (candidates.isEmpty()) {
            return null;
        }
        float total = 0f;
        float[] weights = new float[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            Rule rule = candidates.get(i);
            float trust = Math.max(SimConfig.TRUST_EPSILON, rule.getTrust());
            float trustWeight = (float) Math.pow(trust, SimConfig.TRUST_ALPHA);
            float utility = SimConfig.RULE_NEED_UTILITY_WEIGHT * needUtility(rule.getAction(), agent);
            float weight = trustWeight + utility;
            weights[i] = weight;
            total += weight;
        }
        if (total <= 0f) {
            return candidates.get(random.nextInt(candidates.size()));
        }
        float r = random.nextFloat() * total;
        float accum = 0f;
        for (int i = 0; i < weights.length; i++) {
            accum += weights[i];
            if (r <= accum) {
                return candidates.get(i);
            }
        }
        return candidates.get(candidates.size() - 1);
    }

    private static float needUtility(ActionType action, AgentState agent) {
        return switch (action) {
            case EAT -> 1f - agent.getHunger();
            case MOVE -> MathUtil.clamp01(agent.getStress());
            case IDLE -> MathUtil.clamp01(1f - agent.getStress());
            case BROADCAST_SIGNAL -> MathUtil.clamp01(agent.getHunger()) * 0.5f
                    + MathUtil.clamp01(agent.getStress()) * 0.1f;
            case FOLLOW_SIGNAL -> MathUtil.clamp01(1f - agent.getHunger());
        };
    }
}
