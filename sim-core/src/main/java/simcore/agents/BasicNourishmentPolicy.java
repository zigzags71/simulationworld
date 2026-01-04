package simcore.agents;

import simcore.config.SimConfig;
import simcore.util.MathUtil;
import simcore.world.WorldGrid;

public class BasicNourishmentPolicy implements NourishmentPolicy {
    @Override
    public void applyNutrition(AgentState agent, WorldGrid world) {
        if (agent.getHunger() >= SimConfig.CONSUME_HUNGER_THRESHOLD) {
            return;
        }
        int idx = MathUtil.index(agent.getX(), agent.getY(), world.getWidth());
        float[] food = world.getFoodField();
        float available = food[idx];
        if (available < SimConfig.FOOD_MIN_TO_EAT) {
            return;
        }
        float consume = Math.min(available, SimConfig.FOOD_CONSUME_RATE);
        food[idx] = available - consume;
        agent.applyNutrition(consume * SimConfig.FOOD_TO_HUNGER_GAIN, consume * SimConfig.FOOD_TO_ENERGY_GAIN);
    }
}
