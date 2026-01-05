package simcore.world.signals;

/**
 * Lightweight representation of a signal broadcast in the world.
 */
public class Signal {
    private final long id;
    private final int x;
    private final int y;
    private final int strengthBucket;
    private final float confidence;
    private int ttlTicks;
    private final int generation;
    private final long originAgentId;
    private final long createdTick;

    public Signal(long id, int x, int y, int strengthBucket, float confidence, int ttlTicks, int generation,
                  long originAgentId, long createdTick) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.strengthBucket = strengthBucket;
        this.confidence = Math.max(0f, Math.min(1f, confidence));
        this.ttlTicks = Math.max(0, ttlTicks);
        this.generation = generation;
        this.originAgentId = originAgentId;
        this.createdTick = createdTick;
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

    public int getStrengthBucket() {
        return strengthBucket;
    }

    public float getConfidence() {
        return confidence;
    }

    public int getTtlTicks() {
        return ttlTicks;
    }

    public void decrementTtl() {
        ttlTicks = Math.max(0, ttlTicks - 1);
    }

    public int getGeneration() {
        return generation;
    }

    public long getOriginAgentId() {
        return originAgentId;
    }

    public long getCreatedTick() {
        return createdTick;
    }
}
