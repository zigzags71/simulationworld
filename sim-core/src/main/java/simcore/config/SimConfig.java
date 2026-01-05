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
    public static final float INITIAL_SOCIAL_CREDIT = 0.0f;

    public static final float HUNGER_DRAIN_PER_TICK = 0.0008f;
    public static final float ENERGY_DRAIN_PER_TICK = 0.00045f;
    public static final float STRESS_RECOVERY_PER_TICK = 0.0006f;
    public static final float HAZARD_ENERGY_DRAIN_PER_TICK = 0.0025f;
    public static final float HAZARD_STRESS_GAIN_PER_TICK = 0.0020f;

    public static final float CONSUME_HUNGER_THRESHOLD = 0.65f;
    public static final float FOOD_MIN_TO_EAT = 0.02f;
    public static final float FOOD_CONSUME_RATE = 0.015f;
    public static final float TILE_FOOD_MAX = 1.0f;
    public static final float FOOD_PAINT_ADD = 0.2f;
    public static final float FOOD_REGEN_PER_TICK = 0.0f;
    public static final float FOOD_TO_HUNGER_GAIN = 0.8f;
    public static final float FOOD_TO_ENERGY_GAIN = 0.3f;

    public static final int EMITTER_RADIUS_MAX = 50;
    public static final int EMITTER_DEFAULT_RADIUS = 6;
    public static final float EMITTER_DEFAULT_STRENGTH = 0.02f;
    public static final boolean EMITTER_PLACEMENT_ENABLED = true;

    public static final float RULE_MATCH_DISTANCE = 1.0f;
    public static final float TRUST_ALPHA = 2.0f;
    public static final float TRUST_EPSILON = 0.05f;
    public static final float TRUST_LEARN_UP = 0.02f;
    public static final float TRUST_LEARN_DOWN = 0.03f;
    public static final float ERROR_SUCCESS_THRESHOLD = 0.05f;
    public static final float PRED_ERROR_EMA_ALPHA = 0.1f;
    public static final float RULE_NEED_UTILITY_WEIGHT = 0.5f;

    public static final float MOVE_FOOD_WEIGHT = 1.0f;
    public static final float MOVE_HAZARD_WEIGHT = 0.8f;
    public static final float MOVE_ENERGY_COST = 0.0006f;
    public static final float MOVE_HUNGER_COST = 0.0005f;

    public static final float IDLE_STRESS_RECOVERY_BONUS = 0.0002f;

    public static final float CROWDING_MAX_EXPECTED = 6f;

    public static final int SIGNAL_TTL_TICKS = 600;
    public static final int SIGNAL_PICKUP_RADIUS_BASE = 6;
    public static final int SIGNAL_SENSE_RADIUS_BASE = 12;
    public static final int SIGNAL_SENSE_RADIUS_MAX = 200;
    public static final float SIGNAL_RELAY_TRUST_THRESHOLD = 0.65f;
    public static final int SIGNAL_VERIFY_WINDOW_TICKS = 80;
    public static final float SIGNAL_BROADCAST_COST_ENERGY = 0.01f;
    public static final float SIGNAL_BROADCAST_COST_HUNGER = 0.005f;
    public static final float SIGNAL_BASE_CONFIDENCE = 0.25f;
    public static final float SIGNAL_VERIFIED_CONFIDENCE = 0.75f;
    public static final float SIGNAL_RELAY_CONFIDENCE = 0.55f;
    public static final float SIGNAL_SOCIAL_CREDIT_WEIGHT = 0.5f;
    public static final int SIGNAL_STRENGTH_BINS = FIELD_BIN_COUNT;
    public static final int SIGNAL_BROADCAST_AFTER_EAT_WINDOW = 30;
    public static final int SIGNAL_BROADCAST_COOLDOWN_TICKS = 50;
    public static final int SIGNAL_MAX_ACTIVE = 5_000;

    public static final int SOCIAL_CREDIT_REWARD_WINDOW_TICKS = 60;
    public static final float SOCIAL_CREDIT_REWARD_PER_ENERGY_GAIN = 0.2f;

    public static boolean LOG_EVENTS_ENABLED = false;
    public static boolean LOG_SELECTED_AGENT_ENABLED = false;
    public static boolean LOG_SELECTED_REGION_ENABLED = false;
    public static int LOG_THROTTLE_TICKS = 20;
    public static boolean FILE_LOG_ENABLED = false;
    public static String LOG_DIR = "./logs";
    public static int SUMMARY_INTERVAL_TICKS = 20;
    public static int SELECTION_INTERVAL_TICKS = 20;
    public static final int MAX_EMITTERS_RENDERED = 2048;

    private SimConfig() {
    }
}
