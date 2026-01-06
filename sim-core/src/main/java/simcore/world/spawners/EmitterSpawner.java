package simcore.world.spawners;

import java.util.Random;

import simcore.config.SimConfig;
import simcore.world.WorldGrid;
import simcore.world.objects.FoodEmitter;

public class EmitterSpawner {
    private final long id;
    private final int centerX;
    private final int centerY;
    private final int radius;
    private final SpawnerType type;
    private long activeEmitterId = -1;
    private long nextRespawnTick = 0;
    private final Random random;

    public EmitterSpawner(long id, int centerX, int centerY, int radius, SpawnerType type) {
        this.id = id;
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
        this.type = type;
        this.random = new Random(id * 31 + centerX * 17L + centerY * 13L);
    }

    public long getId() {
        return id;
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

    public SpawnerType getType() {
        return type;
    }

    public long getActiveEmitterId() {
        return activeEmitterId;
    }

    public void tick(long tick, WorldGrid world) {
        if (activeEmitterId != -1) {
            FoodEmitter emitter = world.findEmitterById(activeEmitterId);
            if (emitter == null || emitter.isExpired(tick)) {
                if (emitter != null) {
                    world.removeEmitterById(emitter.getId());
                }
                activeEmitterId = -1;
                nextRespawnTick = tick + SimConfig.SPAWNER_RESPAWN_COOLDOWN_TICKS;
            }
        }
        if (activeEmitterId == -1 && tick >= nextRespawnTick) {
            spawnEmitter(tick, world);
        }
    }

    private void spawnEmitter(long tick, WorldGrid world) {
        for (int attempts = 0; attempts < 50; attempts++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dy = random.nextInt(radius * 2 + 1) - radius;
            if (dx * dx + dy * dy > radius * radius) {
                continue;
            }
            int x = Math.max(0, Math.min(world.getWidth() - 1, centerX + dx));
            int y = Math.max(0, Math.min(world.getHeight() - 1, centerY + dy));
            int idx = y * world.getWidth() + x;
            if (world.getWaterMask()[idx]) {
                continue;
            }
            if (type == SpawnerType.FOOD) {
                FoodEmitter emitter = world.addEmitter(x, y, SimConfig.FOOD_EMITTER_RADIUS, SimConfig.FOOD_EMITTER_STRENGTH, true);
                emitter.setExpiresAtTick(tick + SimConfig.FOOD_EMITTER_TTL_TICKS);
                activeEmitterId = emitter.getId();
            }
            return;
        }
        nextRespawnTick = tick + SimConfig.SPAWNER_RESPAWN_COOLDOWN_TICKS;
    }
}
