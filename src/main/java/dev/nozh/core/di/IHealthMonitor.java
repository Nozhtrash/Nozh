package dev.nozh.core.di;

import dev.nozh.core.telemetry.TelemetrySnapshot;

/**
 * Interface for health monitoring - enables DI.
 * 
 * Simplified version using only basic types.
 */
public interface IHealthMonitor {
    /**
     * Updates health monitor from telemetry snapshot.
     * 
     * @param snapshot telemetry snapshot
     */
    void updateFromTelemetry(TelemetrySnapshot snapshot);
    
    /**
     * Records an error for health tracking.
     * 
     * @param errorMessage error message
     */
    void recordError(String errorMessage);
    
    /**
     * Checks if the system is healthy.
     * 
     * @return true if healthy
     */
    boolean isHealthy();
    
    /**
     * Gets health status as a string.
     * 
     * @return health status (e.g., "HEALTHY", "WARNING", "CRITICAL")
     */
    String getHealthStatus();
    
    /**
     * Gets a detailed health report.
     * 
     * @return health report string
     */
    String getHealthReport();
    
    /**
     * Shuts down the health monitor.
     */
    void shutdown();
}
