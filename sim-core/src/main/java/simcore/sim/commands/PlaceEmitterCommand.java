package simcore.sim.commands;

public class PlaceEmitterCommand {
    public final int x;
    public final int y;
    public final int radius;
    public final float strength;
    public final boolean enabled;

    public PlaceEmitterCommand(int x, int y, int radius, float strength, boolean enabled) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.strength = strength;
        this.enabled = enabled;
    }
}
