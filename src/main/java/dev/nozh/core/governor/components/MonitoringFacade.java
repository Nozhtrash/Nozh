package dev.nozh.core.governor.components;

import dev.nozh.NozhConstants;
import dev.nozh.core.monitoring.*;
import dev.nozh.core.telemetry.TelemetrySnapshot;
import java.nio.file.Path;

/**
 * Facade for all monitoring concerns (health, metrics, logging).
 * 
 * Extracted from IntegratedGovernor as part of God Class refactoring.
 * This class provides a unified interface for monitoring operations.
 * 
 * <p><b>Thread Safety:</b> This class is thread-safe.
 * 
 * <p><b>Null Safety:</b> All methods handle null gracefully.
 * 
 * @author Nozh Team
 * @since 0.4.0
 */
public final class MonitoringFacade {
    
    private final SystemHealthMonitor healthMonitor;
    private final PerformanceEventLogger eventLogger;
    private final MetricsCollector metricsCollector;
    
    /**
     * Constructs a new MonitoringFacade.
     * 
     * @param logPath path for performance logs (must not be null)
     * @throws NullPointerException if logPath is null
     */
    public MonitoringFacade(Path logPath) {
        if (logPath == null) {
            throw new NullPointerException("Log path cannot be null");
        }
        
        this.healthMonitor = new SystemHealthMonitor();
        this.eventLogger = new PerformanceEventLogger(logPath);
        this.metricsCollector = new MetricsCollector();
        
        NozhConstants.LOGGER.info("MonitoringFacade initialized");
    }
    
    /**
     * Updates all monitors with telemetry data.
     * 
     * @param snapshot telemetry snapshot (may be null)
     */
    public void updateFromTelemetry(TelemetrySnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        
        // Update health monitor
        try {
            healthMonitor.updateFromTelemetry(snapshot);
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Health monitor update failed", e);
        }
        
        // Record metrics
        try {
            metricsCollector.recordTelemetry(snapshot);
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Metrics recording failed", e);
        }
    }
    
    /**
     * Logs periodic metrics.
     * 
     * @param avgFps average FPS
     * @param p95Frametime 95th percentile frametime
     * @param spikeCount spike count
     */
    public void logPeriodicMetrics(double avgFps, double p95Frametime, int spikeCount) {
        try {
            eventLogger.logMetrics(avgFps, p95Frametime, spikeCount);
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Event logging failed", e);
        }
    }
    
    /**
     * Logs action execution.
     * 
     * @param actionId action ID
     * @param success success flag
     * @param durationMs duration in milliseconds
     */
    public void logActionExecution(String actionId, boolean success, long durationMs) {
        try {
            eventLogger.logActionExecution(actionId, success, durationMs);
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to log action execution", e);
        }
        
        try {
            metricsCollector.recordAction(actionId, success, durationMs);
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to record action metrics", e);
        }
    }
    
    /**
     * Records an error.
     * 
     * @param errorMessage error message
     */
    public void recordError(String errorMessage) {
        if (errorMessage != null) {
            healthMonitor.recordError(errorMessage);
        }
    }
    
    /**
     * Checks if system is healthy.
     * 
     * @return true if healthy
     */
    public boolean isHealthy() {
        return !healthMonitor.isCritical();
    }
    
    /**
     * Gets health status string.
     * 
     * @return health status
     */
    public String getHealthStatus() {
        try {
            return healthMonitor.getHealthStatus();
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to get health status", e);
            return "ERROR";
        }
    }
    
    /**
     * Gets detailed health report.
     * 
     * @return health report
     */
    public String getHealthReport() {
        try {
            return String.format(
                    "Health: %s (%.2f) | Memory: %.1f%% | GC: %d pauses (%.1fms avg)",
                    healthMonitor.getHealthStatus(),
                    healthMonitor.getHealthScore(),
                    healthMonitor.getMemoryUsagePercent() * 100,
                    healthMonitor.getGCCount(),
                    healthMonitor.getAverageGCPause()
            );
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to generate health report", e);
            return "Health report generation failed: " + e.getMessage();
        }
    }
    
    /**
     * Gets metrics summary.
     * 
     * @return metrics map
     */
    public java.util.Map<String, Object> getMetricsSummary() {
        try {
            return metricsCollector.getSummary();
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to get metrics summary", e);
            return java.util.Collections.emptyMap();
        }
    }
    
    /**
     * Shuts down all monitoring components.
     */
    public void shutdown() {
        try {
            eventLogger.shutdown();
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to shutdown event logger", e);
        }
        
        NozhConstants.LOGGER.info("MonitoringFacade shutdown complete");
    }
}
