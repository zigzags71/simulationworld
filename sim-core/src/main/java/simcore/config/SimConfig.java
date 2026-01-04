package simcore.config;

/**
 * Centralized immutable configuration for the simulation.
 */
public final class SimConfig {
    public static final int WORLD_W = 1024;
    public static final int WORLD_H = 1024;
    public static final int TICK_RATE = 20;
    public static final int RENDER_MAX_FPS = 180;
    public static final int NUM_AGENTS = 5_000;
    public static final int MAX_RENDERED_AGENTS = 20_000;
    public static final long DEFAULT_SEED = 1337L;
    public static final int FIELD_BIN_COUNT = 5;
    public static final float DEFAULT_FOOD_RICHNESS = 0.55f;
    public static final float DEFAULT_HAZARD_BASELINE = 0.35f;
    public static final float DEFAULT_PATCHINESS = 0.5f;
    public static final float DEFAULT_WATER_RATIO = 0.2f;
    public static final float BRUSH_MAX_DELTA = 0.4f;
    public static final float BRUSH_ERASE_BLEND = 0.35f;

    public static final float INITIAL_ENERGY = 1.0f;
    public static final float INITIAL_HUNGER = 1.0f;
    public static final float INITIAL_STRESS = 0.0f;

    public static final float HUNGER_DRAIN_PER_TICK = 0.0008f;
    public static final float ENERGY_DRAIN_PER_TICK = 0.00045f;
    public static final float STRESS_RECOVERY_PER_TICK = 0.0006f;
    public static final float HAZARD_ENERGY_DRAIN_PER_TICK = 0.0025f;
    public static final float HAZARD_STRESS_GAIN_PER_TICK = 0.0020f;

    public static final float CONSUME_HUNGER_THRESHOLD = 0.65f;
    public static final float FOOD_MIN_TO_EAT = 0.02f;
    public static final float FOOD_CONSUME_RATE = 0.015f;
    public static final float FOOD_TO_HUNGER_GAIN = 0.8f;
    public static final float FOOD_TO_ENERGY_GAIN = 0.3f;

    public static final float PREDICTION_ERROR_JITTER = 0.0015f;
    public static final float AWARENESS_THRESHOLD = 0.7f;

    private SimConfig() {
    }
}
