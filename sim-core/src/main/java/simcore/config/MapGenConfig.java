package simcore.config;

/**
 * Immutable configuration for world generation. Values are normalized in the range [0, 1].
 */
public final class MapGenConfig {
    private final long seed;
    private final float foodRichness;
    private final float hazardBaseline;
    private final float patchiness;
    private final float waterRatio;

    private MapGenConfig(long seed, float foodRichness, float hazardBaseline, float patchiness, float waterRatio) {
        this.seed = seed;
        this.foodRichness = clamp(foodRichness);
        this.hazardBaseline = clamp(hazardBaseline);
        this.patchiness = clamp(patchiness);
        this.waterRatio = clamp(waterRatio);
    }

    public static MapGenConfig defaults() {
        return new MapGenConfig(SimConfig.DEFAULT_SEED, SimConfig.DEFAULT_FOOD_RICHNESS,
                SimConfig.DEFAULT_HAZARD_BASELINE, SimConfig.DEFAULT_PATCHINESS, SimConfig.DEFAULT_WATER_RATIO);
    }

    public MapGenConfig withSeed(long newSeed) {
        return new MapGenConfig(newSeed, foodRichness, hazardBaseline, patchiness, waterRatio);
    }

    public MapGenConfig withFoodRichness(float newFoodRichness) {
        return new MapGenConfig(seed, newFoodRichness, hazardBaseline, patchiness, waterRatio);
    }

    public MapGenConfig withHazardBaseline(float newHazardBaseline) {
        return new MapGenConfig(seed, foodRichness, newHazardBaseline, patchiness, waterRatio);
    }

    public MapGenConfig withPatchiness(float newPatchiness) {
        return new MapGenConfig(seed, foodRichness, hazardBaseline, newPatchiness, waterRatio);
    }

    public MapGenConfig withWaterRatio(float newWaterRatio) {
        return new MapGenConfig(seed, foodRichness, hazardBaseline, patchiness, newWaterRatio);
    }

    public long getSeed() {
        return seed;
    }

    public float getFoodRichness() {
        return foodRichness;
    }

    public float getHazardBaseline() {
        return hazardBaseline;
    }

    public float getPatchiness() {
        return patchiness;
    }

    public float getWaterRatio() {
        return waterRatio;
    }

    private float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
