package simcore.world;

import simcore.config.SimConfig;
import simcore.util.MathUtil;

import java.util.Random;

/**
 * Immutable world definition storing terrain and environmental fields.
 */
public class WorldGrid {
    private final int width;
    private final int height;
    private final float[] foodField;
    private final float[] hazardField;
    private final boolean[] waterMask;

    public WorldGrid(int width, int height, float[] foodField, float[] hazardField, boolean[] waterMask) {
        this.width = width;
        this.height = height;
        this.foodField = foodField;
        this.hazardField = hazardField;
        this.waterMask = waterMask;
    }

    public static WorldGrid generate(long seed) {
        Random random = new Random(seed);
        float[] food = new float[SimConfig.WORLD_W * SimConfig.WORLD_H];
        float[] hazard = new float[food.length];
        boolean[] water = new boolean[food.length];

        float foodBase = 0.4f + random.nextFloat() * 0.2f;
        float hazardBase = 0.3f + random.nextFloat() * 0.3f;

        for (int y = 0; y < SimConfig.WORLD_H; y++) {
            for (int x = 0; x < SimConfig.WORLD_W; x++) {
                int idx = index(x, y, SimConfig.WORLD_W);
                float fx = (float) x / SimConfig.WORLD_W;
                float fy = (float) y / SimConfig.WORLD_H;
                float foodNoise = layeredNoise(random, fx, fy);
                float hazardNoise = layeredNoise(random, fy, fx);
                food[idx] = MathUtil.clamp01(foodBase + foodNoise * 0.6f);
                hazard[idx] = MathUtil.clamp01(hazardBase + hazardNoise * 0.6f);
                water[idx] = (food[idx] < 0.25f && hazard[idx] < 0.25f);
            }
        }

        smooth(water, food, hazard, SimConfig.WORLD_W, SimConfig.WORLD_H);
        return new WorldGrid(SimConfig.WORLD_W, SimConfig.WORLD_H, food, hazard, water);
    }

    private static void smooth(boolean[] water, float[] food, float[] hazard, int w, int h) {
        int[] neighborWater = new int[water.length];
        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                int idx = index(x, y, w);
                int count = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dy == 0) {
                            continue;
                        }
                        if (water[index(x + dx, y + dy, w)]) {
                            count++;
                        }
                    }
                }
                neighborWater[idx] = count;
            }
        }
        for (int i = 0; i < water.length; i++) {
            if (neighborWater[i] > 4) {
                water[i] = true;
            }
            food[i] = MathUtil.clamp01(food[i] * 0.9f + (water[i] ? 0.0f : 0.1f));
            hazard[i] = MathUtil.clamp01(hazard[i] * 0.9f + (water[i] ? 0.2f : 0.0f));
        }
    }

    private static float layeredNoise(Random baseRandom, float x, float y) {
        long combined = Double.doubleToLongBits(x) ^ (Double.doubleToLongBits(y) << 1);
        Random rand = new Random(baseRandom.nextLong() ^ combined);
        float value = 0f;
        float amplitude = 1f;
        float frequency = 1f;
        for (int i = 0; i < 3; i++) {
            value += (rand.nextFloat() - 0.5f) * amplitude;
            amplitude *= 0.5f;
            frequency *= 2f;
        }
        return value;
    }

    private static int index(int x, int y, int w) {
        return y * w + x;
    }

    public TileFields getTile(int x, int y) {
        int idx = index(x, y, width);
        return new TileFields(foodField[idx], hazardField[idx], waterMask[idx]);
    }

    public float[] getFoodField() {
        return foodField;
    }

    public float[] getHazardField() {
        return hazardField;
    }

    public boolean[] getWaterMask() {
        return waterMask;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
