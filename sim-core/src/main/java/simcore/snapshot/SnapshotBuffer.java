package simcore.snapshot;

import java.util.concurrent.atomic.AtomicReference;

public class SnapshotBuffer {
    private final AtomicReference<RenderSnapshot> front;
    private final RenderSnapshot back;
    private final float[] backFood;
    private final float[] backHazard;
    private final float[] backCrowding;
    private final int[] backAgentX;
    private final int[] backAgentY;
    private final int[] backAgentColor;
    private final long[] backAgentIds;
    private int backAgentCount;

    public SnapshotBuffer(int width, int height, int maxAgents) {
        backFood = new float[width * height];
        backHazard = new float[width * height];
        backCrowding = new float[width * height];
        backAgentX = new int[maxAgents];
        backAgentY = new int[maxAgents];
        backAgentColor = new int[maxAgents];
        backAgentIds = new long[maxAgents];
        backAgentCount = 0;
        back = new RenderSnapshot(width, height, backFood, backHazard, backCrowding, backAgentX, backAgentY, backAgentColor, backAgentIds, backAgentCount, 0);
        front = new AtomicReference<>(back);
    }

    public RenderSnapshot getLatest() {
        return front.get();
    }

    public RenderSnapshot beginWrite() {
        return back;
    }

    public void setAgentCount(int count) {
        this.backAgentCount = count;
    }

    public void publish(long tickIndex) {
        RenderSnapshot updated = new RenderSnapshot(back.getWidth(), back.getHeight(), backFood, backHazard, backCrowding, backAgentX, backAgentY, backAgentColor, backAgentIds, backAgentCount, tickIndex);
        front.set(updated);
    }
}
