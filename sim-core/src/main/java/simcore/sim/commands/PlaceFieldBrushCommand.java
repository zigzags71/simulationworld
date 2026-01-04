package simcore.sim.commands;

public class PlaceFieldBrushCommand {
    private final BrushType type;
    private final int centerX;
    private final int centerY;
    private final int radius;
    private final long seed;

    public PlaceFieldBrushCommand(BrushType type, int centerX, int centerY, int radius, long seed) {
        this.type = type;
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = Math.max(1, radius);
        this.seed = seed;
    }

    public BrushType getType() {
        return type;
    }

    public int getCenterX() {
        return centerX;
    }

    public int getCenterY() {
        return centerY;
    }

    public int getRadius() {
        return radius;
    }

    public long getSeed() {
        return seed;
    }
}
