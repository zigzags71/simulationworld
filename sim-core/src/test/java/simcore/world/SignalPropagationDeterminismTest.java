package simcore.world;

import org.junit.jupiter.api.Test;
import simcore.agents.AgentState;
import simcore.agents.AgentSystem;
import simcore.config.MapGenConfig;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SignalPropagationDeterminismTest {

    @Test
    void simulationRemainsDeterministicWithSignals() {
        DeterminismSnapshot first = runSimulation(2024L);
        DeterminismSnapshot second = runSimulation(2024L);

        assertEquals(first.population, second.population);
        assertEquals(first.totalDeaths, second.totalDeaths);
        assertEquals(first.foodSum, second.foodSum, 1e-6f);
        assertEquals(first.hungerSum, second.hungerSum, 1e-6f);
        assertEquals(first.positionChecksum, second.positionChecksum);
    }

    private DeterminismSnapshot runSimulation(long seed) {
        WorldGrid world = WorldGrid.generate(MapGenConfig.defaults().withSeed(seed));
        AgentSystem agents = new AgentSystem(world, seed + 99, 50, null);
        world.addEmitter(world.getWidth() / 2, world.getHeight() / 2, 8, 0.1f, true);

        for (int t = 0; t < 120; t++) {
            world.tickEmitters(t);
            world.getSignalField().tickDecay();
            agents.tick(world, t);
        }

        float foodSum = 0f;
        for (float f : world.getFoodField()) {
            foodSum += f;
        }
        float hungerSum = 0f;
        long positionChecksum = 0L;
        List<AgentState> states = agents.getAgents();
        for (AgentState state : states) {
            hungerSum += state.getHunger();
            positionChecksum += (state.getX() * 73856093L) ^ (state.getY() * 19349663L);
        }
        DeterminismSnapshot snap = new DeterminismSnapshot();
        snap.population = states.size();
        snap.totalDeaths = agents.getTotalDeaths();
        snap.foodSum = foodSum;
        snap.hungerSum = hungerSum;
        snap.positionChecksum = positionChecksum;
        return snap;
    }

    private static class DeterminismSnapshot {
        int population;
        int totalDeaths;
        float foodSum;
        float hungerSum;
        long positionChecksum;
    }
}
