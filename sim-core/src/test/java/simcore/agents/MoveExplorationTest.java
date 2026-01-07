package simcore.agents;

import org.junit.jupiter.api.Test;
import simcore.config.SimConfig;
import simcore.rules.ActionType;
import simcore.rules.OutcomeVector;
import simcore.world.WorldGrid;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoveExplorationTest {

    // Hungry agents should explore instead of idling when no nearby tile is better, preventing polite starvation.
    @Test
    void hungryAgentsExploreInBarrenNeighborhood() {
        float[] food = new float[9];
        float[] hazard = new float[9];
        boolean[] water = new boolean[9];
        WorldGrid world = new WorldGrid(3, 3, 42L, food, hazard, water);

        ActionExecutor executor = new ActionExecutor(new Random(5));
        executor.prepareWorld(world);
        executor.resetClaimsForTick();

        AgentState agent = AgentState.forTest(new AgentId(1), 1, 1, 1f, 0.2f, 0f, 0f);
        OutcomeVector delta = executor.execute(ActionType.MOVE, agent, world, 0, 0);

        assertEquals(-SimConfig.MOVE_ENERGY_COST, delta.getDeltaEnergy(), 1e-6f);
        assertEquals(-SimConfig.MOVE_HUNGER_COST, delta.getDeltaHunger(), 1e-6f);
        assertTrue(delta.getDeltaStress() <= 0f);
        assertTrue(agent.getX() != 1 || agent.getY() != 1, "agent should attempt to move when exploring");
    }
}
