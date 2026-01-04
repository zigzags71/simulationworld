package simcore.util;

import org.junit.jupiter.api.Test;
import simcore.sim.commands.BrushType;
import simcore.sim.commands.PlaceFieldBrushCommand;

import static org.junit.jupiter.api.Assertions.*;

class FieldBrushApplierTest {
    @Test
    void appliesDeterministicFoodBrush() {
        float[] foodA = new float[25];
        float[] hazardA = new float[25];
        float[] foodB = new float[25];
        float[] hazardB = new float[25];
        PlaceFieldBrushCommand command = new PlaceFieldBrushCommand(BrushType.FOOD, 2, 2, 2, 99L);

        FieldBrushApplier.apply(command, foodA, hazardA, 5, 5, 0.4f, 0.3f);
        FieldBrushApplier.apply(command, foodB, hazardB, 5, 5, 0.4f, 0.3f);

        assertArrayEquals(foodA, foodB, "Brush should be deterministic for a given seed");
        assertArrayEquals(hazardA, hazardB);
        assertTrue(foodA[MathUtil.index(2, 2, 5)] > 0.2f, "Center tile should increase");
    }

    @Test
    void eraserBlendsTowardBaseline() {
        float[] food = new float[9];
        float[] hazard = new float[9];
        for (int i = 0; i < food.length; i++) {
            food[i] = 0.9f;
            hazard[i] = 0.8f;
        }
        PlaceFieldBrushCommand command = new PlaceFieldBrushCommand(BrushType.ERASE, 1, 1, 1, 5L);
        boolean changed = FieldBrushApplier.apply(command, food, hazard, 3, 3, 0.4f, 0.3f);

        assertTrue(changed);
        assertTrue(food[MathUtil.index(1, 1, 3)] < 0.9f);
        assertTrue(hazard[MathUtil.index(1, 1, 3)] < 0.8f);
    }
}
