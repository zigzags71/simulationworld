package simcore.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ColorUtilTest {
    @Test
    void lerpInterpolates() {
        int from = ColorUtil.toARGB(255, 0, 0, 0);
        int to = ColorUtil.toARGB(255, 255, 255, 255);
        int mid = ColorUtil.lerpColor(from, to, 0.5f);
        assertEquals(ColorUtil.toARGB(255, 128, 128, 128), mid);
    }

    @Test
    void colorFromSeedIsDeterministic() {
        int first = ColorUtil.colorFromSeed(42L);
        int second = ColorUtil.colorFromSeed(42L);
        int different = ColorUtil.colorFromSeed(43L);
        assertEquals(first, second);
        assertNotEquals(first, different);
    }
}
