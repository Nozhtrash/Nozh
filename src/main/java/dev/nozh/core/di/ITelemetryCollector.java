package dev.nozh.core.di;

import dev.nozh.core.telemetry.TelemetrySnapshot;

/**
 * Interface for telemetry collection - enables DI.
 * 
 * Simplified version using only basic types.
 */
public interface ITelemetryCollector {
    /**
     * Collects and stores current telemetry.
     * 
     * @return true if collection succeeded
     */
    boolean collectAndStore();
    
    /**
     * Gets a snapshot of recent telemetry.
     * 
     * @return snapshot, never null
     */
    TelemetrySnapshot getSnapshot();
    
    /**
     * Gets the number of dropped samples.
     * 
     * @return dropped count
     */
    int getDroppedCount();
    
    /**
     * Clears all telemetry data.
     */
    void clear();
}
