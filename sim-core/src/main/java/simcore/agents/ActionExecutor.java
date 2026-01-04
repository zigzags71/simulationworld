package simcore.agents;

import simcore.config.SimConfig;
import simcore.rules.ActionType;
import simcore.rules.OutcomeVector;
import simcore.util.MathUtil;
import simcore.world.WorldGrid;

import java.util.Random;

public class ActionExecutor {
    private final Random random;

    public ActionExecutor(Random random) {
        this.random = random;
    }

    public OutcomeVector execute(ActionType action, AgentState agent, WorldGrid world) {
        return switch (action) {
            case IDLE -> idle();
            case MOVE -> move(agent, world);
            case EAT -> eat(agent, world);
        };
    }

    private OutcomeVector idle() {
        return new OutcomeVector(0f, 0f, -SimConfig.IDLE_STRESS_RECOVERY_BONUS);
    }

    private OutcomeVector move(AgentState agent, WorldGrid world) {
        int bestX = agent.getX();
        int bestY = agent.getY();
        float bestScore = Float.NEGATIVE_INFINITY;
        int width = world.getWidth();
        int height = world.getHeight();
        float[] food = world.getFoodField();
        float[] hazard = world.getHazardField();
        boolean[] water = world.getWaterMask();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                int nx = MathUtil.clamp(agent.getX() + dx, 0, width - 1);
                int ny = MathUtil.clamp(agent.getY() + dy, 0, height - 1);
                int idx = MathUtil.index(nx, ny, width);
                if (water[idx]) {
                    continue;
                }
                float score = food[idx] * SimConfig.MOVE_FOOD_WEIGHT - hazard[idx] * SimConfig.MOVE_HAZARD_WEIGHT;
                score += random.nextFloat() * 0.01f;
                if (score > bestScore) {
                    bestScore = score;
                    bestX = nx;
                    bestY = ny;
                }
            }
        }
        agent.moveTo(bestX, bestY);
        return new OutcomeVector(-SimConfig.MOVE_ENERGY_COST, -SimConfig.MOVE_HUNGER_COST, 0f);
    }

    private OutcomeVector eat(AgentState agent, WorldGrid world) {
        int idx = MathUtil.index(agent.getX(), agent.getY(), world.getWidth());
        float[] food = world.getFoodField();
        float available = food[idx];
        if (available < SimConfig.FOOD_MIN_TO_EAT) {
            return new OutcomeVector(0f, 0f, SimConfig.HAZARD_STRESS_GAIN_PER_TICK * 0.2f);
        }
        float consume = Math.min(available, SimConfig.FOOD_CONSUME_RATE);
        food[idx] = available - consume;
        float hungerGain = consume * SimConfig.FOOD_TO_HUNGER_GAIN;
        float energyGain = consume * SimConfig.FOOD_TO_ENERGY_GAIN;
        float stressChange = -SimConfig.STRESS_RECOVERY_PER_TICK * 0.5f;
        return new OutcomeVector(energyGain, hungerGain, stressChange);
    }
}
