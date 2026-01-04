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

    public static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
