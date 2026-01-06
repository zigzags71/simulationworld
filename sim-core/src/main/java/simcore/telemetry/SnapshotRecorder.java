package simcore.telemetry;

import simcore.config.MapGenConfig;
import simcore.config.SimConfig;
import simcore.events.SelectedRegionSnapshotEvent;
import simcore.events.TelemetryBus;
import simcore.events.TelemetryEvent;
import simcore.naming.CultureNameRegistry;
import simcore.naming.NameRegistry;
import simcore.snapshot.RenderSnapshot;
import simcore.snapshot.RuleView;
import simcore.snapshot.SelectedAgentDetails;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Writes periodic JSONL logs for summaries and selected diagnostics without blocking the tick thread.
 */
public class SnapshotRecorder implements AutoCloseable {
    private static final int QUEUE_CAPACITY = 4096;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String VERSION_TAG = "v0.4.3";

    private final Supplier<RenderSnapshot> snapshotSupplier;
    private final Supplier<SelectedRegionSnapshotEvent> regionSnapshotSupplier;
    private final Path runDirectory;
    private final BufferedWriter summaryWriter;
    private BufferedWriter selectionWriter;
    private final Deque<LogEntry> queue = new ArrayDeque<>(QUEUE_CAPACITY);
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Thread writerThread;

    public SnapshotRecorder(TelemetryBus telemetryBus,
                            Supplier<SelectedRegionSnapshotEvent> regionSnapshotSupplier,
                            Supplier<RenderSnapshot> snapshotSupplier,
                            MapGenConfig mapGenConfig,
                            long seed) throws IOException {
        this.snapshotSupplier = snapshotSupplier;
        this.regionSnapshotSupplier = regionSnapshotSupplier;
        this.runDirectory = buildRunDirectory(seed);
        Files.createDirectories(runDirectory);
        this.summaryWriter = Files.newBufferedWriter(runDirectory.resolve("summary.jsonl"), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        writeConfig(runDirectory, mapGenConfig, seed);
        this.writerThread = new Thread(this::drainQueue, "snapshot-writer");
        this.writerThread.setDaemon(true);
        this.writerThread.start();
        telemetryBus.subscribe(this::handleTelemetry);
    }

    private void handleTelemetry(TelemetryEvent event) {
        if (!running.get()) {
            return;
        }
        if (SimConfig.SUMMARY_INTERVAL_TICKS > 0 && event.getTickIndex() % SimConfig.SUMMARY_INTERVAL_TICKS == 0) {
            enqueue(new LogEntry(buildSummaryJson(event), LogType.SUMMARY));
        }
        if (SimConfig.SELECTION_INTERVAL_TICKS > 0 && event.getTickIndex() % SimConfig.SELECTION_INTERVAL_TICKS == 0) {
            enqueueSelection(event.getTickIndex());
        }
    }

    private void enqueueSelection(long tick) {
        RenderSnapshot snapshot = snapshotSupplier.get();
        SelectedAgentDetails agent = snapshot != null ? snapshot.getSelectedAgentDetails() : null;
        SelectedRegionSnapshotEvent region = regionSnapshotSupplier.get();
        if (agent == null && region == null) {
            return;
        }
        String json = buildSelectionJson(tick, agent, region);
        enqueue(new LogEntry(json, LogType.SELECTION));
    }

    private void enqueue(LogEntry entry) {
        synchronized (queue) {
            if (queue.size() >= QUEUE_CAPACITY) {
                if (entry.type == LogType.SUMMARY) {
                    dropOldest(LogType.SELECTION);
                    if (queue.size() >= QUEUE_CAPACITY) {
                        queue.pollFirst();
                    }
                } else {
                    if (!dropOldest(LogType.SELECTION)) {
                        return;
                    }
                }
            }
            queue.addLast(entry);
            queue.notifyAll();
        }
    }

    private boolean dropOldest(LogType type) {
        var iterator = queue.iterator();
        while (iterator.hasNext()) {
            LogEntry entry = iterator.next();
            if (entry.type == type) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    private void drainQueue() {
        try {
            while (running.get() || !queue.isEmpty()) {
                LogEntry entry;
                synchronized (queue) {
                    while (queue.isEmpty() && running.get()) {
                        queue.wait(50L);
                    }
                    entry = queue.pollFirst();
                }
                if (entry == null) {
                    continue;
                }
                writeEntry(entry);
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(summaryWriter);
            closeQuietly(selectionWriter);
        }
    }

    private void writeEntry(LogEntry entry) throws IOException {
        if (entry.type == LogType.SUMMARY) {
            summaryWriter.write(entry.json);
            summaryWriter.newLine();
            summaryWriter.flush();
        } else {
            ensureSelectionWriter();
            if (selectionWriter != null) {
                selectionWriter.write(entry.json);
                selectionWriter.newLine();
                selectionWriter.flush();
            }
        }
    }

    private void ensureSelectionWriter() throws IOException {
        if (selectionWriter != null) {
            return;
        }
        selectionWriter = Files.newBufferedWriter(runDirectory.resolve("selection.jsonl"),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private Path buildRunDirectory(long seed) {
        String timestamp = FORMATTER.format(LocalDateTime.now());
        return Path.of(SimConfig.LOG_DIR, "run_" + timestamp + "_seed" + seed + "_" + VERSION_TAG);
    }

    private void writeConfig(Path runDir, MapGenConfig mapGenConfig, long seed) throws IOException {
        Path configPath = runDir.resolve("config.json");
        String json = "{" +
                "\"seed\":" + seed + ',' +
                "\"worldWidth\":" + SimConfig.WORLD_W + ',' +
                "\"worldHeight\":" + SimConfig.WORLD_H + ',' +
                "\"foodRichness\":" + mapGenConfig.getFoodRichness() + ',' +
                "\"hazardBaseline\":" + mapGenConfig.getHazardBaseline() + ',' +
                "\"patchiness\":" + mapGenConfig.getPatchiness() + ',' +
                "\"waterRatio\":" + mapGenConfig.getWaterRatio() + ',' +
                "\"tileFoodMax\":" + SimConfig.TILE_FOOD_MAX + ',' +
                "\"foodPaintAdd\":" + SimConfig.FOOD_PAINT_ADD + ',' +
                "\"foodRegenPerTick\":" + SimConfig.FOOD_REGEN_PER_TICK + ',' +
                "\"summaryIntervalTicks\":" + SimConfig.SUMMARY_INTERVAL_TICKS + ',' +
                "\"selectionIntervalTicks\":" + SimConfig.SELECTION_INTERVAL_TICKS +
                "}";
        Files.writeString(configPath, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private String buildSummaryJson(TelemetryEvent event) {
        return String.format(Locale.US,
                "{\"tick\":%d,\"population\":%d,\"deathsThisTick\":%d,\"totalDeaths\":%d,\"meanEnergy\":%.6f,\"meanHunger\":%.6f,\"meanStress\":%.6f,\"meanPredictionError\":%.6f}",
                event.getTickIndex(), event.getPopulation(), event.getDeathsThisTick(), event.getTotalDeaths(),
                event.getMeanEnergy(), event.getMeanHunger(), event.getMeanStress(), event.getMeanPredictionError());
    }

    private String buildSelectionJson(long tick, SelectedAgentDetails agent, SelectedRegionSnapshotEvent region) {
        StringBuilder builder = new StringBuilder();
        builder.append('{').append("\"tick\":").append(tick);
        if (agent != null) {
            builder.append(',').append("\"agent\":{")
                    .append("\"id\":").append(agent.getAgentId()).append(',')
                    .append("\"firstNameId\":").append(agent.getFirstNameId()).append(',')
                    .append("\"surnameId\":").append(agent.getSurnameId()).append(',')
                    .append("\"name\":\"").append(NameRegistry.resolveFirstName(agent.getFirstNameId())).append(' ')
                    .append(NameRegistry.resolveSurname(agent.getSurnameId())).append("\",")
                    .append("\"cultureId\":").append(agent.getCultureId()).append(',')
                    .append("\"cultureName\":\"").append(CultureNameRegistry.resolveCultureName(agent.getCultureId())).append("\",")
                    .append("\"x\":").append(agent.getX()).append(',')
                    .append("\"y\":").append(agent.getY()).append(',')
                    .append("\"age\":").append(agent.getAgeTicks()).append(',')
                    .append("\"energy\":").append(formatFloat(agent.getEnergy())).append(',')
                    .append("\"hunger\":").append(formatFloat(agent.getHunger())).append(',')
                    .append("\"stress\":").append(formatFloat(agent.getStress())).append(',')
                    .append("\"predictionError\":").append(formatFloat(agent.getPredictionError())).append(',')
                    .append("\"socialCredit\":").append(formatFloat(agent.getSocialCredit())).append(',')
                    .append("\"rules\":[");
            RuleView[] rules = agent.getRules();
            for (int i = 0; i < rules.length; i++) {
                RuleView rule = rules[i];
                builder.append('{')
                        .append("\"id\":").append(rule.getRuleId()).append(',')
                        .append("\"type\":\"").append(rule.getType()).append("\",")
                        .append("\"action\":\"").append(rule.getAction()).append("\",")
                        .append("\"trust\":").append(formatFloat(rule.getTrust())).append(',')
                        .append("\"uses\":").append(rule.getUses()).append(',')
                        .append("\"successes\":").append(rule.getSuccesses())
                        .append('}');
                if (i < rules.length - 1) {
                    builder.append(',');
                }
            }
            builder.append(']').append('}');
        }
        if (region != null) {
            builder.append(',').append("\"region\":{")
                    .append("\"minX\":").append(region.getMinX()).append(',')
                    .append("\"minY\":").append(region.getMinY()).append(',')
                    .append("\"maxX\":").append(region.getMaxX()).append(',')
                    .append("\"maxY\":").append(region.getMaxY()).append(',')
                    .append("\"agentCount\":").append(region.getAgentCount()).append(',')
                    .append("\"avgEnergy\":").append(formatFloat(region.getAvgEnergy())).append(',')
                    .append("\"avgHunger\":").append(formatFloat(region.getAvgHunger())).append(',')
                    .append("\"avgStress\":").append(formatFloat(region.getAvgStress())).append(',')
                    .append("\"avgPredictionError\":").append(formatFloat(region.getAvgPredictionError()))
                    .append('}');
        }
        builder.append('}');
        return builder.toString();
    }

    private String formatFloat(float value) {
        return String.format(Locale.US, "%.6f", value);
    }

    @Override
    public void close() {
        running.set(false);
        synchronized (queue) {
            queue.notifyAll();
        }
        writerThread.interrupt();
    }

    private void closeQuietly(BufferedWriter writer) {
        if (writer == null) {
            return;
        }
        try {
            writer.close();
        } catch (IOException ignored) {
        }
    }

    private record LogEntry(String json, LogType type) {
    }

    private enum LogType {
        SUMMARY,
        SELECTION
    }
}
