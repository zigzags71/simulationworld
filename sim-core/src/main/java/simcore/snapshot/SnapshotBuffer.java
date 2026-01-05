package simcore.snapshot;

import java.util.concurrent.atomic.AtomicReference;

public class SnapshotBuffer {
    private final int width;
    private final int height;
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

    private final float[] frontFoodA;
    private final float[] frontHazardA;
    private final float[] frontCrowdingA;
    private final int[] frontAgentXA;
    private final int[] frontAgentYA;
    private final int[] frontAgentColorA;
    private final long[] frontAgentIdsA;
    private final int[] frontAgentAgeA;
    private final float[] frontAgentEnergyA;
    private final float[] frontAgentHungerA;
    private final float[] frontAgentStressA;
    private final float[] frontAgentPredictionErrorA;
    private final boolean[] frontAgentAwarenessA;
    private final int[] frontAgentCultureA;
    private final int[] frontAgentCountsA;
    private final int[] frontEmitterXA;
    private final int[] frontEmitterYA;
    private final float[] frontEmitterStrengthA;
    private final int[] frontEmitterRadiusA;
    private final boolean[] frontEmitterEnabledA;
    private final long[] frontEmitterIdA;

    private final float[] frontFoodB;
    private final float[] frontHazardB;
    private final float[] frontCrowdingB;
    private final int[] frontAgentXB;
    private final int[] frontAgentYB;
    private final int[] frontAgentColorB;
    private final long[] frontAgentIdsB;
    private final int[] frontAgentAgeB;
    private final float[] frontAgentEnergyB;
    private final float[] frontAgentHungerB;
    private final float[] frontAgentStressB;
    private final float[] frontAgentPredictionErrorB;
    private final boolean[] frontAgentAwarenessB;
    private final int[] frontAgentCultureB;
    private final int[] frontAgentCountsB;
    private final int[] frontEmitterXB;
    private final int[] frontEmitterYB;
    private final float[] frontEmitterStrengthB;
    private final int[] frontEmitterRadiusB;
    private final boolean[] frontEmitterEnabledB;
    private final long[] frontEmitterIdB;

    public SnapshotBuffer(int width, int height, int maxAgents) {
        this.width = width;
        this.height = height;
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

        frontFoodA = new float[width * height];
        frontHazardA = new float[width * height];
        frontCrowdingA = new float[width * height];
        frontAgentXA = new int[maxAgents];
        frontAgentYA = new int[maxAgents];
        frontAgentColorA = new int[maxAgents];
        frontAgentIdsA = new long[maxAgents];
        frontAgentAgeA = new int[maxAgents];
        frontAgentEnergyA = new float[maxAgents];
        frontAgentHungerA = new float[maxAgents];
        frontAgentStressA = new float[maxAgents];
        frontAgentPredictionErrorA = new float[maxAgents];
        frontAgentAwarenessA = new boolean[maxAgents];
        frontAgentCultureA = new int[maxAgents];
        frontAgentCountsA = new int[width * height];
        frontEmitterXA = new int[simcore.config.SimConfig.MAX_EMITTERS_RENDERED];
        frontEmitterYA = new int[simcore.config.SimConfig.MAX_EMITTERS_RENDERED];
        frontEmitterStrengthA = new float[simcore.config.SimConfig.MAX_EMITTERS_RENDERED];
        frontEmitterRadiusA = new int[simcore.config.SimConfig.MAX_EMITTERS_RENDERED];
        frontEmitterEnabledA = new boolean[simcore.config.SimConfig.MAX_EMITTERS_RENDERED];
        frontEmitterIdA = new long[simcore.config.SimConfig.MAX_EMITTERS_RENDERED];

        frontFoodB = new float[width * height];
        frontHazardB = new float[width * height];
        frontCrowdingB = new float[width * height];
        frontAgentXB = new int[maxAgents];
        frontAgentYB = new int[maxAgents];
        frontAgentColorB = new int[maxAgents];
        frontAgentIdsB = new long[maxAgents];
        frontAgentAgeB = new int[maxAgents];
        frontAgentEnergyB = new float[maxAgents];
        frontAgentHungerB = new float[maxAgents];
        frontAgentStressB = new float[maxAgents];
        frontAgentPredictionErrorB = new float[maxAgents];
        frontAgentAwarenessB = new boolean[maxAgents];
        frontAgentCultureB = new int[maxAgents];
        frontAgentCountsB = new int[width * height];
        frontEmitterXB = new int[simcore.config.SimConfig.MAX_EMITTERS_RENDERED];
        frontEmitterYB = new int[simcore.config.SimConfig.MAX_EMITTERS_RENDERED];
        frontEmitterStrengthB = new float[simcore.config.SimConfig.MAX_EMITTERS_RENDERED];
        frontEmitterRadiusB = new int[simcore.config.SimConfig.MAX_EMITTERS_RENDERED];
        frontEmitterEnabledB = new boolean[simcore.config.SimConfig.MAX_EMITTERS_RENDERED];
        frontEmitterIdB = new long[simcore.config.SimConfig.MAX_EMITTERS_RENDERED];

        RenderSnapshot initialFront = new RenderSnapshot(width, height, frontFoodA, frontHazardA, frontCrowdingA, frontAgentXA, frontAgentYA,
                frontAgentColorA, frontAgentIdsA, frontAgentAgeA, frontAgentEnergyA, frontAgentHungerA, frontAgentStressA,
                frontAgentPredictionErrorA, frontAgentAwarenessA, frontAgentCultureA, frontAgentCountsA, 0, frontEmitterXA,
                frontEmitterYA, frontEmitterStrengthA, frontEmitterRadiusA, frontEmitterEnabledA, frontEmitterIdA, 0, 0, null);
        front = new AtomicReference<>(initialFront);
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
        RenderSnapshot current = front.get();
        boolean useBufferA = current.getFood() != frontFoodA;
        float[] targetFood = useBufferA ? frontFoodA : frontFoodB;
        float[] targetHazard = useBufferA ? frontHazardA : frontHazardB;
        float[] targetCrowding = useBufferA ? frontCrowdingA : frontCrowdingB;
        int[] targetAgentX = useBufferA ? frontAgentXA : frontAgentXB;
        int[] targetAgentY = useBufferA ? frontAgentYA : frontAgentYB;
        int[] targetAgentColor = useBufferA ? frontAgentColorA : frontAgentColorB;
        long[] targetAgentIds = useBufferA ? frontAgentIdsA : frontAgentIdsB;
        int[] targetAgentAge = useBufferA ? frontAgentAgeA : frontAgentAgeB;
        float[] targetAgentEnergy = useBufferA ? frontAgentEnergyA : frontAgentEnergyB;
        float[] targetAgentHunger = useBufferA ? frontAgentHungerA : frontAgentHungerB;
        float[] targetAgentStress = useBufferA ? frontAgentStressA : frontAgentStressB;
        float[] targetAgentPredictionError = useBufferA ? frontAgentPredictionErrorA : frontAgentPredictionErrorB;
        boolean[] targetAgentAwareness = useBufferA ? frontAgentAwarenessA : frontAgentAwarenessB;
        int[] targetAgentCulture = useBufferA ? frontAgentCultureA : frontAgentCultureB;
        int[] targetAgentCounts = useBufferA ? frontAgentCountsA : frontAgentCountsB;
        int[] targetEmitterX = useBufferA ? frontEmitterXA : frontEmitterXB;
        int[] targetEmitterY = useBufferA ? frontEmitterYA : frontEmitterYB;
        float[] targetEmitterStrength = useBufferA ? frontEmitterStrengthA : frontEmitterStrengthB;
        int[] targetEmitterRadius = useBufferA ? frontEmitterRadiusA : frontEmitterRadiusB;
        boolean[] targetEmitterEnabled = useBufferA ? frontEmitterEnabledA : frontEmitterEnabledB;
        long[] targetEmitterId = useBufferA ? frontEmitterIdA : frontEmitterIdB;

        System.arraycopy(backFood, 0, targetFood, 0, targetFood.length);
        System.arraycopy(backHazard, 0, targetHazard, 0, targetHazard.length);
        System.arraycopy(backCrowding, 0, targetCrowding, 0, targetCrowding.length);
        System.arraycopy(backAgentX, 0, targetAgentX, 0, targetAgentX.length);
        System.arraycopy(backAgentY, 0, targetAgentY, 0, targetAgentY.length);
        System.arraycopy(backAgentColor, 0, targetAgentColor, 0, targetAgentColor.length);
        System.arraycopy(backAgentIds, 0, targetAgentIds, 0, targetAgentIds.length);
        System.arraycopy(backAgentAge, 0, targetAgentAge, 0, targetAgentAge.length);
        System.arraycopy(backAgentEnergy, 0, targetAgentEnergy, 0, targetAgentEnergy.length);
        System.arraycopy(backAgentHunger, 0, targetAgentHunger, 0, targetAgentHunger.length);
        System.arraycopy(backAgentStress, 0, targetAgentStress, 0, targetAgentStress.length);
        System.arraycopy(backAgentPredictionError, 0, targetAgentPredictionError, 0, targetAgentPredictionError.length);
        System.arraycopy(backAgentAwareness, 0, targetAgentAwareness, 0, targetAgentAwareness.length);
        System.arraycopy(backAgentCulture, 0, targetAgentCulture, 0, targetAgentCulture.length);
        System.arraycopy(backAgentCounts, 0, targetAgentCounts, 0, targetAgentCounts.length);
        System.arraycopy(backEmitterX, 0, targetEmitterX, 0, targetEmitterX.length);
        System.arraycopy(backEmitterY, 0, targetEmitterY, 0, targetEmitterY.length);
        System.arraycopy(backEmitterStrength, 0, targetEmitterStrength, 0, targetEmitterStrength.length);
        System.arraycopy(backEmitterRadius, 0, targetEmitterRadius, 0, targetEmitterRadius.length);
        System.arraycopy(backEmitterEnabled, 0, targetEmitterEnabled, 0, targetEmitterEnabled.length);
        System.arraycopy(backEmitterId, 0, targetEmitterId, 0, targetEmitterId.length);

        RenderSnapshot updated = new RenderSnapshot(width, height, targetFood, targetHazard, targetCrowding,
                targetAgentX, targetAgentY, targetAgentColor, targetAgentIds, targetAgentAge, targetAgentEnergy, targetAgentHunger,
                targetAgentStress, targetAgentPredictionError, targetAgentAwareness, targetAgentCulture, targetAgentCounts,
                backAgentCount, targetEmitterX, targetEmitterY, targetEmitterStrength, targetEmitterRadius, targetEmitterEnabled,
                targetEmitterId, backEmitterCount, tickIndex, selectedAgentDetails);
        front.set(updated);
    }
}
