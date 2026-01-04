package simcore.rules;

import java.util.Objects;

public final class ContextKey {
    private final int hungerBin;
    private final int energyBin;
    private final int stressBin;
    private final int foodBin;
    private final int hazardBin;
    private final int crowdingBin;
    private final int awarenessFlag;
    private final int foodAffordance;

    public ContextKey(int hungerBin, int energyBin, int stressBin, int foodBin, int hazardBin, int crowdingBin,
                      int awarenessFlag, int foodAffordance) {
        this.hungerBin = hungerBin;
        this.energyBin = energyBin;
        this.stressBin = stressBin;
        this.foodBin = foodBin;
        this.hazardBin = hazardBin;
        this.crowdingBin = crowdingBin;
        this.awarenessFlag = awarenessFlag;
        this.foodAffordance = foodAffordance;
    }

    public int getHungerBin() {
        return hungerBin;
    }

    public int getEnergyBin() {
        return energyBin;
    }

    public int getStressBin() {
        return stressBin;
    }

    public int getFoodBin() {
        return foodBin;
    }

    public int getHazardBin() {
        return hazardBin;
    }

    public int getCrowdingBin() {
        return crowdingBin;
    }

    public int getAwarenessFlag() {
        return awarenessFlag;
    }

    public int getFoodAffordance() {
        return foodAffordance;
    }

    /**
     * Computes the Manhattan-like distance between two {@link ContextKey} instances.
     * <p>
     * Distance is defined as the sum of absolute differences across all binned numeric
     * features (hunger, energy, stress, food, hazard, crowding) plus {@code 1} if the
     * awareness flag differs and the popcount of differing affordance bits.
     * This definition must remain stable to keep rule matching predictable.
     *
     * @param other the other context key
     * @return distance as described above
     */
    public int distanceTo(ContextKey other) {
        int dist = 0;
        dist += Math.abs(hungerBin - other.hungerBin);
        dist += Math.abs(energyBin - other.energyBin);
        dist += Math.abs(stressBin - other.stressBin);
        dist += Math.abs(foodBin - other.foodBin);
        dist += Math.abs(hazardBin - other.hazardBin);
        dist += Math.abs(crowdingBin - other.crowdingBin);
        dist += (awarenessFlag == other.awarenessFlag) ? 0 : 1;
        dist += Integer.bitCount(foodAffordance ^ other.foodAffordance);
        return dist;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContextKey that = (ContextKey) o;
        return hungerBin == that.hungerBin && energyBin == that.energyBin && stressBin == that.stressBin && foodBin == that.foodBin && hazardBin == that.hazardBin && crowdingBin == that.crowdingBin && awarenessFlag == that.awarenessFlag && foodAffordance == that.foodAffordance;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hungerBin, energyBin, stressBin, foodBin, hazardBin, crowdingBin, awarenessFlag, foodAffordance);
    }

    @Override
    public String toString() {
        return "C{" + hungerBin + ',' + energyBin + ',' + stressBin + ',' + foodBin + ',' + hazardBin + ',' + crowdingBin + ',' + awarenessFlag + ',' + foodAffordance + '}';
    }
}
