package simcore.world;

import org.junit.jupiter.api.Test;
import simcore.config.MapGenConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HazardGenerationTest {

    @Test
    void hazardZeroWhenIntensityZero() {
        WorldGrid world = WorldGrid.generate(MapGenConfig.defaults().withHazardBaseline(0f));
        float[] hazard = world.getHazardField();
        for (int i = 0; i < hazard.length; i++) {
            assertEquals(0f, hazard[i], 1e-6f, "hazard not zero at index " + i);
        }
    }
}
