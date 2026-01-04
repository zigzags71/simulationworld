package simcore.world;

/**
 * Represents environmental scalar fields for a tile.
 */
public final class TileFields {
    private final float food;
    private final float hazard;
    private final boolean water;

    public TileFields(float food, float hazard, boolean water) {
        this.food = food;
        this.hazard = hazard;
        this.water = water;
    }

    public float getFood() {
        return food;
    }

    public float getHazard() {
        return hazard;
    }

    public boolean isWater() {
        return water;
    }
}
