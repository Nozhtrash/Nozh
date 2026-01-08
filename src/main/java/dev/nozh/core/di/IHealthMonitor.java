package dev.nozh.core.di;

import dev.nozh.core.telemetry.TelemetrySnapshot;

/**
 * Interface for health monitoring - enables DI.
 */
public interface IHealthMonitor {
    void updateFromTelemetry(TelemetrySnapshot snapshot);
    void recordError(String errorMessage);
    boolean isHealthy();
    String getHealthStatus();
    String getHealthReport();
    void shutdown();
}
