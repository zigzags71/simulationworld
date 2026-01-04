package simcore.agents;

import simcore.config.SimConfig;
import simcore.util.MathUtil;
import simcore.world.WorldGrid;

import java.util.Random;

public class RandomWalkMovement implements MovementPolicy {
    private final Random random;

    public RandomWalkMovement(long seed) {
        this.random = new Random(seed);
    }

    @Override
    public void move(AgentState agent, WorldGrid world) {
        int attempt = 0;
        while (attempt < 4) {
            int dx = random.nextInt(3) - 1;
            int dy = random.nextInt(3) - 1;
            int nx = Math.min(Math.max(agent.getX() + dx, 0), SimConfig.WORLD_W - 1);
            int ny = Math.min(Math.max(agent.getY() + dy, 0), SimConfig.WORLD_H - 1);
            int idx = MathUtil.index(nx, ny, world.getWidth());
            if (!world.getWaterMask()[idx]) {
                agent.moveTo(nx, ny);
                return;
            }
            attempt++;
        }
    }
}
