package simcore.sim;

public class TickLoop implements Runnable {
    private final int targetRate;
    private final Runnable step;
    private volatile boolean running;

    public TickLoop(int targetRate, Runnable step) {
        this.targetRate = targetRate;
        this.step = step;
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        running = true;
        long frameDurationNanos = 1_000_000_000L / targetRate;
        while (running) {
            long start = System.nanoTime();
            step.run();
            long elapsed = System.nanoTime() - start;
            long sleepNanos = frameDurationNanos - elapsed;
            if (sleepNanos > 0) {
                try {
                    Thread.sleep(sleepNanos / 1_000_000L, (int) (sleepNanos % 1_000_000L));
                } catch (InterruptedException ignored) {
                    running = false;
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
