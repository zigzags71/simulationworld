package simcore.agents;

import simcore.config.SimConfig;
import simcore.events.AgentEatAttemptEvent;
import simcore.events.EventBus;
import simcore.events.SimulationEvent;
import simcore.rules.ActionType;
import simcore.rules.OutcomeVector;
import simcore.util.MathUtil;
import simcore.world.WorldGrid;

import java.util.Random;

public class ActionExecutor {
    private final Random random;
    private final EatRequestBuffer eatRequests = new EatRequestBuffer();

    public ActionExecutor(Random random) {
        this.random = random;
    }

    public void beginTick(int expectedAgents) {
        eatRequests.reset(expectedAgents);
    }

    public OutcomeVector execute(ActionType action, AgentState agent, WorldGrid world, int agentSlot, long tickIndex) {
        return switch (action) {
            case IDLE -> idle();
            case MOVE -> move(agent, world);
            case EAT -> eat(agent, world, agentSlot, tickIndex);
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

    private OutcomeVector eat(AgentState agent, WorldGrid world, int agentSlot, long tickIndex) {
        int idx = MathUtil.index(agent.getX(), agent.getY(), world.getWidth());
        eatRequests.add(idx, agent.getId().value(), agentSlot, SimConfig.FOOD_CONSUME_RATE, tickIndex);
        return OutcomeVector.zero();
    }

    public void resolveEatRequests(WorldGrid world, OutcomeVector[] actionDeltas, EventBus<SimulationEvent> eventBus) {
        float[] food = world.getFoodField();
        int requestCount = eatRequests.getSize();
        for (int i = 0; i < requestCount; i++) {
            int tileIndex = eatRequests.getTileIndex(i);
            int agentSlot = eatRequests.getAgentSlot(i);
            float desired = eatRequests.getDesiredAmount(i);
            float available = food[tileIndex];
            boolean hasFood = available >= SimConfig.FOOD_MIN_TO_EAT;
            boolean success = hasFood && available >= desired;
            float consumed = success ? desired : 0f;
            if (success) {
                food[tileIndex] = available - consumed;
            }
            float hungerGain = consumed * SimConfig.FOOD_TO_HUNGER_GAIN;
            float energyGain = consumed * SimConfig.FOOD_TO_ENERGY_GAIN;
            float stressChange = success ? -SimConfig.STRESS_RECOVERY_PER_TICK * 0.5f : SimConfig.HAZARD_STRESS_GAIN_PER_TICK * 0.2f;
            actionDeltas[agentSlot] = new OutcomeVector(energyGain, hungerGain, stressChange);
            if (eventBus != null && SimConfig.LOG_SELECTED_AGENT_ENABLED) {
                eventBus.publish(new AgentEatAttemptEvent(eatRequests.getAgentId(i), tileIndex, success, consumed,
                        eatRequests.getTick(i)));
            }
        }
    }

    private static class EatRequestBuffer {
        private int[] tileIndex = new int[0];
        private long[] agentId = new long[0];
        private int[] agentSlot = new int[0];
        private float[] desiredAmount = new float[0];
        private long[] tick = new long[0];
        private int size;

        void reset(int capacityHint) {
            ensureCapacity(capacityHint);
            size = 0;
        }

        void add(int tileIndex, long agentId, int agentSlot, float desiredAmount, long tick) {
            ensureCapacity(size + 1);
            this.tileIndex[size] = tileIndex;
            this.agentId[size] = agentId;
            this.agentSlot[size] = agentSlot;
            this.desiredAmount[size] = desiredAmount;
            this.tick[size] = tick;
            size++;
        }

        int getSize() {
            return size;
        }

        int getTileIndex(int index) {
            return tileIndex[index];
        }

        long getAgentId(int index) {
            return agentId[index];
        }

        int getAgentSlot(int index) {
            return agentSlot[index];
        }

        float getDesiredAmount(int index) {
            return desiredAmount[index];
        }

        long getTick(int index) {
            return tick[index];
        }

        private void ensureCapacity(int required) {
            if (required <= tileIndex.length) {
                return;
            }
            int newSize = Math.max(required, tileIndex.length * 2 + 8);
            tileIndex = resize(tileIndex, newSize);
            agentId = resize(agentId, newSize);
            agentSlot = resize(agentSlot, newSize);
            desiredAmount = resize(desiredAmount, newSize);
            tick = resize(tick, newSize);
        }

        private int[] resize(int[] arr, int newSize) {
            int[] next = new int[newSize];
            System.arraycopy(arr, 0, next, 0, arr.length);
            return next;
        }

        private long[] resize(long[] arr, int newSize) {
            long[] next = new long[newSize];
            System.arraycopy(arr, 0, next, 0, arr.length);
            return next;
        }

        private float[] resize(float[] arr, int newSize) {
            float[] next = new float[newSize];
            System.arraycopy(arr, 0, next, 0, arr.length);
            return next;
        }
    }
}
