package simcore.world;

import simcore.config.MapGenConfig;
import simcore.config.SimConfig;
import simcore.util.MathUtil;
import simcore.world.objects.FoodEmitter;
import simcore.world.signals.SignalField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Immutable world definition storing terrain and environmental fields.
 */
public class WorldGrid {
    private final int width;
    private final int height;
    private final float[] foodStock;
    private final float[] hazardField;
    private final boolean[] waterMask;
    private final List<FoodEmitter> emitters;
    private long nextEmitterId = 1;
    private final SignalField signalField;

    private final long seed;

    public WorldGrid(int width, int height, long seed, float[] foodField, float[] hazardField, boolean[] waterMask) {
        this.width = width;
        this.height = height;
        this.seed = seed;
        this.foodStock = foodField;
        this.hazardField = hazardField;
        this.waterMask = waterMask;
        this.emitters = new ArrayList<>();
        this.signalField = new SignalField(width, height);
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
        clampFood(food);
        return new WorldGrid(width, height, config.getSeed(), food, hazard, water);
    }

    private static void clampFood(float[] food) {
        for (int i = 0; i < food.length; i++) {
            food[i] = Math.min(food[i], SimConfig.TILE_FOOD_MAX);
        }
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

    public float[] getFoodField() {
        return foodStock;
    }

    public float[] getFoodStock() {
        return foodStock;
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

    public long getSeed() {
        return seed;
    }

    public SignalField getSignalField() {
        return signalField;
    }

    public void regenerateFood(float amountPerTile) {
        if (amountPerTile <= 0f) {
            return;
        }
        for (int i = 0; i < foodStock.length; i++) {
            float next = foodStock[i] + amountPerTile;
            foodStock[i] = Math.min(next, SimConfig.TILE_FOOD_MAX);
        }
    }

    public List<FoodEmitter> getEmittersView() {
        return Collections.unmodifiableList(emitters);
    }

    public FoodEmitter addEmitter(int x, int y, int radius, float strength, boolean enabled) {
        FoodEmitter emitter = new FoodEmitter(nextEmitterId++, x, y, radius, strength, enabled);
        emitters.add(emitter);
        emitters.sort((a, b) -> Long.compare(a.getId(), b.getId()));
        return emitter;
    }

    public boolean removeEmitterAt(int x, int y) {
        FoodEmitter emitter = findEmitterAt(x, y);
        if (emitter == null) {
            return false;
        }
        return emitters.remove(emitter);
    }

    public FoodEmitter findEmitterAt(int x, int y) {
        FoodEmitter best = null;
        for (FoodEmitter emitter : emitters) {
            if (emitter.getX() == x && emitter.getY() == y) {
                if (best == null || emitter.getId() < best.getId()) {
                    best = emitter;
                }
            }
        }
        return best;
    }

    public void updateEmitter(long id, Integer radius, Float strength, Boolean enabled) {
        for (FoodEmitter emitter : emitters) {
            if (emitter.getId() == id) {
                if (radius != null) {
                    emitter.setRadius(radius);
                }
                if (strength != null) {
                    emitter.setStrengthPerTick(strength);
                }
                if (enabled != null) {
                    emitter.setEnabled(enabled);
                }
                return;
            }
        }
    }

    public void tickEmitters() {
        for (FoodEmitter emitter : emitters) {
            if (!emitter.isEnabled()) {
                continue;
            }
            depositEmitter(emitter);
        }
    }

    private void depositEmitter(FoodEmitter emitter) {
        int r = emitter.getRadius();
        int cx = emitter.getX();
        int cy = emitter.getY();
        float strength = emitter.getStrengthPerTick();
        if (r < 0 || strength <= 0f) {
            return;
        }
        int minX = Math.max(0, cx - r);
        int maxX = Math.min(width - 1, cx + r);
        int minY = Math.max(0, cy - r);
        int maxY = Math.min(height - 1, cy + r);
        int rSq = r * r;
        for (int dy = -r; dy <= r; dy++) {
            int y = cy + dy;
            if (y < 0 || y >= height) {
                continue;
            }
            for (int dx = -r; dx <= r; dx++) {
                int x = cx + dx;
                if (x < 0 || x >= width) {
                    continue;
                }
                if (dx * dx + dy * dy > rSq) {
                    continue;
                }
                int idx = index(x, y, width);
                if (waterMask[idx]) {
                    continue;
                }
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                float w = r == 0 ? 1f : Math.max(0f, 1f - (dist / r));
                float add = strength * w;
                float next = Math.min(SimConfig.TILE_FOOD_MAX, foodStock[idx] + add);
                foodStock[idx] = next;
            }
        }
    }
}
