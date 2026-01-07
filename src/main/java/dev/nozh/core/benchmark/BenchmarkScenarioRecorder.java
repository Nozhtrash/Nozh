package dev.nozh.core.benchmark;

import dev.nozh.NozhConstants;
import dev.nozh.api.PerfSnapshot;
import dev.nozh.core.context.Scenario;
import dev.nozh.core.profiler.PerfManager;
import dev.nozh.core.telemetry.TelemetryExportFormat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Records scenario-based benchmark artifacts and telemetry exports.
 */
public final class BenchmarkScenarioRecorder {

    private static final long SNAPSHOT_INTERVAL_MS = 1_000L;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            .withZone(ZoneOffset.UTC);

    private final PerfManager perfManager;
    private final Supplier<PerfSnapshot> snapshotSupplier;
    private final Path rootDir;
    private final BenchmarkSnapshotSeries snapshotSeries = new BenchmarkSnapshotSeries();

    private BenchmarkEnvironment environment;
    private Scenario activeScenario;
    private long scenarioStartMillis;
    private long lastSnapshotMillis;
    private boolean sessionActive;
    private Path sessionDir;
    private Path initialBenchmarkSnapshotJson;

    public BenchmarkScenarioRecorder(PerfManager perfManager, Supplier<PerfSnapshot> snapshotSupplier, Path rootDir) {
        this.perfManager = perfManager;
        this.snapshotSupplier = snapshotSupplier != null ? snapshotSupplier : PerfSnapshot::empty;
        this.rootDir = rootDir;
    }

    public void onSessionStart(BenchmarkEnvironment environment) {
        this.environment = environment;
        this.sessionActive = true;
        this.activeScenario = null;
        this.scenarioStartMillis = 0L;
        this.lastSnapshotMillis = 0L;
        this.snapshotSeries.clear();
        this.initialBenchmarkSnapshotJson = null;
        this.sessionDir = resolveSessionDir();
        writeMatrixSnapshot();
    }

    public void onSessionEnd() {
        if (!sessionActive) {
            return;
        }
        closeScenario(System.currentTimeMillis());
        sessionActive = false;
    }

    public void tick(Scenario scenario) {
        if (!sessionActive) {
            return;
        }
        Scenario resolvedScenario = scenario != null ? scenario : Scenario.STANDARD;
        long now = System.currentTimeMillis();
        if (activeScenario == null || activeScenario != resolvedScenario) {
            closeScenario(now);
            startScenario(resolvedScenario, now);
        }
        if (now - lastSnapshotMillis >= SNAPSHOT_INTERVAL_MS) {
            captureSnapshot();
            lastSnapshotMillis = now;
        }
    }

    public void recordInitialBenchmarkSnapshot(PerfSnapshot snapshot) {
        if (!sessionActive || snapshot == null || sessionDir == null) {
            return;
        }
        try {
            Path outputFile = sessionDir.resolve("initial_benchmark_snapshot.json");
            BenchmarkSnapshotSeries series = new BenchmarkSnapshotSeries();
            series.addSnapshot(snapshot);
            series.writeJson(outputFile);
            initialBenchmarkSnapshotJson = outputFile;
        } catch (Exception e) {
            NozhConstants.LOGGER.warn("Failed to write initial benchmark snapshot: {}", e.getMessage());
        }
    }

    private void startScenario(Scenario scenario, long now) {
        activeScenario = scenario;
        scenarioStartMillis = now;
        snapshotSeries.clear();
    }

    private void closeScenario(long now) {
        if (activeScenario == null || sessionDir == null) {
            return;
        }
        try {
            TelemetryExportFormat[] formats = {TelemetryExportFormat.CSV, TelemetryExportFormat.JSON};
            Path csv = null;
            Path json = null;
            for (TelemetryExportFormat format : formats) {
                Path export = perfManager != null ? perfManager.exportTelemetry(sessionDir, format) : null;
                if (format == TelemetryExportFormat.CSV) {
                    csv = export;
                } else {
                    json = export;
                }
            }
            Path snapshotsJson = null;
            if (!snapshotSeries.isEmpty()) {
                String timestamp = TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(now));
                snapshotsJson = sessionDir.resolve("snapshots_" + activeScenario.name().toLowerCase()
                        + "_" + timestamp + ".json");
                snapshotSeries.writeJson(snapshotsJson);
            }
            BenchmarkArtifactMetadata metadata = new BenchmarkArtifactMetadata(
                    environment,
                    activeScenario.name(),
                    scenarioStartMillis,
                    now,
                    csv,
                    json,
                    snapshotsJson,
                    initialBenchmarkSnapshotJson);
            writeMetadata(metadata, now);
        } catch (Exception e) {
            NozhConstants.LOGGER.warn("Failed to export benchmark artifacts: {}", e.getMessage());
        } finally {
            snapshotSeries.clear();
            activeScenario = null;
            scenarioStartMillis = 0L;
        }
    }

    private void captureSnapshot() {
        PerfSnapshot snapshot = snapshotSupplier.get();
        if (snapshot == null) {
            return;
        }
        snapshotSeries.addSnapshot(snapshot);
    }

    private Path resolveSessionDir() {
        String timestamp = TIMESTAMP_FORMAT.format(Instant.now());
        Path dir = rootDir.resolve("session_" + timestamp);
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            NozhConstants.LOGGER.warn("Failed to create benchmark session directory: {}", e.getMessage());
        }
        return dir;
    }

    private void writeMatrixSnapshot() {
        if (sessionDir == null) {
            return;
        }
        try {
            BenchmarkMatrix matrix = BenchmarkMatrix.defaultMatrix();
            Path outputFile = sessionDir.resolve("benchmark_matrix.json");
            Files.writeString(outputFile, matrix.toJson(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            NozhConstants.LOGGER.warn("Failed to write benchmark matrix: {}", e.getMessage());
        }
    }

    private void writeMetadata(BenchmarkArtifactMetadata metadata, long now) throws Exception {
        Objects.requireNonNull(sessionDir, "sessionDir");
        String timestamp = TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(now));
        String scenarioName = metadata.scenario().toLowerCase();
        Path outputFile = sessionDir.resolve("metadata_" + scenarioName + "_" + timestamp + ".json");
        Files.writeString(outputFile, metadata.toJson(), StandardCharsets.UTF_8);
    }
}
