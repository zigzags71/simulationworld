package uiviewer.config;

public final class UIConfig {
    public static final long BRUSH_INTERVAL_MS = 50L;
    public static final int REGION_AGENT_LIST_CAP = 500;
    public static final int DEFAULT_AGENT_SPAWN = 20;
    public static final int MIN_AGENT_SPAWN = 5;
    public static final int MAX_AGENT_SPAWN = 50;
    public static final boolean RENDER_BENCHMARK_LOGGING = Boolean.getBoolean("ui.render.benchmark");

    private UIConfig() {
    }
}
