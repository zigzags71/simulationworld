package simcore.agents;

import org.junit.jupiter.api.Test;
import simcore.config.SimConfig;
import simcore.rules.ActionType;
import simcore.world.WorldGrid;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionExecutorMoveTest {

    // Intent: When hungry and the local neighborhood has no better tiles, MOVE should still move (wander) rather than acting like IDLE.
    @Test
    void hungryAgentMovesEvenWithoutBetterNeighbor() {
        float[] food = new float[9];
        float[] hazard = new float[9];
        boolean[] water = new boolean[9];
        WorldGrid world = new WorldGrid(3, 3, 7L, food, hazard, water);

        ActionExecutor executor = new ActionExecutor(new Random(11L));
        executor.prepareWorld(world);
        executor.beginTick(1);

        AgentState agent = AgentState.forTest(new AgentId(1), 1, 1, SimConfig.INITIAL_ENERGY, 0.0f, 0f, 0f);
        executor.execute(ActionType.MOVE, agent, world, 0, 0);

        assertTrue(agent.getX() != 1 || agent.getY() != 1, "agent should move when hungry even if no tile is better");
    }
}
