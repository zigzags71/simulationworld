package simcore.util;

import java.util.Random;

public final class ColorUtil {
    private ColorUtil() {
    }

    public static int toARGB(int a, int r, int g, int b) {
        int alpha = (a & 0xFF) << 24;
        int red = (r & 0xFF) << 16;
        int green = (g & 0xFF) << 8;
        int blue = (b & 0xFF);
        return alpha | red | green | blue;
    }

    public static int lerpColor(int fromARGB, int toARGB, float t) {
        float clamped = MathUtil.clamp01(t);
        int fa = (fromARGB >> 24) & 0xFF;
        int fr = (fromARGB >> 16) & 0xFF;
        int fg = (fromARGB >> 8) & 0xFF;
        int fb = fromARGB & 0xFF;

        int ta = (toARGB >> 24) & 0xFF;
        int tr = (toARGB >> 16) & 0xFF;
        int tg = (toARGB >> 8) & 0xFF;
        int tb = toARGB & 0xFF;

        int a = Math.round(fa + (ta - fa) * clamped);
        int r = Math.round(fr + (tr - fr) * clamped);
        int g = Math.round(fg + (tg - fg) * clamped);
        int b = Math.round(fb + (tb - fb) * clamped);
        return toARGB(a, r, g, b);
    }

    public static int colorFromSeed(long seed) {
        Random random = new Random(seed);
        int r = 80 + random.nextInt(150);
        int g = 80 + random.nextInt(150);
        int b = 80 + random.nextInt(150);
        return toARGB(255, r, g, b);
    }
}
