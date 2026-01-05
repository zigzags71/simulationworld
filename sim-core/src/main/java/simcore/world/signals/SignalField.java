package simcore.world.signals;

import simcore.config.SimConfig;
import simcore.util.MathUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Deterministic container for signals with simple radius queries.
 */
public class SignalField {
    private static final int BUCKET_SIZE = 16;
    private final int width;
    private final int height;
    private final List<Signal> signals;
    private final int bucketsW;
    private final int bucketsH;
    private final List<List<Signal>> buckets;
    private long nextSignalId = 1;

    public SignalField(int width, int height) {
        this.width = width;
        this.height = height;
        this.signals = new ArrayList<>();
        this.bucketsW = (width + BUCKET_SIZE - 1) / BUCKET_SIZE;
        this.bucketsH = (height + BUCKET_SIZE - 1) / BUCKET_SIZE;
        this.buckets = new ArrayList<>(bucketsW * bucketsH);
        for (int i = 0; i < bucketsW * bucketsH; i++) {
            buckets.add(new ArrayList<>());
        }
    }

    public List<Signal> getSignals() {
        return Collections.unmodifiableList(signals);
    }

    public Signal addSignal(int x, int y, int strengthBucket, float confidence, int ttl, int generation, long originAgentId,
                            long tick) {
        strengthBucket = Math.max(0, Math.min(SimConfig.SIGNAL_STRENGTH_BINS, strengthBucket));
        confidence = MathUtil.clamp01(confidence);
        ttl = Math.max(0, ttl);
        generation = Math.max(0, generation);
        x = MathUtil.clamp(x, 0, width - 1);
        y = MathUtil.clamp(y, 0, height - 1);
        if (signals.size() >= SimConfig.SIGNAL_MAX_ACTIVE) {
            Signal removed = signals.remove(0);
            removeFromBucket(removed);
        }
        Signal signal = new Signal(nextSignalId++, x, y, strengthBucket, confidence, ttl, generation, originAgentId, tick);
        signals.add(signal);
        addToBucket(signal);
        return signal;
    }

    public void tickDecay() {
        Iterator<Signal> it = signals.iterator();
        while (it.hasNext()) {
            Signal signal = it.next();
            signal.decrementTtl();
            if (signal.getTtlTicks() <= 0) {
                it.remove();
            }
        }
        rebuildBuckets();
    }

    public boolean hasSignalWithinRadius(int x, int y, int radius) {
        return bestSignalWithinRadius(x, y, radius) != null;
    }

    public Signal bestSignalWithinRadius(int x, int y, int radius) {
        if (signals.isEmpty() || radius <= 0) {
            return null;
        }
        int rSq = radius * radius;
        Signal best = null;
        float bestScore = Float.NEGATIVE_INFINITY;
        int minBucketX = Math.max(0, (x - radius) / BUCKET_SIZE);
        int maxBucketX = Math.min(bucketsW - 1, (x + radius) / BUCKET_SIZE);
        int minBucketY = Math.max(0, (y - radius) / BUCKET_SIZE);
        int maxBucketY = Math.min(bucketsH - 1, (y + radius) / BUCKET_SIZE);
        for (int by = minBucketY; by <= maxBucketY; by++) {
            for (int bx = minBucketX; bx <= maxBucketX; bx++) {
                List<Signal> bucket = buckets.get(bucketIndex(bx, by));
                for (Signal signal : bucket) {
                    int dx = signal.getX() - x;
                    int dy = signal.getY() - y;
                    int distSq = dx * dx + dy * dy;
                    if (distSq > rSq) {
                        continue;
                    }
                    float distPenalty = (float) Math.sqrt(distSq);
                    float score = signal.getConfidence() * (0.5f + signal.getStrengthBucket()) - distPenalty;
                    if (score > bestScore || (score == bestScore && best != null && signal.getId() < best.getId())) {
                        bestScore = score;
                        best = signal;
                    }
                }
            }
        }
        return best;
    }

    public boolean isEmpty() {
        return signals.isEmpty();
    }

    private void rebuildBuckets() {
        for (List<Signal> bucket : buckets) {
            bucket.clear();
        }
        for (Signal signal : signals) {
            addToBucket(signal);
        }
    }

    private void addToBucket(Signal signal) {
        buckets.get(bucketIndex(signal.getX() / BUCKET_SIZE, signal.getY() / BUCKET_SIZE)).add(signal);
    }

    private void removeFromBucket(Signal signal) {
        buckets.get(bucketIndex(signal.getX() / BUCKET_SIZE, signal.getY() / BUCKET_SIZE)).remove(signal);
    }

    private int bucketIndex(int bucketX, int bucketY) {
        return bucketY * bucketsW + bucketX;
    }
}
