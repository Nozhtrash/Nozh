package dev.nozh.core.monitoring;

import dev.nozh.NozhConstants;
import dev.nozh.core.telemetry.TelemetrySnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Monitors system health and detects degradation.
 * 
 * Tracks:
 * - Governor health (is it making good decisions?)
 * - Telemetry quality (is data reliable?)
 * - Action success rate
 * - System stability
 * 
 * Triggers recovery when issues detected.
 * 
 * TASK 11: Health monitoring - degradation detection
 */
public final class SystemHealthMonitor {

    private final ConcurrentLinkedQueue<HealthEvent> recentEvents = new ConcurrentLinkedQueue<>();
    private static final int MAX_EVENTS = 100;

    private volatile HealthStatus currentStatus = HealthStatus.HEALTHY;
    private volatile long lastHealthCheck = System.currentTimeMillis();

    private int consecutiveFailures = 0;
    private int consecutiveSlowFrames = 0;
    private int actionRollbackCount = 0;

    private static final int FAILURE_THRESHOLD = 5;
    private static final int SLOW_FRAME_THRESHOLD = 60; // 3 seconds @ 20 TPS
    private static final int ROLLBACK_THRESHOLD = 3;

    /**
     * Update health based on telemetry.
     */
    public void updateFromTelemetry(TelemetrySnapshot snapshot) {
        lastHealthCheck = System.currentTimeMillis();

        if (snapshot.avgFrametimeMs() > 50.0) {
            consecutiveSlowFrames++;
            if (consecutiveSlowFrames > SLOW_FRAME_THRESHOLD) {
                recordEvent(HealthEventType.PERFORMANCE_DEGRADED, "Sustained slow frames");
                updateStatus(HealthStatus.DEGRADED);
            }
        } else {
            consecutiveSlowFrames = 0;
        }
    }

    /**
     * Record action success.
     */
    public void recordActionSuccess(String actionId) {
        consecutiveFailures = 0;
        recordEvent(HealthEventType.ACTION_SUCCESS, "Action succeeded: " + actionId);
    }

    /**
     * Record action failure.
     */
    public void recordActionFailure(String actionId, String reason) {
        consecutiveFailures++;
        recordEvent(HealthEventType.ACTION_FAILURE, "Action failed: " + actionId + " - " + reason);

        if (consecutiveFailures >= FAILURE_THRESHOLD) {
            updateStatus(HealthStatus.UNHEALTHY);
        }
    }

    /**
     * Record action rollback.
     */
    public void recordRollback(String actionId) {
        actionRollbackCount++;
        recordEvent(HealthEventType.ROLLBACK_OCCURRED, "Rolled back: " + actionId);

        if (actionRollbackCount >= ROLLBACK_THRESHOLD) {
            updateStatus(HealthStatus.DEGRADED);
        }
    }

    /**
     * Record recovery action.
     */
    public void recordRecovery(String recoveryAction) {
        recordEvent(HealthEventType.RECOVERY_INITIATED, recoveryAction);
        consecutiveFailures = 0;
        consecutiveSlowFrames = 0;
        actionRollbackCount = 0;
    }

    /**
     * Get current health status.
     */
    public HealthStatus getStatus() {
        return currentStatus;
    }

    /**
     * Check if system is healthy.
     */
    public boolean isHealthy() {
        return currentStatus == HealthStatus.HEALTHY;
    }

    /**
     * Get recent health events.
     */
    public List<HealthEvent> getRecentEvents(int count) {
        List<HealthEvent> events = new ArrayList<>(recentEvents);
        return events.subList(0, Math.min(count, events.size()));
    }

    /**
     * Generate health report.
     */
    public String generateHealthReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== SYSTEM HEALTH REPORT ===\n");
        report.append("Status: ").append(currentStatus).append("\n");
        report.append("Consecutive Failures: ").append(consecutiveFailures).append("\n");
        report.append("Consecutive Slow Frames: ").append(consecutiveSlowFrames).append("\n");
        report.append("Rollback Count: ").append(actionRollbackCount).append("\n");
        report.append("\nRecent Events (last 10):\n");

        int count = 0;
        for (HealthEvent event : recentEvents) {
            if (count++ >= 10) break;
            report.append("  ").append(event).append("\n");
        }

        return report.toString();
    }

    /**
     * Clear health history.
     */
    public void reset() {
        recentEvents.clear();
        consecutiveFailures = 0;
        consecutiveSlowFrames = 0;
        actionRollbackCount = 0;
        updateStatus(HealthStatus.HEALTHY);
    }

    private void recordEvent(HealthEventType type, String details) {
        HealthEvent event = new HealthEvent(type, details, System.currentTimeMillis());
        recentEvents.offer(event);

        while (recentEvents.size() > MAX_EVENTS) {
            recentEvents.poll();
        }

        if (type == HealthEventType.ACTION_FAILURE || type == HealthEventType.PERFORMANCE_DEGRADED) {
            NozhConstants.LOGGER.warn("Health event: " + event);
        }
    }

    private void updateStatus(HealthStatus newStatus) {
        if (newStatus != currentStatus) {
            NozhConstants.LOGGER.warn("Health status changed: " + currentStatus + " -> " + newStatus);
            currentStatus = newStatus;
            recordEvent(HealthEventType.STATUS_CHANGED, "Status: " + newStatus);
        }
    }

    public enum HealthStatus {
        HEALTHY,
        DEGRADED,
        UNHEALTHY,
        CRITICAL
    }

    public enum HealthEventType {
        ACTION_SUCCESS,
        ACTION_FAILURE,
        ROLLBACK_OCCURRED,
        PERFORMANCE_DEGRADED,
        RECOVERY_INITIATED,
        STATUS_CHANGED
    }

    public record HealthEvent(HealthEventType type, String details, long timestamp) {
        @Override
        public String toString() {
            return String.format("[%tT] %s: %s", timestamp, type, details);
        }
    }
}
