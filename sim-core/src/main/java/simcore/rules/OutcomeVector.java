package simcore.rules;

import simcore.util.MathUtil;

public class OutcomeVector {
    private final float deltaEnergy;
    private final float deltaHunger;
    private final float deltaStress;

    public OutcomeVector(float deltaEnergy, float deltaHunger, float deltaStress) {
        this.deltaEnergy = deltaEnergy;
        this.deltaHunger = deltaHunger;
        this.deltaStress = deltaStress;
    }

    public float getDeltaEnergy() {
        return deltaEnergy;
    }

    public float getDeltaHunger() {
        return deltaHunger;
    }

    public float getDeltaStress() {
        return deltaStress;
    }

    public OutcomeVector add(OutcomeVector other) {
        return new OutcomeVector(deltaEnergy + other.deltaEnergy, deltaHunger + other.deltaHunger, deltaStress + other.deltaStress);
    }

    public OutcomeVector add(float energy, float hunger, float stress) {
        return new OutcomeVector(deltaEnergy + energy, deltaHunger + hunger, deltaStress + stress);
    }

    public OutcomeVector negate() {
        return new OutcomeVector(-deltaEnergy, -deltaHunger, -deltaStress);
    }

    public float distanceTo(OutcomeVector other, boolean l2) {
        float de = deltaEnergy - other.deltaEnergy;
        float dh = deltaHunger - other.deltaHunger;
        float ds = deltaStress - other.deltaStress;
        if (l2) {
            return (float) Math.sqrt(de * de + dh * dh + ds * ds);
        }
        return Math.abs(de) + Math.abs(dh) + Math.abs(ds);
    }

    public static OutcomeVector zero() {
        return new OutcomeVector(0f, 0f, 0f);
    }

    public static OutcomeVector fromAgent(float energy, float hunger, float stress) {
        return new OutcomeVector(energy, hunger, stress);
    }

    public OutcomeVector deltaFrom(OutcomeVector before) {
        return new OutcomeVector(deltaEnergy - before.deltaEnergy, deltaHunger - before.deltaHunger, deltaStress - before.deltaStress);
    }

    public OutcomeVector clamp01() {
        return new OutcomeVector(MathUtil.clamp01(deltaEnergy), MathUtil.clamp01(deltaHunger), MathUtil.clamp01(deltaStress));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OutcomeVector that = (OutcomeVector) o;
        return Float.compare(that.deltaEnergy, deltaEnergy) == 0
                && Float.compare(that.deltaHunger, deltaHunger) == 0
                && Float.compare(that.deltaStress, deltaStress) == 0;
    }

    @Override
    public int hashCode() {
        int result = Float.hashCode(deltaEnergy);
        result = 31 * result + Float.hashCode(deltaHunger);
        result = 31 * result + Float.hashCode(deltaStress);
        return result;
    }

    @Override
    public String toString() {
        return "OutcomeVector{" +
                "de=" + deltaEnergy +
                ", dh=" + deltaHunger +
                ", ds=" + deltaStress +
                '}';
    }
}
