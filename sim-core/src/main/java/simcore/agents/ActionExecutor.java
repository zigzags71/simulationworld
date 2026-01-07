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

import java.util.Arrays;
import java.util.Random;

public class ActionExecutor {
    private final Random random;
    private final EatRequestBuffer eatRequests = new EatRequestBuffer();
    private final TileRequestIndex tileRequestIndex = new TileRequestIndex();
    private final RequestOrderBuffer requestOrder = new RequestOrderBuffer();
    private AgentTickMetrics metrics;
    private float[] claimedFood;
    private int claimedFoodTileCount = -1;

    private void ensureClaimedFood(WorldGrid world) {
        int tileCount = world.getWidth() * world.getHeight();
        if (claimedFood == null || claimedFood.length != tileCount) {
            claimedFood = new float[tileCount];
        }
    }

    public ActionExecutor(Random random) {
        this.random = random;
    }

    public void prepareWorld(WorldGrid world) {
        ensureClaimedFood(world);
        claimedFoodTileCount = world.getWidth() * world.getHeight();
    }

    public void resetClaimsForTick() {
        if (claimedFood != null) {
            Arrays.fill(claimedFood, 0f);
        }
    }

    public void beginTick(int expectedAgents) {
        eatRequests.reset(expectedAgents);
        if (claimedFood != null) {
            Arrays.fill(claimedFood, 0f);
        }
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
        ensureClaimedFood(world);
        int bestX = agent.getX();
        int bestY = agent.getY();
        int width = world.getWidth();
        int height = world.getHeight();
        float[] food = world.getFoodField();
        float[] hazard = world.getHazardField();
        boolean[] water = world.getWaterMask();
        float localCrowding = computeLocalFoodCrowding(world, agent.getX(), agent.getY(), SimConfig.CROWDING_RADIUS);
        float foodWeightScale = Math.max(0f, 1f - localCrowding * SimConfig.CROWDING_FOOD_WEIGHT);
        int currentIdx = MathUtil.index(agent.getX(), agent.getY(), width);
        float currentFood = Math.max(0f, food[currentIdx] - claimedFood[currentIdx]);
        float currentScore = currentFood * SimConfig.MOVE_FOOD_WEIGHT * foodWeightScale
                - hazard[currentIdx] * SimConfig.MOVE_HAZARD_WEIGHT;
        float bestNeighborScore = Float.NEGATIVE_INFINITY;
        float bestTieBreaker = -1f;
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
                float avail = Math.max(0f, food[idx] - claimedFood[idx]);
                float score = avail * SimConfig.MOVE_FOOD_WEIGHT * foodWeightScale
                        - hazard[idx] * SimConfig.MOVE_HAZARD_WEIGHT;
                float tieBreaker = random.nextFloat() * 0.0001f;
                if (score > bestNeighborScore
                        || (Math.abs(score - bestNeighborScore) < 1e-6f && tieBreaker > bestTieBreaker)) {
                    bestNeighborScore = score;
                    bestTieBreaker = tieBreaker;
                    bestX = nx;
                    bestY = ny;
                }
            }
        }
        if (bestNeighborScore <= currentScore) {
            if (agent.getHunger() <= SimConfig.MOVE_WANDER_HUNGER_THRESHOLD) {
                return wanderMove(agent, world);
            }
            return idle();
        }
        agent.moveTo(bestX, bestY);
        return new OutcomeVector(-SimConfig.MOVE_ENERGY_COST, -SimConfig.MOVE_HUNGER_COST, 0f);
    }

    public float computeLocalFoodCrowding(WorldGrid world, int ax, int ay, int radius) {
        ensureClaimedFood(world);
        float[] food = world.getFoodField();
        int w = world.getWidth();
        int h = world.getHeight();
        int relevant = 0;
        int fullyClaimed = 0;
        for (int y = ay - radius; y <= ay + radius; y++) {
            if (y < 0 || y >= h) {
                continue;
            }
            for (int x = ax - radius; x <= ax + radius; x++) {
                if (x < 0 || x >= w) {
                    continue;
                }
                int idx = y * w + x;
                float f = food[idx];
                if (f <= SimConfig.FOOD_MIN_TO_EAT) {
                    continue;
                }
                relevant++;
                float unclaimed = Math.max(0f, f - claimedFood[idx]);
                if (unclaimed <= SimConfig.FOOD_MIN_TO_EAT) {
                    fullyClaimed++;
                }
            }
        }
        if (relevant == 0) {
            return 0f;
        }
        return fullyClaimed / (float) relevant;
    }

    private OutcomeVector wanderMove(AgentState agent, WorldGrid world) {
        return exploratoryMove(agent, world);
    }

    // Intent: when agent is hungry but no neighbor is strictly better, MOVE should still wander rather than degenerating into IDLE.
    private OutcomeVector exploratoryMove(AgentState agent, WorldGrid world) {
        int width = world.getWidth();
        int height = world.getHeight();
        boolean[] water = world.getWaterMask();
        int chosenX = agent.getX();
        int chosenY = agent.getY();
        int candidateCount = 0;
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
                candidateCount++;
                if (random.nextInt(candidateCount) == 0) {
                    chosenX = nx;
                    chosenY = ny;
                }
            }
        }
        if (candidateCount == 0) {
            return idle();
        }
        agent.moveTo(chosenX, chosenY);
        return new OutcomeVector(-SimConfig.MOVE_ENERGY_COST, -SimConfig.MOVE_HUNGER_COST, 0f);
    }

    private OutcomeVector directedMove(AgentState agent, WorldGrid world, int targetX, int targetY) {
        int dx = Integer.compare(targetX, agent.getX());
        int dy = Integer.compare(targetY, agent.getY());
        int nextX = MathUtil.clamp(agent.getX() + dx, 0, world.getWidth() - 1);
        int nextY = MathUtil.clamp(agent.getY() + dy, 0, world.getHeight() - 1);
        int idx = MathUtil.index(nextX, nextY, world.getWidth());
        boolean backtrack = nextX == agent.getLastX() && nextY == agent.getLastY();
        if (world.getWaterMask()[idx] || backtrack) {
            int altX1 = MathUtil.clamp(agent.getX() + dx, 0, world.getWidth() - 1);
            int altY1 = agent.getY();
            int altX2 = agent.getX();
            int altY2 = MathUtil.clamp(agent.getY() + dy, 0, world.getHeight() - 1);
            boolean alt1Valid = dx != 0 && !world.getWaterMask()[MathUtil.index(altX1, altY1, world.getWidth())]
                    && !(altX1 == agent.getLastX() && altY1 == agent.getLastY());
            boolean alt2Valid = dy != 0 && !world.getWaterMask()[MathUtil.index(altX2, altY2, world.getWidth())]
                    && !(altX2 == agent.getLastX() && altY2 == agent.getLastY());
            if (alt1Valid) {
                nextX = altX1;
                nextY = altY1;
            } else if (alt2Valid) {
                nextX = altX2;
                nextY = altY2;
            } else if (world.getWaterMask()[idx]) {
                nextX = agent.getX();
                nextY = agent.getY();
            }
        }
        agent.snapshotLastPos();
        agent.moveTo(nextX, nextY);
        return new OutcomeVector(-SimConfig.MOVE_ENERGY_COST, -SimConfig.MOVE_HUNGER_COST, 0f);
    }

    private OutcomeVector eat(AgentState agent, WorldGrid world, int agentSlot, long tickIndex) {
        ensureClaimedFood(world);
        int idx = MathUtil.index(agent.getX(), agent.getY(), world.getWidth());
        float[] food = world.getFoodField();
        float remaining = Math.max(0f, food[idx] - claimedFood[idx]);
        if (remaining < SimConfig.FOOD_MIN_TO_EAT) {
            return OutcomeVector.zero();
        }
        float desired = SimConfig.FOOD_CONSUME_RATE;
        float claim = Math.min(desired, remaining);
        claimedFood[idx] += claim;
        eatRequests.add(idx, agent.getId().value(), agentSlot, claim, tickIndex, agent.getEnergy());
        return OutcomeVector.zero();
    }

    private OutcomeVector broadcast(AgentState agent, WorldGrid world, long tickIndex) {
        int idx = MathUtil.index(agent.getX(), agent.getY(), world.getWidth());
        float localFood = world.getFoodField()[idx];
        FoodEmitter nearbyEmitter = world.findNearestEmitterWithin(agent.getX(), agent.getY(),
                SimConfig.SIGNAL_EMITTER_DETECT_RADIUS);
        boolean ateRecently = agent.getLastSuccessfulEatTick() >= 0
                && (tickIndex - agent.getLastSuccessfulEatTick()) <= SimConfig.SIGNAL_BROADCAST_AFTER_EAT_WINDOW;
        boolean cooldownReady = agent.getLastBroadcastTick() < 0
                || (tickIndex - agent.getLastBroadcastTick()) >= SimConfig.SIGNAL_BROADCAST_COOLDOWN_TICKS;
        boolean signalCreated = false;
        if ((ateRecently || nearbyEmitter != null) && cooldownReady && nearbyEmitter != null) {
            if (localFood >= SimConfig.FOOD_MIN_TO_EAT || nearbyEmitter != null) {
                if (!nearbyEmitter.hasLeader()) {
                    nearbyEmitter.setLeaderAgentId(agent.getId().value());
                }
                long remaining = nearbyEmitter.getExpiresAtTick() - tickIndex;
                int ttl = (int) Math.max(1, Math.min(Integer.MAX_VALUE, remaining));
                int strengthBucket = BinningUtil.bin01(localFood, SimConfig.SIGNAL_STRENGTH_BINS);
                Signal signal = world.getSignalField().addSignal(agent.getX(), agent.getY(), strengthBucket, SimConfig.SIGNAL_BASE_CONFIDENCE,
                        ttl, 0, agent.getId().value(), nearbyEmitter.getLeaderAgentId(), agent.getSocialCredit(), nearbyEmitter.getId(), tickIndex);
                if (signal != null) {
                    agent.setLastBroadcastTick(tickIndex);
                    if (metrics != null) {
                        metrics.incrementSignalsEmitted();
                    }
                    signalCreated = true;
                }
            }
        }
        if (signalCreated) {
            return new OutcomeVector(-SimConfig.SIGNAL_BROADCAST_COST_ENERGY, -SimConfig.SIGNAL_BROADCAST_COST_HUNGER, 0f);
        }
        return OutcomeVector.zero();
    }

    private OutcomeVector followSignalMove(AgentState agent, WorldGrid world, long tickIndex) {
        if (agent.hasFollowLock(tickIndex)) {
            OutcomeVector delta = directedMove(agent, world, agent.getFollowLockTargetX(), agent.getFollowLockTargetY());
            if (agent.getX() == agent.getFollowLockTargetX() && agent.getY() == agent.getFollowLockTargetY()) {
                agent.clearFollowLock();
            }
            return delta;
        }
        SignalField field = world.getSignalField();
        if (field.isEmpty()) {
            return exploratoryMove(agent, world);
        }
        int baseRadius = SimConfig.SIGNAL_SENSE_RADIUS_BASE;
        int dynamic = (int) (agent.getPredictionError() * (SimConfig.SIGNAL_SENSE_RADIUS_MAX - baseRadius));
        int radius = Math.min(SimConfig.SIGNAL_SENSE_RADIUS_MAX, baseRadius + Math.max(0, dynamic));
        Signal best = field.bestSignalWithinRadius(agent.getX(), agent.getY(), radius, agent.getId().value());
        if (best == null) {
            return exploratoryMove(agent, world);
        }
        agent.setFollowLock(best.getX(), best.getY(), tickIndex + SimConfig.PATTERN_FOLLOW_LOCK_TICKS);
        agent.setFollowMemory(best.getId(), -1, best.getLeaderAgentId(), tickIndex);
        if (metrics != null) {
            metrics.incrementFollowMoves();
        }
        OutcomeVector delta = directedMove(agent, world, best.getX(), best.getY());
        if (agent.getX() == best.getX() && agent.getY() == best.getY()) {
            agent.clearFollowLock();
        }
        return delta;
    }

    public OutcomeVector executeFoodLockMoveIfActive(AgentState agent, WorldGrid world, long tickIndex) {
        if (!agent.hasFoodLock(tickIndex)) {
            return null;
        }
        int targetX = agent.getFoodLockTargetX();
        int targetY = agent.getFoodLockTargetY();
        int targetIdx = MathUtil.index(targetX, targetY, world.getWidth());
        if (world.getFoodField()[targetIdx] < SimConfig.FOOD_MIN_TO_EAT
                || (agent.getX() == targetX && agent.getY() == targetY)) {
            agent.clearFoodLock();
            return null;
        }
        return directedMove(agent, world, targetX, targetY);
    }

    public OutcomeVector executeFollowLockMove(AgentState agent, WorldGrid world, long tickIndex) {
        if (!agent.hasFollowLock(tickIndex)) {
            return null;
        }
        float foodHere = world.getFoodAt(agent.getX(), agent.getY());
        if (foodHere >= SimConfig.FOOD_MIN_TO_EAT) {
            agent.clearFollowLock();
            return null;
        }
        OutcomeVector delta = directedMove(agent, world, agent.getFollowLockTargetX(), agent.getFollowLockTargetY());
        if (agent.getX() == agent.getFollowLockTargetX() && agent.getY() == agent.getFollowLockTargetY()) {
            agent.clearFollowLock();
        }
        return delta;
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
            float energyAtRequest = eatRequests.getEnergyAtRequest(requestIndex);
            float hungerGain = (energyAtRequest < 0.999f) ? (consumed * SimConfig.FOOD_TO_HUNGER_GAIN) : 0f;
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
        private float[] agentEnergyAtRequest = new float[0];
        private int size;

        void reset(int capacityHint) {
            ensureCapacity(capacityHint);
            size = 0;
        }

        void add(int tileIndex, long agentId, int agentSlot, float desiredAmount, long tick, float energyAtRequest) {
            ensureCapacity(size + 1);
            this.tileIndex[size] = tileIndex;
            this.agentId[size] = agentId;
            this.agentSlot[size] = agentSlot;
            this.desiredAmount[size] = desiredAmount;
            this.tick[size] = tick;
            this.agentEnergyAtRequest[size] = energyAtRequest;
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

        float getEnergyAtRequest(int index) {
            return agentEnergyAtRequest[index];
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
            agentEnergyAtRequest = resize(agentEnergyAtRequest, newSize);
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
