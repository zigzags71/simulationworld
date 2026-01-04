package simcore.sim.commands;

public class SpawnAgentsCommand {
    private final int centerX;
    private final int centerY;
    private final int radius;
    private final int count;
    private final long seed;

    public SpawnAgentsCommand(int centerX, int centerY, int radius, int count, long seed) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = Math.max(1, radius);
        this.count = Math.max(0, count);
        this.seed = seed;
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

    public int getCount() {
        return count;
    }

    public long getSeed() {
        return seed;
    }
}
