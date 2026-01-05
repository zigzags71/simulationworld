package simcore.agents;

import simcore.config.SimConfig;
import simcore.events.AgentEatAttemptEvent;
import simcore.events.EventBus;
import simcore.events.SimulationEvent;
import simcore.rules.ActionType;
import simcore.rules.OutcomeVector;
import simcore.util.BinningUtil;
import simcore.util.MathUtil;
import simcore.world.WorldGrid;
import simcore.world.objects.FoodEmitter;
import simcore.world.signals.Signal;
import simcore.world.signals.SignalField;

import java.util.Random;

public class ActionExecutor {
    private final Random random;
    private final EatRequestBuffer eatRequests = new EatRequestBuffer();
    private final TileRequestIndex tileRequestIndex = new TileRequestIndex();
    private final RequestOrderBuffer requestOrder = new RequestOrderBuffer();
    private AgentTickMetrics metrics;

    public ActionExecutor(Random random) {
        this.random = random;
    }

    public void beginTick(int expectedAgents) {
        eatRequests.reset(expectedAgents);
    }

    public void setMetrics(AgentTickMetrics metrics) {
        this.metrics = metrics;
    }

    public OutcomeVector execute(ActionType action, AgentState agent, WorldGrid world, int agentSlot, long tickIndex) {
        return switch (action) {
            case IDLE -> idle();
            case MOVE -> move(agent, world);
            case EAT -> eat(agent, world, agentSlot, tickIndex);
            case BROADCAST_SIGNAL -> broadcast(agent, world, tickIndex);
            case FOLLOW_SIGNAL -> followSignalMove(agent, world, tickIndex);
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

    private OutcomeVector broadcast(AgentState agent, WorldGrid world, long tickIndex) {
        int idx = MathUtil.index(agent.getX(), agent.getY(), world.getWidth());
        float localFood = world.getFoodField()[idx];
        boolean emitterNearby = world.findEmitterAt(agent.getX(), agent.getY()) != null;
        if (!emitterNearby) {
            for (FoodEmitter emitter : world.getEmittersView()) {
                if (Math.abs(emitter.getX() - agent.getX()) <= 2 && Math.abs(emitter.getY() - agent.getY()) <= 2) {
                    emitterNearby = true;
                    break;
                }
            }
        }
        boolean ateRecently = agent.getLastSuccessfulEatTick() >= 0
                && (tickIndex - agent.getLastSuccessfulEatTick()) <= SimConfig.SIGNAL_BROADCAST_AFTER_EAT_WINDOW;
        boolean cooldownReady = agent.getLastBroadcastTick() < 0
                || (tickIndex - agent.getLastBroadcastTick()) >= SimConfig.SIGNAL_BROADCAST_COOLDOWN_TICKS;
        boolean signalCreated = false;
        if ((ateRecently || emitterNearby) && cooldownReady) {
            if (localFood >= SimConfig.FOOD_MIN_TO_EAT || emitterNearby) {
                int strengthBucket = BinningUtil.bin01(localFood, SimConfig.SIGNAL_STRENGTH_BINS);
                world.getSignalField().addSignal(agent.getX(), agent.getY(), strengthBucket, SimConfig.SIGNAL_BASE_CONFIDENCE,
                        SimConfig.SIGNAL_TTL_TICKS, 0, agent.getId().value(), agent.getSocialCredit(), tickIndex);
                agent.setLastBroadcastTick(tickIndex);
                if (metrics != null) {
                    metrics.incrementSignalsEmitted();
                }
                signalCreated = true;
            }
        }
        if (signalCreated) {
            return new OutcomeVector(-SimConfig.SIGNAL_BROADCAST_COST_ENERGY, -SimConfig.SIGNAL_BROADCAST_COST_HUNGER, 0f);
        }
        return OutcomeVector.zero();
    }

    private OutcomeVector followSignalMove(AgentState agent, WorldGrid world, long tickIndex) {
        SignalField field = world.getSignalField();
        if (field.isEmpty()) {
            return move(agent, world);
        }
        int baseRadius = SimConfig.SIGNAL_SENSE_RADIUS_BASE;
        int dynamic = (int) (agent.getPredictionError() * (SimConfig.SIGNAL_SENSE_RADIUS_MAX - baseRadius));
        int radius = Math.min(SimConfig.SIGNAL_SENSE_RADIUS_MAX, baseRadius + Math.max(0, dynamic));
        Signal best = field.bestSignalWithinRadius(agent.getX(), agent.getY(), radius);
        if (best == null) {
            return move(agent, world);
        }
        int dx = Integer.compare(best.getX(), agent.getX());
        int dy = Integer.compare(best.getY(), agent.getY());
        int targetX = MathUtil.clamp(agent.getX() + dx, 0, world.getWidth() - 1);
        int targetY = MathUtil.clamp(agent.getY() + dy, 0, world.getHeight() - 1);
        int idx = MathUtil.index(targetX, targetY, world.getWidth());
        if (world.getWaterMask()[idx]) {
            if (!world.getWaterMask()[MathUtil.index(MathUtil.clamp(agent.getX() + dx, 0, world.getWidth() - 1), agent.getY(), world.getWidth())]) {
                targetX = MathUtil.clamp(agent.getX() + dx, 0, world.getWidth() - 1);
                targetY = agent.getY();
            } else if (!world.getWaterMask()[MathUtil.index(agent.getX(), MathUtil.clamp(agent.getY() + dy, 0, world.getHeight() - 1), world.getWidth())]) {
                targetX = agent.getX();
                targetY = MathUtil.clamp(agent.getY() + dy, 0, world.getHeight() - 1);
            }
        }
        agent.moveTo(targetX, targetY);
        agent.setFollowMemory(best.getId(), -1, best.getOriginAgentId(), tickIndex);
        if (metrics != null) {
            metrics.incrementFollowMoves();
        }
        return new OutcomeVector(-SimConfig.MOVE_ENERGY_COST, -SimConfig.MOVE_HUNGER_COST, 0f);
    }

    public void resolveEatRequests(WorldGrid world, OutcomeVector[] actionDeltas, EventBus<SimulationEvent> eventBus) {
        float[] food = world.getFoodField();
        int requestCount = eatRequests.getSize();
        if (requestCount == 0) {
            return;
        }
        int tileCount = world.getWidth() * world.getHeight();
        tileRequestIndex.ensureCapacity(tileCount);
        requestOrder.ensureCapacity(requestCount);
        tileRequestIndex.beginBatch();
        for (int i = 0; i < requestCount; i++) {
            tileRequestIndex.countRequest(eatRequests.getTileIndex(i));
        }
        tileRequestIndex.buildOffsets();
        for (int i = 0; i < requestCount; i++) {
            int tileIndex = eatRequests.getTileIndex(i);
            int writePos = tileRequestIndex.claimSlot(tileIndex);
            requestOrder.set(writePos, i);
        }
        int touchedTileCount = tileRequestIndex.getTouchedCount();
        for (int i = 0; i < touchedTileCount; i++) {
            int tileIndex = tileRequestIndex.getTouchedTile(i);
            int start = tileRequestIndex.getOffset(tileIndex);
            int count = tileRequestIndex.getCount(tileIndex);
            resolveEatRequestsForTile(tileIndex, start, count, food, actionDeltas, eventBus);
            tileRequestIndex.resetTile(tileIndex);
        }
    }

    private void resolveEatRequestsForTile(int tileIndex, int start, int count, float[] food, OutcomeVector[] actionDeltas,
                                           EventBus<SimulationEvent> eventBus) {
        if (count == 0) {
            return;
        }
        requestOrder.sortByAgentId(start, count, eatRequests);
        float available = food[tileIndex];
        for (int i = start; i < start + count; i++) {
            int requestIndex = requestOrder.get(i);
            int agentSlot = eatRequests.getAgentSlot(requestIndex);
            float desired = eatRequests.getDesiredAmount(requestIndex);
            float consumed = Math.min(desired, available);
            boolean success = consumed > 0f;
            available -= consumed;
            float hungerGain = consumed * SimConfig.FOOD_TO_HUNGER_GAIN;
            float energyGain = consumed * SimConfig.FOOD_TO_ENERGY_GAIN;
            boolean fullBite = consumed >= desired;
            float stressChange = success
                    ? (fullBite ? -SimConfig.STRESS_RECOVERY_PER_TICK * 0.5f : SimConfig.HAZARD_STRESS_GAIN_PER_TICK * 0.1f)
                    : SimConfig.HAZARD_STRESS_GAIN_PER_TICK * 0.2f;
            actionDeltas[agentSlot] = new OutcomeVector(energyGain, hungerGain, stressChange);
            if (eventBus != null && SimConfig.LOG_SELECTED_AGENT_ENABLED) {
                eventBus.publish(new AgentEatAttemptEvent(eatRequests.getAgentId(requestIndex), tileIndex, success, consumed,
                        eatRequests.getTick(requestIndex)));
            }
        }
        food[tileIndex] = Math.max(0f, available);
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

    private static class TileRequestIndex {
        private int[] counts = new int[0];
        private int[] offsets = new int[0];
        private int[] fill = new int[0];
        private int[] touched = new int[0];
        private int touchedCount;

        void ensureCapacity(int tiles) {
            if (counts.length >= tiles) {
                return;
            }
            counts = resize(counts, tiles);
            offsets = resize(offsets, tiles);
            fill = resize(fill, tiles);
            touched = resize(touched, tiles);
        }

        void beginBatch() {
            touchedCount = 0;
        }

        void countRequest(int tileIndex) {
            if (counts[tileIndex] == 0) {
                touched[touchedCount++] = tileIndex;
            }
            counts[tileIndex] += 1;
        }

        void buildOffsets() {
            int offset = 0;
            for (int i = 0; i < touchedCount; i++) {
                int tile = touched[i];
                offsets[tile] = offset;
                fill[tile] = offset;
                offset += counts[tile];
            }
        }

        int claimSlot(int tileIndex) {
            int pos = fill[tileIndex];
            fill[tileIndex] = pos + 1;
            return pos;
        }

        int getOffset(int tileIndex) {
            return offsets[tileIndex];
        }

        int getCount(int tileIndex) {
            return counts[tileIndex];
        }

        int getTouchedCount() {
            return touchedCount;
        }

        int getTouchedTile(int index) {
            return touched[index];
        }

        void resetTile(int tileIndex) {
            counts[tileIndex] = 0;
            offsets[tileIndex] = 0;
            fill[tileIndex] = 0;
        }

        private int[] resize(int[] arr, int newSize) {
            int[] next = new int[newSize];
            System.arraycopy(arr, 0, next, 0, arr.length);
            return next;
        }
    }

    private static class RequestOrderBuffer {
        private int[] order = new int[0];

        void ensureCapacity(int count) {
            if (order.length >= count) {
                return;
            }
            order = resize(order, count);
        }

        void set(int index, int requestIndex) {
            order[index] = requestIndex;
        }

        int get(int index) {
            return order[index];
        }

        void sortByAgentId(int start, int count, EatRequestBuffer requests) {
            quicksort(start, start + count - 1, requests);
        }

        private void quicksort(int low, int high, EatRequestBuffer requests) {
            int i = low;
            int j = high;
            long pivot = requests.getAgentId(order[low + (high - low) / 2]);
            while (i <= j) {
                while (requests.getAgentId(order[i]) < pivot) {
                    i++;
                }
                while (requests.getAgentId(order[j]) > pivot) {
                    j--;
                }
                if (i <= j) {
                    int tmp = order[i];
                    order[i] = order[j];
                    order[j] = tmp;
                    i++;
                    j--;
                }
            }
            if (low < j) {
                quicksort(low, j, requests);
            }
            if (i < high) {
                quicksort(i, high, requests);
            }
        }

        private int[] resize(int[] arr, int newSize) {
            int[] next = new int[newSize];
            System.arraycopy(arr, 0, next, 0, arr.length);
            return next;
        }
    }
}
