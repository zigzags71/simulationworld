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

        assertEquals(expectedDistance(base, neighbor), base.distanceTo(neighbor));
        assertEquals(expectedDistance(base, far), base.distanceTo(far));
    }

    private int expectedDistance(ContextKey a, ContextKey b) {
        int binDiff = Math.abs(a.getHungerBin() - b.getHungerBin())
                + Math.abs(a.getEnergyBin() - b.getEnergyBin())
                + Math.abs(a.getStressBin() - b.getStressBin())
                + Math.abs(a.getFoodBin() - b.getFoodBin())
                + Math.abs(a.getHazardBin() - b.getHazardBin())
                + Math.abs(a.getCrowdingBin() - b.getCrowdingBin());
        int awarenessDiff = a.getAwarenessFlag() == b.getAwarenessFlag() ? 0 : 1;
        int affordanceDiff = Integer.bitCount(a.getFoodAffordance() ^ b.getFoodAffordance());
        return binDiff + awarenessDiff + affordanceDiff;
    }
}
