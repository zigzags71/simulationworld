package simcore.rules;

import org.junit.jupiter.api.Test;
import simcore.util.BinningUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ContextKeyTest {

    @Test
    void equalityAndHashFollowBins() {
        int bin = BinningUtil.bin01(0.4f, 5);
        ContextKey keyA = new ContextKey(bin, bin, bin, bin, bin, bin, 0, 1);
        ContextKey keyB = new ContextKey(bin, bin, bin, bin, bin, bin, 0, 1);
        ContextKey keyC = new ContextKey(bin, bin, bin, bin, bin, bin, 1, 1);

        assertEquals(keyA, keyB);
        assertEquals(keyA.hashCode(), keyB.hashCode());
        assertNotEquals(keyA, keyC);
    }

    @Test
    void distanceReflectsBinDifferences() {
        ContextKey base = new ContextKey(0, 0, 0, 0, 0, 0, 0, 0);
        ContextKey neighbor = new ContextKey(1, 0, 0, 0, 0, 0, 0, 0);
        ContextKey far = new ContextKey(4, 4, 4, 4, 4, 4, 1, 1);

        assertEquals(1, base.distanceTo(neighbor));
        assertEquals(23, base.distanceTo(far));
    }
}
