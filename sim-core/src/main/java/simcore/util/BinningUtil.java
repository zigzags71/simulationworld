package simcore.util;

public final class BinningUtil {
    private BinningUtil() {
    }

    public static int toBin(float value, int binCount) {
        if (binCount <= 0) {
            throw new IllegalArgumentException("binCount must be positive");
        }
        float clamped = MathUtil.clamp01(value);
        int bin = (int) (clamped * binCount);
        return Math.min(binCount - 1, bin);
    }

    public static int bin01(float value, int bins) {
        return toBin(value, bins);
    }
}
