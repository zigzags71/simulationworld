package simcore.agents;

import simcore.config.SimConfig;
import simcore.events.AgentDiedEvent;
import simcore.events.EventBus;
import simcore.events.RuleExecutedEvent;
import simcore.events.SimulationEvent;
import simcore.rules.ActionType;
import simcore.rules.ContextKey;
import simcore.rules.OutcomeVector;
import simcore.rules.Rule;
import simcore.rules.RuleSelector;
import simcore.rules.RuleType;
import simcore.util.BinningUtil;
import simcore.util.MathUtil;
import simcore.world.WorldGrid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class AgentSystem {
    private final List<AgentState> agents;
    private final ActionExecutor actionExecutor;
    private final Random random;
    private final EventBus<SimulationEvent> eventBus;
    private OutcomeVector[] baseDeltas = new OutcomeVector[0];
    private OutcomeVector[] actionDeltas = new OutcomeVector[0];
    private OutcomeVector[] beforeStates = new OutcomeVector[0];
    private Rule[] chosenRules = new Rule[0];
    private AgentState[] agentBuffer = new AgentState[0];
    private float[] hazardBuffer = new float[0];
    private boolean[] skipBuffer = new boolean[0];
    private long nextId;
    private int totalDeaths;

    public AgentSystem(WorldGrid world, long seed) {
        this(world, seed, SimConfig.NUM_AGENTS, null);
    }

    public AgentSystem(WorldGrid world, long seed, int initialPopulation, EventBus<SimulationEvent> eventBus) {
        this.random = new Random(seed);
        this.agents = new ArrayList<>(Math.max(initialPopulation, 16));
        this.actionExecutor = new ActionExecutor(new Random(seed + 13));
        this.eventBus = eventBus;
        this.nextId = 0;
        spawnInitialAgents(world, initialPopulation);
    }

    public AgentTickMetrics tick(WorldGrid world, long tickIndex) {
        AgentTickMetrics metrics = new AgentTickMetrics();
        int width = world.getWidth();
        int[] crowding = computeCrowding(width, world.getHeight());
        int agentCount = agents.size();
        ensureBuffers(agentCount);
        actionExecutor.beginTick(agentCount);
        for (int i = 0; i < agentCount; i++) {
            AgentState agent = agents.get(i);
            agentBuffer[i] = agent;
            chosenRules[i] = null;
            skipBuffer[i] = false;
            if (agent == null) {
                skipBuffer[i] = true;
                continue;
            }
            ContextKey contextKey = buildContext(agent, world, crowding);
            Rule rule = RuleSelector.choose(RuleSelector.applicable(agent.getRulebook(), contextKey), agent, random);
            if (rule == null) {
                skipBuffer[i] = true;
                continue;
            }
            chosenRules[i] = rule;
            beforeStates[i] = OutcomeVector.fromAgent(agent.getEnergy(), agent.getHunger(), agent.getStress());
            int idx = MathUtil.index(agent.getX(), agent.getY(), width);
            float hazardHere = world.getHazardField()[idx];
            hazardBuffer[i] = hazardHere;
            baseDeltas[i] = new OutcomeVector(
                    -SimConfig.ENERGY_DRAIN_PER_TICK - hazardHere * SimConfig.HAZARD_ENERGY_DRAIN_PER_TICK,
                    -SimConfig.HUNGER_DRAIN_PER_TICK,
                    hazardHere * SimConfig.HAZARD_STRESS_GAIN_PER_TICK - SimConfig.STRESS_RECOVERY_PER_TICK);
            actionDeltas[i] = actionExecutor.execute(rule.getAction(), agent, world, i, tickIndex);
        }
        actionExecutor.resolveEatRequests(world, actionDeltas, eventBus);
        for (int i = agentCount - 1; i >= 0; i--) {
            if (skipBuffer[i]) {
                continue;
            }
            AgentState agent = agentBuffer[i];
            Rule rule = chosenRules[i];
            if (agent == null || rule == null) {
                continue;
            }
            OutcomeVector before = beforeStates[i];
            OutcomeVector actionDelta = actionDeltas[i] != null ? actionDeltas[i] : OutcomeVector.zero();
            OutcomeVector totalDelta = baseDeltas[i].add(actionDelta);
            agent.applyTick(totalDelta.getDeltaEnergy(), totalDelta.getDeltaHunger(), totalDelta.getDeltaStress());
            OutcomeVector after = OutcomeVector.fromAgent(agent.getEnergy(), agent.getHunger(), agent.getStress());
            OutcomeVector observed = after.deltaFrom(before);
            float error = observed.distanceTo(rule.getExpected(), false);
            applyTrustUpdate(rule, error, tickIndex);
            agent.updatePredictionError(error);
            if (eventBus != null && SimConfig.LOG_SELECTED_AGENT_ENABLED) {
                eventBus.publish(new RuleExecutedEvent(agent.getId().value(), rule.getRuleId(), rule.getAction(), rule.getTrust(),
                        error, tickIndex));
            }
            if (agent.isDead()) {
                agents.remove(i);
                totalDeaths++;
                metrics.markDeath();
                if (eventBus != null && SimConfig.LOG_EVENTS_ENABLED) {
                    eventBus.publish(new AgentDiedEvent(agent.getId().value(), tickIndex, agent.getX(), agent.getY()));
                }
                continue;
            }
            metrics.accumulate(agent, hazardBuffer[i]);
        }
        metrics.setTotalDeaths(totalDeaths);
        return metrics;
    }

    private void ensureBuffers(int count) {
        if (baseDeltas.length < count) {
            baseDeltas = new OutcomeVector[count];
            actionDeltas = new OutcomeVector[count];
            beforeStates = new OutcomeVector[count];
            chosenRules = new Rule[count];
            agentBuffer = new AgentState[count];
            hazardBuffer = new float[count];
            skipBuffer = new boolean[count];
        }
    }

    private void spawnInitialAgents(WorldGrid world, int initialPopulation) {
        int w = world.getWidth();
        int h = world.getHeight();
        boolean[] water = world.getWaterMask();
        int spawned = 0;
        while (spawned < initialPopulation) {
            int x = random.nextInt(w);
            int y = random.nextInt(h);
            if (!water[MathUtil.index(x, y, w)]) {
                AgentState agent = new AgentState(new AgentId(nextId++), x, y, SimConfig.INITIAL_ENERGY, spawned);
                seedRules(agent, world, new int[w * h]);
                agents.add(agent);
                spawned++;
            }
        }
    }

    public int spawnAgents(WorldGrid world, int centerX, int centerY, int radius, int count, Random rand) {
        if (count <= 0 || radius <= 0) {
            return 0;
        }
        int width = world.getWidth();
        int height = world.getHeight();
        boolean[] water = world.getWaterMask();
        int minX = Math.max(0, centerX - radius);
        int maxX = Math.min(width - 1, centerX + radius);
        int minY = Math.max(0, centerY - radius);
        int maxY = Math.min(height - 1, centerY + radius);
        int radiusSq = radius * radius;
        List<Integer> candidates = new ArrayList<>();
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                int dx = x - centerX;
                int dy = y - centerY;
                if (dx * dx + dy * dy <= radiusSq) {
                    int idx = MathUtil.index(x, y, width);
                    if (!water[idx]) {
                        candidates.add(idx);
                    }
                }
            }
        }
        if (candidates.isEmpty()) {
            return 0;
        }
        Collections.shuffle(candidates, rand);
        int spawned = 0;
        int cultureBase = rand.nextInt(Integer.MAX_VALUE);
        for (int i = 0; i < candidates.size() && spawned < count; i++) {
            int idx = candidates.get(i);
            int x = idx % width;
            int y = idx / width;
            AgentState agent = new AgentState(new AgentId(nextId++), x, y, SimConfig.INITIAL_ENERGY, cultureBase + spawned);
            seedRules(agent, world, new int[width * height]);
            agents.add(agent);
            spawned++;
        }
        return spawned;
    }

    public List<AgentState> getAgents() {
        return Collections.unmodifiableList(agents);
    }

    public AgentState findAgentById(long id) {
        for (AgentState agent : agents) {
            if (agent.getId().value() == id) {
                return agent;
            }
        }
        return null;
    }

    private int[] computeCrowding(int width, int height) {
        int[] counts = new int[width * height];
        for (AgentState agent : agents) {
            counts[MathUtil.index(agent.getX(), agent.getY(), width)] += 1;
        }
        return counts;
    }

    private ContextKey buildContext(AgentState agent, WorldGrid world, int[] crowding) {
        int width = world.getWidth();
        int idx = MathUtil.index(agent.getX(), agent.getY(), width);
        int bins = SimConfig.FIELD_BIN_COUNT;
        int hungerBin = BinningUtil.bin01(agent.getHunger(), bins);
        int energyBin = BinningUtil.bin01(agent.getEnergy(), bins);
        int stressBin = BinningUtil.bin01(agent.getStress(), bins);
        int foodBin = BinningUtil.bin01(world.getFoodField()[idx], bins);
        int hazardBin = BinningUtil.bin01(world.getHazardField()[idx], bins);
        float crowdNorm = Math.min(1f, crowding[idx] / SimConfig.CROWDING_MAX_EXPECTED);
        int crowdBin = BinningUtil.bin01(crowdNorm, bins);
        int awareness = 0;
        int affordance = world.getFoodField()[idx] >= SimConfig.FOOD_MIN_TO_EAT ? 1 : 0;
        return new ContextKey(hungerBin, energyBin, stressBin, foodBin, hazardBin, crowdBin, awareness, affordance);
    }

    private void seedRules(AgentState agent, WorldGrid world, int[] crowding) {
        ContextKey contextKey = buildContext(agent, world, crowding);
        OutcomeVector idleExpected = new OutcomeVector(-SimConfig.ENERGY_DRAIN_PER_TICK,
                -SimConfig.HUNGER_DRAIN_PER_TICK, -SimConfig.STRESS_RECOVERY_PER_TICK - SimConfig.IDLE_STRESS_RECOVERY_BONUS);
        OutcomeVector moveExpected = new OutcomeVector(-SimConfig.ENERGY_DRAIN_PER_TICK - SimConfig.MOVE_ENERGY_COST,
                -SimConfig.HUNGER_DRAIN_PER_TICK - SimConfig.MOVE_HUNGER_COST, -SimConfig.STRESS_RECOVERY_PER_TICK);
        OutcomeVector eatExpected = new OutcomeVector(SimConfig.FOOD_TO_ENERGY_GAIN * SimConfig.FOOD_CONSUME_RATE,
                SimConfig.FOOD_TO_HUNGER_GAIN * SimConfig.FOOD_CONSUME_RATE, -SimConfig.STRESS_RECOVERY_PER_TICK * 0.5f);
        agent.addRule(new Rule(agent.allocateRuleId(), RuleType.NORMAL, contextKey, ActionType.IDLE, idleExpected, 0.8f));
        agent.addRule(new Rule(agent.allocateRuleId(), RuleType.NORMAL, contextKey, ActionType.MOVE, moveExpected, 0.5f));
        agent.addRule(new Rule(agent.allocateRuleId(), RuleType.NORMAL, contextKey, ActionType.EAT, eatExpected, 0.5f));
    }

    void applyTrustUpdate(Rule rule, float error, long tickIndex) {
        rule.incrementUses();
        rule.setLastUsedTick(tickIndex);
        rule.setLastError(error);
        if (error < SimConfig.ERROR_SUCCESS_THRESHOLD) {
            rule.incrementSuccesses();
            float improved = rule.getTrust() + SimConfig.TRUST_LEARN_UP * (1f - rule.getTrust());
            rule.setTrust(MathUtil.clamp01(improved));
        } else {
            float degraded = rule.getTrust() - SimConfig.TRUST_LEARN_DOWN * error * rule.getTrust();
            rule.setTrust(MathUtil.clamp01(degraded));
        }
    }
}
