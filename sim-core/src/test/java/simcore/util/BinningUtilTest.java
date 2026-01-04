package simcore.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BinningUtilTest {
    @Test
    void mapsValuesIntoBins() {
        assertEquals(0, BinningUtil.toBin(0f, 5));
        assertEquals(2, BinningUtil.toBin(0.5f, 5));
        assertEquals(4, BinningUtil.toBin(0.99f, 5));
    }

    @Test
    void clampsValues() {
        assertEquals(0, BinningUtil.toBin(-0.5f, 5));
        assertEquals(4, BinningUtil.toBin(5f, 5));
    }

    @Test
    void rejectsInvalidBinCounts() {
        assertThrows(IllegalArgumentException.class, () -> BinningUtil.toBin(0.1f, 0));
    }

    @Test
    void distributesUpperEdgeIntoLastBin() {
        assertEquals(4, BinningUtil.toBin(1f, 5));
    }
}
