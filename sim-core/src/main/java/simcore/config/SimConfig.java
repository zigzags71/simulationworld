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
    public static final long DEFAULT_SEED = 1337L;
    public static final int FIELD_BIN_COUNT = 5;

    private SimConfig() {
    }
}
