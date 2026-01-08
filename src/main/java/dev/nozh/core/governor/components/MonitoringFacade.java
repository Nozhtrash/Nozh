package dev.nozh.core.governor.components;

import dev.nozh.NozhConstants;
import dev.nozh.core.monitoring.*;
import dev.nozh.core.telemetry.TelemetrySnapshot;
import java.nio.file.Path;

/**
 * Facade for monitoring concerns (health, metrics, logging).
 * 
 * Simplified version that works with existing main branch code.
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
     * @param logPath path for performance logs
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
     * @param snapshot telemetry snapshot
     */
    public void updateFromTelemetry(TelemetrySnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        
        try {
            healthMonitor.updateFromTelemetry(snapshot);
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Health monitor update failed", e);
        }
        
        try {
            metricsCollector.recordTelemetry(snapshot);
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Metrics recording failed", e);
        }
    }
    
    /**
     * Logs periodic metrics.
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
     */
    public void logActionExecution(String actionId, boolean success, long durationMs) {
        try {
            eventLogger.logActionExecution(actionId, success, durationMs);
            metricsCollector.recordAction(actionId, success, durationMs);
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to log action", e);
        }
    }
    
    /**
     * Records an error.
     */
    public void recordError(String errorMessage) {
        if (errorMessage != null) {
            try {
                healthMonitor.recordError(errorMessage);
            } catch (Exception e) {
                NozhConstants.LOGGER.error("Failed to record error", e);
            }
        }
    }
    
    /**
     * Checks if system is healthy.
     */
    public boolean isHealthy() {
        try {
            return !healthMonitor.isCritical();
        } catch (Exception e) {
            return true;
        }
    }
    
    /**
     * Gets health status string.
     */
    public String getHealthStatus() {
        try {
            return healthMonitor.getHealthStatus();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
    
    /**
     * Gets detailed health report.
     */
    public String getHealthReport() {
        try {
            return String.format(
                    "Health: %s (%.2f) | Memory: %.1f%%",
                    healthMonitor.getHealthStatus(),
                    healthMonitor.getHealthScore(),
                    healthMonitor.getMemoryUsagePercent() * 100
            );
        } catch (Exception e) {
            return "Health report unavailable";
        }
    }
    
    /**
     * Gets metrics summary.
     */
    public java.util.Map<String, Object> getMetricsSummary() {
        try {
            return metricsCollector.getSummary();
        } catch (Exception e) {
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
