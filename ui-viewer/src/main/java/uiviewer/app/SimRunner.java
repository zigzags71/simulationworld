package uiviewer.app;

import simcore.sim.SimulationEngine;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class SimRunner {
    private final SimulationEngine engine;
    private final ScheduledExecutorService executor;
    private final AtomicLong tickCounter = new AtomicLong();
    private volatile boolean running;
    private volatile int targetTps;
    private ScheduledFuture<?> scheduledTask;
    private long lastTickStartNanos = 0L;

    public SimRunner(SimulationEngine engine, int initialTps) {
        this.engine = engine;
        this.targetTps = initialTps;
        ThreadFactory factory = r -> {
            Thread t = new Thread(r, "sim-runner");
            t.setDaemon(true);
            return t;
        };
        this.executor = Executors.newSingleThreadScheduledExecutor(factory);
    }

    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        engine.resume();
        schedule();
    }

    public synchronized void pause() {
        running = false;
        engine.pause();
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTask = null;
        }
    }

    public synchronized void stop() {
        pause();
        executor.shutdownNow();
    }

    public synchronized void setTargetTps(int targetTps) {
        if (targetTps <= 0) {
            return;
        }
        this.targetTps = targetTps;
        if (running) {
            schedule();
        }
    }

    public long pollTicksPerSecond() {
        return tickCounter.getAndSet(0);
    }

    private void schedule() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }
        if (targetTps > 1000) {
            scheduledTask = executor.scheduleAtFixedRate(this::stepBurstSafely, 0, 1, TimeUnit.MILLISECONDS);
        } else {
            long periodNanos = 1_000_000_000L / targetTps;
            scheduledTask = executor.scheduleAtFixedRate(this::stepSafely, 0, periodNanos, TimeUnit.NANOSECONDS);
        }
    }

    private void stepSafely() {
        long now = System.nanoTime();
        long period = 1_000_000_000L / targetTps;
        if (lastTickStartNanos != 0 && now - lastTickStartNanos > period * 2L) {
            lastTickStartNanos = now;
            engine.step();
            tickCounter.incrementAndGet();
            return;
        }
        lastTickStartNanos = now;
        engine.step();
        tickCounter.incrementAndGet();
    }

    private void stepBurstSafely() {
        long now = System.nanoTime();
        long stepsPerCall = Math.max(1, targetTps / 1000);
        if (lastTickStartNanos != 0 && now - lastTickStartNanos > 2_000_000L) {
            lastTickStartNanos = now;
        }
        lastTickStartNanos = now;
        for (int i = 0; i < stepsPerCall; i++) {
            engine.step();
        }
        tickCounter.addAndGet(stepsPerCall);
    }
}
