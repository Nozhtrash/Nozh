package dev.nozh.core.di;

import dev.nozh.core.telemetry.TelemetrySample;
import dev.nozh.core.telemetry.TelemetrySnapshot;

/**
 * Interface for telemetry collection - enables DI.
 */
public interface ITelemetryCollector {
    TelemetrySample collectAndStore();
    TelemetrySnapshot getSnapshot();
    int getDroppedCount();
    void clear();
}
