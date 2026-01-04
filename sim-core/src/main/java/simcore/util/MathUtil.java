package simcore.util;

public final class MathUtil {
    private MathUtil() {
    }

    public static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    public static int index(int x, int y, int width) {
        return y * width + x;
    }
}
