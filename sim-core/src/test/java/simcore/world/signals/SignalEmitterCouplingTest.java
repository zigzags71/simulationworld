package simcore.world.signals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import simcore.agents.ActionExecutor;
import simcore.agents.AgentId;
import simcore.agents.AgentState;
import simcore.agents.AgentSystem;
import simcore.config.MapGenConfig;
import simcore.config.SimConfig;
import simcore.rules.ActionType;
import simcore.rules.OutcomeVector;
import simcore.world.WorldGrid;
import simcore.world.objects.FoodEmitter;
import simcore.world.signals.Signal;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignalEmitterCouplingTest {
    private boolean originalSpawnerToggle;

    @BeforeEach
    void disableDefaultSpawners() {
        originalSpawnerToggle = SimConfig.DEFAULT_SPAWNERS_ENABLED;
        SimConfig.DEFAULT_SPAWNERS_ENABLED = false;
    }

    @AfterEach
    void restoreSpawnerToggle() {
        SimConfig.DEFAULT_SPAWNERS_ENABLED = originalSpawnerToggle;
    }

    @Test
    void signalTtlMatchesEmitterExpiry() {
        WorldGrid world = WorldGrid.generate(MapGenConfig.defaults());
        FoodEmitter emitter = world.addEmitter(5, 5, 3, 0.05f, true);
        long tick = 100;
        emitter.setExpiresAtTick(tick + 50);
        world.getFoodField()[5 + 5 * world.getWidth()] = SimConfig.FOOD_MIN_TO_EAT;
        ActionExecutor executor = new ActionExecutor(new Random(1));
        AgentState agent = AgentState.forTest(new AgentId(1), 5, 5, 1f, 0.5f, 0f, 0f);

        executor.execute(ActionType.BROADCAST_SIGNAL, agent, world, 0, tick);

        Signal signal = world.getSignalField().getSignals().get(0);
        assertTrue(Math.abs(signal.getTtlTicks() - 50) <= 1, "Signal TTL should align with emitter expiry");
    }

    @Test
    void leaderAssignedOnce() {
        WorldGrid world = WorldGrid.generate(MapGenConfig.defaults());
        FoodEmitter emitter = world.addEmitter(10, 10, 4, 0.05f, true);
        emitter.setExpiresAtTick(200);
        world.getFoodField()[10 + 10 * world.getWidth()] = SimConfig.FOOD_MIN_TO_EAT;

        ActionExecutor executor = new ActionExecutor(new Random(2));
        AgentState leader = AgentState.forTest(new AgentId(1), 10, 10, 1f, 0.5f, 0f, 0f);
        executor.execute(ActionType.BROADCAST_SIGNAL, leader, world, 0, 5);

        AgentState follower = AgentState.forTest(new AgentId(2), 10, 10, 1f, 0.5f, 0f, 0f);
        executor.execute(ActionType.BROADCAST_SIGNAL, follower, world, 1, 10);

        assertEquals(leader.getId().value(), emitter.getLeaderAgentId());
    }

    @Test
    void rewardsGoToEmitterLeader() {
        WorldGrid world = WorldGrid.generate(MapGenConfig.defaults());
        AgentSystem system = new AgentSystem(world, 999L, 2, null);
        AgentState leader = system.getAgents().get(0);
        AgentState follower = system.getAgents().get(1);

        follower.setFollowMemory(1, -1, leader.getId().value(), 10);
        OutcomeVector actionDelta = new OutcomeVector(0.1f, 0.2f, 0f);

        system.rewardLeaderForFollow(follower, actionDelta, 12);

        assertTrue(leader.getSocialCredit() > SimConfig.INITIAL_SOCIAL_CREDIT);
    }
}
