package simcore.world;

import simcore.config.MapGenConfig;
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
        return generate(MapGenConfig.defaults().withSeed(seed));
    }

    public static WorldGrid generate(MapGenConfig config) {
        Random random = new Random(config.getSeed());
        int width = SimConfig.WORLD_W;
        int height = SimConfig.WORLD_H;
        float[] food = new float[width * height];
        float[] hazard = new float[food.length];
        boolean[] water = new boolean[food.length];

        float foodBase = 0.25f + config.getFoodRichness() * 0.75f;
        float hazardBase = 0.2f + config.getHazardBaseline() * 0.6f;
        float noiseScale = 0.35f + config.getPatchiness() * 0.65f;
        float waterThreshold = 0.05f + config.getWaterRatio() * 0.45f;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int idx = index(x, y, width);
                float fx = (float) x / width;
                float fy = (float) y / height;
                float foodNoise = layeredNoise(random, fx, fy, config.getPatchiness());
                float hazardNoise = layeredNoise(random, fy, fx, 1f - config.getPatchiness());
                food[idx] = MathUtil.clamp01(foodBase + foodNoise * noiseScale);
                hazard[idx] = MathUtil.clamp01(hazardBase + hazardNoise * noiseScale);
                water[idx] = (food[idx] + hazard[idx]) < waterThreshold;
            }
        }

        smooth(water, food, hazard, width, height, config.getWaterRatio());
        return new WorldGrid(width, height, food, hazard, water);
    }

    private static void smooth(boolean[] water, float[] food, float[] hazard, int w, int h, float waterRatio) {
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
        float erosion = 0.8f - waterRatio * 0.5f;
        float hazardLift = 0.1f + waterRatio * 0.3f;
        for (int i = 0; i < water.length; i++) {
            if (neighborWater[i] > 4) {
                water[i] = true;
            }
            food[i] = MathUtil.clamp01(food[i] * erosion + (water[i] ? 0.0f : 0.1f));
            hazard[i] = MathUtil.clamp01(hazard[i] * erosion + (water[i] ? hazardLift : 0.0f));
        }
    }

    private static float layeredNoise(Random baseRandom, float x, float y, float roughness) {
        long combined = Double.doubleToLongBits(x) ^ (Double.doubleToLongBits(y) << 1);
        Random rand = new Random(baseRandom.nextLong() ^ combined);
        float value = 0f;
        float amplitude = 1f;
        for (int i = 0; i < 4; i++) {
            value += (rand.nextFloat() - 0.5f) * amplitude * (0.5f + roughness);
            amplitude *= 0.5f;
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
