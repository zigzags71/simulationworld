package simcore.util;

import simcore.config.SimConfig;
import simcore.sim.commands.BrushType;
import simcore.sim.commands.PlaceFieldBrushCommand;

import java.util.Random;

public final class FieldBrushApplier {
    private FieldBrushApplier() {
    }

    public static boolean apply(PlaceFieldBrushCommand command, float[] food, float[] hazard, int width, int height,
                                float foodBaseline, float hazardBaseline) {
        int radius = command.getRadius();
        int cx = MathUtil.clamp(command.getCenterX(), 0, width - 1);
        int cy = MathUtil.clamp(command.getCenterY(), 0, height - 1);
        Random random = new Random(command.getSeed());
        boolean changed = false;
        int rSquared = radius * radius;
        int minX = Math.max(0, cx - radius);
        int maxX = Math.min(width - 1, cx + radius);
        int minY = Math.max(0, cy - radius);
        int maxY = Math.min(height - 1, cy + radius);
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                int dx = x - cx;
                int dy = y - cy;
                int distSq = dx * dx + dy * dy;
                if (distSq > rSquared) {
                    continue;
                }
                float falloff = 1f - (float) Math.sqrt(distSq) / radius;
                float jitter = 0.85f + random.nextFloat() * 0.3f;
                float intensity = falloff * falloff * jitter * SimConfig.BRUSH_MAX_DELTA;
                int idx = MathUtil.index(x, y, width);
                if (command.getType() == BrushType.FOOD) {
                    float next = MathUtil.clamp01(food[idx] + intensity);
                    changed |= next != food[idx];
                    food[idx] = next;
                } else if (command.getType() == BrushType.HAZARD) {
                    float next = MathUtil.clamp01(hazard[idx] + intensity);
                    changed |= next != hazard[idx];
                    hazard[idx] = next;
                } else {
                    float blend = falloff * SimConfig.BRUSH_ERASE_BLEND;
                    float nextFood = MathUtil.lerp(food[idx], foodBaseline, blend);
                    float nextHazard = MathUtil.lerp(hazard[idx], hazardBaseline, blend);
                    changed |= nextFood != food[idx] || nextHazard != hazard[idx];
                    food[idx] = nextFood;
                    hazard[idx] = nextHazard;
                }
            }
        }
        return changed;
    }
}
