package dev.nozh.core.benchmark;

import dev.nozh.NozhConstants;
import dev.nozh.api.PerfSnapshot;
import dev.nozh.core.state.StateStore;
import dev.nozh.core.telemetry.TelemetrySnapshot;

import java.util.function.Supplier;

/**
 * Automatic, non-intrusive benchmark to calibrate baseline telemetry.
 */
public final class InitialBenchmarkRunner {

    private static final long START_DELAY_MS = 5_000L;
    private static final long DURATION_MS = 10_000L;

    private final StateStore stateStore;
    private final Supplier<PerfSnapshot> snapshotSupplier;

    private boolean sessionActive = false;
    private boolean benchmarkStarted = false;
    private boolean benchmarkCompleted = false;
    private long sessionStartMillis = 0L;
    private long benchmarkStartMillis = 0L;

    public InitialBenchmarkRunner(StateStore stateStore, Supplier<PerfSnapshot> snapshotSupplier) {
        this.stateStore = stateStore;
        this.snapshotSupplier = snapshotSupplier;
    }

    public void onSessionStart() {
        sessionActive = true;
        benchmarkStarted = false;
        benchmarkCompleted = false;
        sessionStartMillis = System.currentTimeMillis();
        benchmarkStartMillis = 0L;
    }

    public void onSessionEnd() {
        sessionActive = false;
        benchmarkStarted = false;
        benchmarkCompleted = false;
        benchmarkStartMillis = 0L;
    }

    public void tick() {
        if (!sessionActive || benchmarkCompleted) {
            return;
        }

        long now = System.currentTimeMillis();
        var state = stateStore.snapshotSafe();
        if (!benchmarkStarted) {
            if (state.benchmarkRunning() || !"NONE".equalsIgnoreCase(state.benchmarkValidity())) {
                benchmarkCompleted = true;
                return;
            }
            if (now - sessionStartMillis < START_DELAY_MS) {
                return;
            }
            benchmarkStarted = true;
            benchmarkStartMillis = now;
            try {
                stateStore.update(s -> s.withBenchmarkStatus(true, benchmarkStartMillis, state.benchmarkValidity()));
            } catch (Exception e) {
                NozhConstants.LOGGER.warn("Failed to start initial benchmark: {}", e.getMessage());
            }
            return;
        }

        if (now - benchmarkStartMillis < DURATION_MS) {
            return;
        }

        PerfSnapshot snapshot = snapshotSupplier.get();
        if (snapshot == null) {
            snapshot = PerfSnapshot.empty();
        }
        TelemetrySnapshot telemetry = TelemetrySnapshot.of(
                snapshot.avgFrametimeMs(),
                snapshot.p95FrametimeMs(),
                snapshot.spikeCount(),
                snapshot.sampleCount(),
                0);
        BenchmarkValidity validity = NoiseDetector.classify(telemetry);
        try {
            stateStore.update(s -> s.withBenchmarkStatus(false, benchmarkStartMillis, validity.name()));
        } catch (Exception e) {
            NozhConstants.LOGGER.warn("Failed to finish initial benchmark: {}", e.getMessage());
        }
        benchmarkCompleted = true;
    }
}
