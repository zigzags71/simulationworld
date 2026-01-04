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
    private final int[] backAgentAge;
    private final float[] backAgentEnergy;
    private final float[] backAgentHunger;
    private final float[] backAgentStress;
    private final float[] backAgentPredictionError;
    private final boolean[] backAgentAwareness;
    private final int[] backAgentCulture;
    private final int[] backAgentCounts;
    private int backAgentCount;
    private final int[] backEmitterX;
    private final int[] backEmitterY;
    private final float[] backEmitterStrength;
    private final int[] backEmitterRadius;
    private final boolean[] backEmitterEnabled;
    private final long[] backEmitterId;
    private int backEmitterCount;
    private SelectedAgentDetails selectedAgentDetails;

    public SnapshotBuffer(int width, int height, int maxAgents) {
        backFood = new float[width * height];
        backHazard = new float[width * height];
        backCrowding = new float[width * height];
        backAgentX = new int[maxAgents];
        backAgentY = new int[maxAgents];
        backAgentColor = new int[maxAgents];
        backAgentIds = new long[maxAgents];
        backAgentAge = new int[maxAgents];
        backAgentEnergy = new float[maxAgents];
        backAgentHunger = new float[maxAgents];
        backAgentStress = new float[maxAgents];
        backAgentPredictionError = new float[maxAgents];
        backAgentAwareness = new boolean[maxAgents];
        backAgentCulture = new int[maxAgents];
        backAgentCounts = new int[width * height];
        backAgentCount = 0;
        backEmitterX = new int[simcore.config.SimConfig.MAX_EMITTERS_RENDERED];
        backEmitterY = new int[simcore.config.SimConfig.MAX_EMITTERS_RENDERED];
        backEmitterStrength = new float[simcore.config.SimConfig.MAX_EMITTERS_RENDERED];
        backEmitterRadius = new int[simcore.config.SimConfig.MAX_EMITTERS_RENDERED];
        backEmitterEnabled = new boolean[simcore.config.SimConfig.MAX_EMITTERS_RENDERED];
        backEmitterId = new long[simcore.config.SimConfig.MAX_EMITTERS_RENDERED];
        backEmitterCount = 0;
        back = new RenderSnapshot(width, height, backFood, backHazard, backCrowding, backAgentX, backAgentY, backAgentColor, backAgentIds,
                backAgentAge, backAgentEnergy, backAgentHunger, backAgentStress, backAgentPredictionError, backAgentAwareness,
                backAgentCulture, backAgentCounts, backAgentCount, backEmitterX, backEmitterY, backEmitterStrength, backEmitterRadius,
                backEmitterEnabled, backEmitterId, backEmitterCount, 0, null);
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

    public void setEmitterCount(int count) {
        this.backEmitterCount = count;
    }

    public void setSelectedAgentDetails(SelectedAgentDetails selectedAgentDetails) {
        this.selectedAgentDetails = selectedAgentDetails;
    }

    public void publish(long tickIndex) {
        RenderSnapshot updated = new RenderSnapshot(back.getWidth(), back.getHeight(), backFood, backHazard, backCrowding,
                backAgentX, backAgentY, backAgentColor, backAgentIds, backAgentAge, backAgentEnergy, backAgentHunger, backAgentStress,
                backAgentPredictionError, backAgentAwareness, backAgentCulture, backAgentCounts, backAgentCount,
                backEmitterX, backEmitterY, backEmitterStrength, backEmitterRadius, backEmitterEnabled, backEmitterId, backEmitterCount,
                tickIndex, selectedAgentDetails);
        front.set(updated);
    }
}
