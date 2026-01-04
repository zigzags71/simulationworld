package simcore.sim.commands;

import org.junit.jupiter.api.Test;
import simcore.agents.AgentState;
import simcore.agents.AgentSystem;
import simcore.util.MathUtil;
import simcore.world.WorldGrid;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpawnAgentsCommandTest {

    @Test
    void spawnsWithinRadius() {
        int width = 10;
        int height = 10;
        WorldGrid world = flatWorld(width, height, new boolean[width * height]);
        AgentSystem system = new AgentSystem(world, 99L, 0, null);
        int spawned = system.spawnAgents(world, 5, 5, 2, 10, new Random(1234));
        assertEquals(10, spawned);
        for (AgentState agent : system.getAgents()) {
            int dx = agent.getX() - 5;
            int dy = agent.getY() - 5;
            assertTrue(dx * dx + dy * dy <= 4);
        }
    }

    @Test
    void avoidsWaterTiles() {
        int width = 5;
        int height = 5;
        boolean[] water = new boolean[width * height];
        water[MathUtil.index(2, 2, width)] = true;
        WorldGrid world = flatWorld(width, height, water);
        AgentSystem system = new AgentSystem(world, 55L, 0, null);
        system.spawnAgents(world, 2, 2, 2, 8, new Random(11));
        for (AgentState agent : system.getAgents()) {
            assertTrue(!(agent.getX() == 2 && agent.getY() == 2));
        }
    }

    @Test
    void capsAtAvailableSpaces() {
        int width = 4;
        int height = 4;
        boolean[] water = new boolean[width * height];
        for (int i = 0; i < water.length; i++) {
            water[i] = true;
        }
        water[MathUtil.index(1, 1, width)] = false;
        water[MathUtil.index(2, 2, width)] = false;
        WorldGrid world = flatWorld(width, height, water);
        AgentSystem system = new AgentSystem(world, 77L, 0, null);
        int spawned = system.spawnAgents(world, 2, 2, 2, 5, new Random(33));
        assertEquals(2, spawned);
        assertEquals(2, system.getAgents().size());
    }

    private WorldGrid flatWorld(int width, int height, boolean[] waterMask) {
        float[] food = new float[width * height];
        float[] hazard = new float[width * height];
        return new WorldGrid(width, height, 1L, food, hazard, waterMask);
    }
}
