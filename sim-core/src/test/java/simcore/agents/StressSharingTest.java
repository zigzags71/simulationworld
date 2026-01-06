package simcore.agents;

import org.junit.jupiter.api.Test;
import simcore.config.SimConfig;
import simcore.world.WorldGrid;

import java.lang.reflect.Field;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StressSharingTest {

    @Test
    void nearbyAgentsShareStress() {
        float[] food = new float[16];
        float[] hazard = new float[16];
        boolean[] water = new boolean[16];
        WorldGrid world = new WorldGrid(4, 4, 10L, food, hazard, water);
        AgentSystem system = new AgentSystem(world, 5L, 0, null);
        system.spawnAgents(world, 0, 0, 1, 2, new Random(2L));

        AgentState highStress = system.getAgents().get(0);
        AgentState lowStress = system.getAgents().get(1);
        highStress.moveTo(1, 1);
        lowStress.moveTo(2, 1);
        highStress.setStress(1.0f);
        lowStress.setStress(0.0f);

        system.tick(world, 0);

        assertTrue(lowStress.getStress() > 0f);
        assertTrue(lowStress.getStress() < 1f);
        assertTrue(highStress.getStress() > 0.6f);
    }

    @Test
    void distantAgentsDoNotShareStress() {
        float[] food = new float[64];
        float[] hazard = new float[64];
        boolean[] water = new boolean[64];
        WorldGrid world = new WorldGrid(8, 8, 11L, food, hazard, water);
        AgentSystem system = new AgentSystem(world, 5L, 0, null);
        system.spawnAgents(world, 0, 0, 1, 2, new Random(3L));

        AgentState highStress = system.getAgents().get(0);
        AgentState lowStress = system.getAgents().get(1);
        highStress.moveTo(0, 0);
        lowStress.moveTo(SimConfig.STRESS_NEIGHBOR_RADIUS + 5, 0);
        highStress.setStress(1.0f);
        lowStress.setStress(0.0f);

        system.tick(world, 0);

        assertEquals(0.0f, lowStress.getStress());
        assertTrue(highStress.getStress() > 0.6f);
    }

    @Test
    void contextMismatchStillAppliesMetabolism() throws Exception {
        float[] food = new float[4];
        float[] hazard = new float[4];
        boolean[] water = new boolean[4];
        WorldGrid world = new WorldGrid(2, 2, 15L, food, hazard, water);
        AgentSystem system = new AgentSystem(world, 6L, 1, null);

        AgentState agent = system.getAgents().get(0);
        agent.setStress(1.0f);

        Field rulebookField = AgentState.class.getDeclaredField("rulebook");
        rulebookField.setAccessible(true);
        ((java.util.List<?>) rulebookField.get(agent)).clear();

        system.tick(world, 0);

        assertTrue(agent.getStress() < 1.0f);
    }
}
