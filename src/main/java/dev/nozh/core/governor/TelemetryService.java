package dev.nozh.core.governor;

import dev.nozh.NozhConstants;
import dev.nozh.core.telemetry.IntegratedRingTelemetryBuffer;
import dev.nozh.core.monitoring.PerformanceEventLogger;
import dev.nozh.core.monitoring.SystemHealthMonitor;
import dev.nozh.core.telemetry.TelemetrySample;
import dev.nozh.core.telemetry.TelemetrySnapshot;
import dev.nozh.core.monitoring.MetricsCollector;

import java.nio.file.Path;

/**
 * Service dedicated to collecting, aggregating, and logging performance
 * telemetry.
 * Extracted from IntegratedGovernor to reduce complexity.
 */
public class TelemetryService {

    private final IntegratedRingTelemetryBuffer telemetryBuffer;
    private final SystemHealthMonitor healthMonitor;
    private final PerformanceEventLogger eventLogger;
    private final MetricsCollector metricsCollector;

    // Configurable interval for logging
    private int logIntervalTicks = 100;

    public TelemetryService(Path logPath) {
        this.telemetryBuffer = new IntegratedRingTelemetryBuffer(512);
        this.healthMonitor = new SystemHealthMonitor();
        this.eventLogger = new PerformanceEventLogger(logPath);
        this.metricsCollector = new MetricsCollector();
    }

    public void processSample(TelemetrySample sample, int tickCounter) {
        if (sample == null || telemetryBuffer == null) {
            recordError("null_sample_or_buffer");
            return;
        }

        // 1. Add to ring buffer
        telemetryBuffer.add(sample);

        // 2. Snapshot for immediate analysis
        TelemetrySnapshot snapshot = telemetryBuffer.snapshot();
        if (snapshot == null) {
            recordError("null_telemetry_snapshot");
            return;
        }

        // 3. Update monitors
        try {
            healthMonitor.updateFromTelemetry(snapshot);
            metricsCollector.recordTelemetry(snapshot);
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Telemetry update failed", e);
        }

        // 4. Periodic Logging
        if (tickCounter % logIntervalTicks == 0) {
            try {
                double avgFps = 1000.0 / snapshot.avgFrametimeMs();
                eventLogger.logMetrics(avgFps, snapshot.p95FrametimeMs(), snapshot.spikeCount());
            } catch (Exception e) {
                NozhConstants.LOGGER.error("Event logging failed", e);
            }
        }
    }

    public TelemetrySnapshot getSnapshot() {
        return telemetryBuffer != null ? telemetryBuffer.snapshot() : null;
    }

    public SystemHealthMonitor getHealthMonitor() {
        return healthMonitor;
    }

    public MetricsCollector getMetricsCollector() {
        return metricsCollector;
    }

    public PerformanceEventLogger getEventLogger() {
        return eventLogger;
    }

    public void recordError(String errorKey) {
        if (healthMonitor != null) {
            healthMonitor.recordError(errorKey);
        }
    }
}
