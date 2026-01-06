package simcore.world.objects;

import simcore.config.SimConfig;

/**
 * Deterministic food emitter that deposits food in a small radius each tick.
 */
public class FoodEmitter {
    private final long id;
    private int x;
    private int y;
    private int radius;
    private float strengthPerTick;
    private boolean enabled;
    private long expiresAtTick = Long.MAX_VALUE;
    private long leaderAgentId = -1;

    public FoodEmitter(long id, int x, int y, int radius, float strengthPerTick, boolean enabled) {
        this.id = id;
        this.x = x;
        this.y = y;
        setRadius(radius);
        setStrengthPerTick(strengthPerTick);
        this.enabled = enabled;
    }

    public long getId() {
        return id;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getRadius() {
        return radius;
    }

    public float getStrengthPerTick() {
        return strengthPerTick;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getExpiresAtTick() {
        return expiresAtTick;
    }

    public void setExpiresAtTick(long tick) {
        this.expiresAtTick = tick;
    }

    public boolean isExpired(long tick) {
        return tick >= expiresAtTick;
    }

    public long getLeaderAgentId() {
        return leaderAgentId;
    }

    public boolean hasLeader() {
        return leaderAgentId != -1;
    }

    public void setLeaderAgentId(long agentId) {
        if (leaderAgentId == -1) {
            this.leaderAgentId = agentId;
        }
    }

    public void setRadius(int radius) {
        int clamped = Math.max(0, Math.min(SimConfig.EMITTER_RADIUS_MAX, radius));
        this.radius = clamped;
    }

    public void setStrengthPerTick(float strengthPerTick) {
        this.strengthPerTick = Math.max(0f, strengthPerTick);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
