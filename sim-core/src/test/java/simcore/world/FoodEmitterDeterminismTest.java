package simcore.world;

import org.junit.jupiter.api.Test;
import simcore.config.MapGenConfig;
import simcore.config.SimConfig;
import simcore.world.objects.FoodEmitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FoodEmitterDeterminismTest {

    @Test
    void emitterDepositsDeterministically() {
        WorldGrid worldA = WorldGrid.generate(MapGenConfig.defaults().withSeed(4242L));
        WorldGrid worldB = WorldGrid.generate(MapGenConfig.defaults().withSeed(4242L));

        FoodEmitter emitterA = worldA.addEmitter(10, 10, 4, 0.05f, true);
        FoodEmitter emitterB = worldB.addEmitter(10, 10, 4, 0.05f, true);
        assertEquals(emitterA.getRadius(), emitterB.getRadius());

        for (int i = 0; i < 20; i++) {
            worldA.tickEmitters();
            worldB.tickEmitters();
        }

        float[] aFood = worldA.getFoodField();
        float[] bFood = worldB.getFoodField();
        assertEquals(aFood.length, bFood.length);
        for (int i = 0; i < aFood.length; i++) {
            assertEquals(bFood[i], aFood[i], 1e-6f, "food mismatch at index " + i);
            assertTrue(aFood[i] >= 0f, "negative food at index " + i);
            assertTrue(aFood[i] <= SimConfig.TILE_FOOD_MAX + 1e-6f, "food exceeds max at index " + i);
        }
    }
}
