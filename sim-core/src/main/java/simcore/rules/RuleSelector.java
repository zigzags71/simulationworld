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
            return filterByAffordance(out, key, normals);
        }
        if (!normals.isEmpty()) {
            return filterByAffordance(normals, key, normals);
        }
        return rules;
    }

    private static List<Rule> filterByAffordance(List<Rule> candidates, ContextKey key, List<Rule> normals) {
        List<Rule> filtered = new ArrayList<>();
        for (Rule rule : candidates) {
            if (isActionAvailable(rule.getAction(), key)) {
                filtered.add(rule);
            }
        }
        if (!filtered.isEmpty()) {
            List<Rule> nonBasic = new ArrayList<>();
            for (Rule rule : filtered) {
                if (rule.getAction() == ActionType.EAT || rule.getAction() == ActionType.FOLLOW_SIGNAL
                        || rule.getAction() == ActionType.BROADCAST_SIGNAL) {
                    nonBasic.add(rule);
                }
            }
            if (!nonBasic.isEmpty()) {
                return nonBasic;
            }
            return filtered;
        }
        List<Rule> fallback = new ArrayList<>();
        for (Rule rule : normals) {
            if (rule.getAction() == ActionType.MOVE || rule.getAction() == ActionType.IDLE) {
                fallback.add(rule);
            }
        }
        if (!fallback.isEmpty()) {
            return fallback;
        }
        return filtered;
    }

    private static boolean isActionAvailable(ActionType action, ContextKey key) {
        int affordance = key.getFoodAffordance();
        boolean hasAnyFoodHere = (affordance & 1) != 0;
        boolean hasGoodFoodHere = (affordance & (1 << 4)) != 0;
        boolean hasSignal = (affordance & (1 << 1)) != 0;
        boolean emitterNearby = (affordance & (1 << 2)) != 0;
        boolean broadcastReady = (affordance & (1 << 3)) != 0;
        boolean broadcastAvailable = (affordance & (1 << 5)) != 0;
        return switch (action) {
            case EAT -> hasGoodFoodHere;
            case FOLLOW_SIGNAL -> hasSignal && !hasGoodFoodHere;
            case BROADCAST_SIGNAL -> broadcastAvailable && emitterNearby && broadcastReady;
            case MOVE, IDLE -> true;
        };
    }

    public static Rule choose(List<Rule> candidates, AgentState agent, Random random) {
        if (candidates.isEmpty()) {
            return null;
        }
        boolean hungry = agent.getHunger() < SimConfig.CONSUME_HUNGER_THRESHOLD;
        if (hungry) {
            boolean hasEat = candidates.stream().anyMatch(rule -> rule.getAction() == ActionType.EAT);
            if (!hasEat) {
                Rule bestMove = null;
                for (Rule rule : candidates) {
                    if (rule.getAction() == ActionType.MOVE) {
                        if (bestMove == null || rule.getTrust() > bestMove.getTrust()) {
                            bestMove = rule;
                        }
                    }
                }
                if (bestMove != null) {
                    return bestMove;
                }
            }
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
        float hunger = MathUtil.clamp01(agent.getHunger());
        float stress = MathUtil.clamp01(agent.getStress());
        float hungry = 1f - hunger;

        return switch (action) {
            case EAT -> hungry;

            case MOVE -> MathUtil.clamp01(hungry * 1.0f + stress * 0.15f);

            case IDLE -> MathUtil.clamp01(hunger * (1f - stress));

            case BROADCAST_SIGNAL -> MathUtil.clamp01(hunger) * 0.5f + stress * 0.1f;
            case FOLLOW_SIGNAL -> hungry;
        };
    }
}
