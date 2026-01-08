package dev.nozh.core.monitoring;

import dev.nozh.NozhConstants;
import dev.nozh.core.telemetry.TelemetrySnapshot;
import net.minecraft.client.MinecraftClient;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Monitors overall system health including memory, GC activity, and error rates.
 * Provides health scores and warnings to prevent crashes and instability.
 * 
 * <p>Health monitoring includes:
 * <ul>
 *   <li>Heap memory usage and pressure</li>
 *   <li>Garbage collection frequency and duration</li>
 *   <li>Error rates with time-windowing</li>
 *   <li>Real-time performance (FPS)</li>
 *   <li>Circuit breaker for critical states</li>
 * </ul>
 * 
 * <p><b>Thread Safety:</b> This class is fully thread-safe.
 * All mutable state uses atomic operations or proper synchronization.
 * 
 * <p><b>Performance:</b> Health score cached for 1 second (adaptive).
 * Circuit breaker activates after 5 consecutive critical states.
 * 
 * @author Nozh Team
 * @since 0.2.0
 */
public class SystemHealthMonitor {
    private static final double CRITICAL_MEMORY_THRESHOLD = 0.95; // 95% memory usage
    private static final double WARNING_MEMORY_THRESHOLD = 0.85; // 85% memory usage
    private static final int MAX_ERRORS_PER_MINUTE = 10;
    private static final long GC_WARNING_THRESHOLD = 500; // 500ms GC pause
    private static final long ERROR_WINDOW_MS = 60000; // 1 minute window
    private static final long BASE_HEALTH_CACHE_MS = 1000; // Base cache time
    
    // Circuit breaker thresholds
    private static final int CRITICAL_THRESHOLD_TRIGGERS = 5;
    private static final long CIRCUIT_RESET_TIMEOUT = 30000; // 30 seconds
    
    private final MemoryMXBean memoryBean;
    private final ConcurrentHashMap<Long, AtomicInteger> errorCounts;
    
    // Thread-safe counters
    private final AtomicLong lastGCTime;
    private final AtomicLong totalGCTime;
    private final AtomicInteger gcCount;
    private final AtomicInteger criticalCounter;
    
    // Cached health score (volatile for visibility)
    private volatile double lastHealthScore;
    private volatile long lastHealthCheckTime;
    private volatile boolean circuitOpen;
    private volatile long circuitOpenedAt;
    
    /**
     * Creates a new SystemHealthMonitor.
     * Initializes all monitoring components in healthy state.
     */
    public SystemHealthMonitor() {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.errorCounts = new ConcurrentHashMap<>();
        this.lastGCTime = new AtomicLong(0);
        this.totalGCTime = new AtomicLong(0);
        this.gcCount = new AtomicInteger(0);
        this.criticalCounter = new AtomicInteger(0);
        this.lastHealthScore = 1.0;
        this.lastHealthCheckTime = 0;
        this.circuitOpen = false;
        this.circuitOpenedAt = 0;
    }
    
    /**
     * Updates health monitor from telemetry snapshot.
     * This method is called periodically by IntegratedGovernor.
     * 
     * <p><b>Thread Safety:</b> Safe to call concurrently.
     * 
     * @param snapshot telemetry snapshot to process (must not be null)
     * @throws NullPointerException if snapshot is null
     */
    public void updateFromTelemetry(TelemetrySnapshot snapshot) {
        if (snapshot == null) {
            throw new NullPointerException("Telemetry snapshot cannot be null");
        }
        
        // Trigger health recalculation
        getHealthScore();
        
        // Future: Extract GC metrics from telemetry if available
    }
    
    /**
     * Calculates overall system health score using weighted components.
     * 
     * <p>Health components and weights:
     * <ul>
     *   <li>Memory health (30%): Based on heap usage</li>
     *   <li>GC health (25%): Based on pause frequency and duration</li>
     *   <li>Error health (25%): Based on recent error rates</li>
     *   <li>Performance health (20%): Based on current FPS</li>
     * </ul>
     * 
     * <p><b>Caching:</b> Results cached for 1-5 seconds (adaptive).
     * 
     * <p><b>Circuit Breaker:</b> Returns 0.0 immediately if circuit is open.
     * 
     * <p><b>Thread Safety:</b> Safe to call concurrently.
     * 
     * @return Health score between 0.0 (critical) and 1.0 (healthy)
     */
    public double getHealthScore() {
        // Check circuit breaker first
        if (circuitOpen) {
            long now = System.currentTimeMillis();
            if (now - circuitOpenedAt > CIRCUIT_RESET_TIMEOUT) {
                // Try to reset circuit after timeout
                resetCircuitBreaker();
                NozhConstants.LOGGER.info("Health monitor circuit breaker reset after timeout");
            } else {
                return 0.0; // Fast fail during crisis
            }
        }
        
        long now = System.currentTimeMillis();
        long cacheTime = getAdaptiveCacheTime();
        
        // CRITICAL FIX: Always update circuit breaker, even when using cached score
        if (now - lastHealthCheckTime < cacheTime) {
            // Update circuit breaker with cached score
            updateCircuitBreaker(lastHealthScore);
            return lastHealthScore; // Return cached value
        }
        
        // Calculate all health components
        double memoryHealth = calculateMemoryHealth();
        double gcHealth = calculateGCHealth();
        double errorHealth = calculateErrorHealth();
        double performanceHealth = calculatePerformanceHealth();
        
        // Weighted average
        double score = (memoryHealth * 0.30) +
                      (gcHealth * 0.25) +
                      (errorHealth * 0.25) +
                      (performanceHealth * 0.20);
        
        // Update circuit breaker state
        updateCircuitBreaker(score);
        
        // Update cached values
        lastHealthScore = score;
        lastHealthCheckTime = now;
        
        return score;
    }
    
    /**
     * Gets adaptive cache time based on system load.
     * Higher memory pressure = longer cache time to reduce overhead.
     * 
     * @return cache time in milliseconds
     */
    private long getAdaptiveCacheTime() {
        double memoryPressure = getMemoryUsagePercent();
        
        if (memoryPressure > 0.9) {
            return BASE_HEALTH_CACHE_MS * 5; // 5s during high pressure
        } else if (memoryPressure > 0.7) {
            return BASE_HEALTH_CACHE_MS * 2; // 2s during medium
        } else {
            return BASE_HEALTH_CACHE_MS; // 1s normal
        }
    }
    
    /**
     * Updates circuit breaker state based on health score.
     * Opens circuit after 5 consecutive critical readings.
     * 
     * @param score current health score
     */
    private void updateCircuitBreaker(double score) {
        if (score < 0.2) {
            int count = criticalCounter.incrementAndGet();
            if (count >= CRITICAL_THRESHOLD_TRIGGERS && !circuitOpen) {
                circuitOpen = true;
                circuitOpenedAt = System.currentTimeMillis();
                NozhConstants.LOGGER.error(
                    "Health monitor circuit breaker OPENED! Critical state detected {} times",
                    count
                );
            }
        } else if (score > 0.4) {
            // Reset counter if health improves significantly
            criticalCounter.set(0);
        }
    }
    
    /**
     * Resets the circuit breaker to closed state.
     */
    private void resetCircuitBreaker() {
        circuitOpen = false;
        circuitOpenedAt = 0;
        criticalCounter.set(0);
    }
    
    /**
     * Calculates memory health based on heap usage.
     * 
     * @return memory health score between 0.0 and 1.0
     */
    private double calculateMemoryHealth() {
        try {
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            long maxMemory = heapUsage.getMax();
            
            if (maxMemory <= 0) {
                return 1.0; // Cannot determine, assume healthy
            }
            
            double usageRatio = (double) heapUsage.getUsed() / maxMemory;
            
            if (usageRatio >= CRITICAL_MEMORY_THRESHOLD) {
                return 0.0; // Critical: imminent OOM
            } else if (usageRatio >= WARNING_MEMORY_THRESHOLD) {
                double range = CRITICAL_MEMORY_THRESHOLD - WARNING_MEMORY_THRESHOLD;
                double position = usageRatio - WARNING_MEMORY_THRESHOLD;
                return 0.5 - (0.5 * (position / range));
            } else {
                return 0.5 + (0.5 * (1.0 - (usageRatio / WARNING_MEMORY_THRESHOLD)));
            }
        } catch (Exception e) {
            NozhConstants.LOGGER.warn("Error calculating memory health", e);
            return 0.5; // Fallback to neutral
        }
    }
    
    /**
     * Calculates GC health based on average pause duration.
     * 
     * @return GC health score between 0.0 and 1.0
     */
    private double calculateGCHealth() {
        int currentGcCount = gcCount.get();
        if (currentGcCount == 0) {
            return 1.0; // No GC activity yet
        }
        
        double avgGCTime = (double) totalGCTime.get() / currentGcCount;
        
        if (avgGCTime >= GC_WARNING_THRESHOLD) {
            double ratio = avgGCTime / (GC_WARNING_THRESHOLD * 2);
            return Math.max(0.0, 1.0 - ratio);
        }
        
        return 1.0;
    }
    
    /**
     * Calculates error health based on recent error rates.
     * 
     * @return error health score between 0.0 and 1.0
     */
    private double calculateErrorHealth() {
        long now = System.currentTimeMillis();
        long cutoff = now - ERROR_WINDOW_MS;
        
        // Clean old error counts
        errorCounts.entrySet().removeIf(e -> e.getKey() < cutoff);
        
        int recentErrors = errorCounts.values().stream()
            .mapToInt(AtomicInteger::get)
            .sum();
        
        if (recentErrors == 0) {
            return 1.0;
        } else if (recentErrors >= MAX_ERRORS_PER_MINUTE) {
            return 0.0;
        } else {
            return 1.0 - ((double) recentErrors / MAX_ERRORS_PER_MINUTE);
        }
    }
    
    /**
     * Calculates performance health based on current FPS.
     * 
     * @return performance health score between 0.0 and 1.0
     */
    private double calculatePerformanceHealth() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) {
                return 1.0;
            }
            
            int currentFps = client.getCurrentFps();
            if (currentFps < 0) {
                return 1.0;
            }
            
            int targetFps = 60;
            
            if (currentFps >= targetFps) {
                return 1.0;
            } else if (currentFps < targetFps / 2) {
                return 0.0;
            } else {
                return (double) currentFps / targetFps;
            }
        } catch (Exception e) {
            NozhConstants.LOGGER.warn("Error calculating performance health", e);
            return 1.0;
        }
    }
    
    /**
     * Records a GC pause for health monitoring.
     * 
     * <p><b>Thread Safety:</b> Safe to call concurrently.
     * 
     * @param durationMs duration of the GC pause in milliseconds (must be >= 0)
     * @throws IllegalArgumentException if durationMs < 0
     */
    public void recordGCPause(long durationMs) {
        if (durationMs < 0) {
            throw new IllegalArgumentException(
                "GC pause duration cannot be negative: " + durationMs
            );
        }
        
        if (durationMs > 10000) {
            NozhConstants.LOGGER.warn(
                "Extreme GC pause detected: {}ms (possible system freeze)",
                durationMs
            );
        }
        
        totalGCTime.addAndGet(durationMs);
        gcCount.incrementAndGet();
        lastGCTime.set(System.currentTimeMillis());
    }
    
    /**
     * Records an error for health monitoring.
     * 
     * <p><b>CRITICAL FIX:</b> Now uses atomic compute() for thread safety.
     * 
     * <p><b>Thread Safety:</b> Safe to call concurrently from multiple threads.
     * 
     * @param errorType type or category of the error
     * @throws NullPointerException if errorType is null
     */
    public void recordError(String errorType) {
        if (errorType == null) {
            throw new NullPointerException("Error type cannot be null");
        }
        
        long timestamp = System.currentTimeMillis();
        
        // FIXED: Use atomic compute() instead of computeIfAbsent() + increment
        // This ensures thread-safe increment without lost updates
        errorCounts.compute(timestamp, (key, value) -> {
            if (value == null) {
                return new AtomicInteger(1);
            }
            value.incrementAndGet();
            return value;
        });
    }
    
    /**
     * Checks if the system is in a critical state.
     * 
     * @return true if health score is below 0.3
     */
    public boolean isCritical() {
        return getHealthScore() < 0.3;
    }
    
    /**
     * Checks if the system is healthy.
     * 
     * @return true if health score is >= 0.7
     */
    public boolean isHealthy() {
        return getHealthScore() >= 0.7;
    }
    
    /**
     * Checks if the system needs attention.
     * 
     * @return true if health score is below 0.6
     */
    public boolean needsAttention() {
        return getHealthScore() < 0.6;
    }
    
    /**
     * Checks if circuit breaker is open.
     * 
     * @return true if circuit breaker is activated
     */
    public boolean isCircuitOpen() {
        return circuitOpen;
    }
    
    /**
     * Gets current memory usage as a percentage.
     * 
     * @return memory usage ratio (0.0 = empty, 1.0 = full)
     */
    public double getMemoryUsagePercent() {
        try {
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            long maxMemory = heapUsage.getMax();
            
            if (maxMemory <= 0) {
                return 0.0;
            }
            
            return (double) heapUsage.getUsed() / maxMemory;
        } catch (Exception e) {
            NozhConstants.LOGGER.warn("Error getting memory usage", e);
            return 0.0;
        }
    }
    
    /**
     * Suggests whether a garbage collection should be triggered.
     * 
     * @return true if memory usage is above warning threshold
     */
    public boolean shouldSuggestGC() {
        return getMemoryUsagePercent() >= WARNING_MEMORY_THRESHOLD;
    }
    
    /**
     * Gets health status enum.
     * 
     * @return current health status
     */
    public HealthStatus getStatus() {
        if (circuitOpen) {
            return HealthStatus.CRITICAL;
        }
        
        double score = getHealthScore();
        
        if (score >= 0.8) {
            return HealthStatus.HEALTHY;
        } else if (score >= 0.6) {
            return HealthStatus.GOOD;
        } else if (score >= 0.4) {
            return HealthStatus.WARNING;
        } else if (score >= 0.2) {
            return HealthStatus.POOR;
        } else {
            return HealthStatus.CRITICAL;
        }
    }
    
    /**
     * Gets a human-readable health status string.
     * 
     * @return status string representation
     */
    public String getHealthStatus() {
        return getStatus().name();
    }
    
    /**
     * Gets the number of GC pauses recorded.
     * 
     * @return total GC pause count
     */
    public int getGCCount() {
        return gcCount.get();
    }
    
    /**
     * Gets the average GC pause duration.
     * 
     * @return average duration in milliseconds
     */
    public double getAverageGCPause() {
        int count = gcCount.get();
        return count > 0 ? (double) totalGCTime.get() / count : 0.0;
    }
    
    /**
     * Gets recent error count (last minute).
     * 
     * @return number of errors in the last 60 seconds
     */
    public int getRecentErrorCount() {
        long now = System.currentTimeMillis();
        long cutoff = now - ERROR_WINDOW_MS;
        
        return errorCounts.entrySet().stream()
            .filter(e -> e.getKey() >= cutoff)
            .mapToInt(e -> e.getValue().get())
            .sum();
    }
    
    /**
     * Generates a detailed health report.
     * 
     * @return multi-line formatted health report
     */
    public String generateHealthReport() {
        HealthStatus status = getStatus();
        double score = lastHealthScore;
        double memoryPercent = getMemoryUsagePercent() * 100;
        int gcCount = getGCCount();
        double avgGcPause = getAverageGCPause();
        int errorCount = getRecentErrorCount();
        String circuitStatus = circuitOpen ? "OPEN (CRISIS MODE)" : "Closed";
        
        return String.format(
            "=== System Health Report ===\n" +
            "Overall Status: %s (%.2f/1.00)\n" +
            "Memory Usage: %.1f%%\n" +
            "GC Activity: %d pauses (avg %.1fms)\n" +
            "Error Rate: %d errors/min\n" +
            "Circuit Breaker: %s\n" +
            "Recommendation: %s",
            status.name(),
            score,
            memoryPercent,
            gcCount,
            avgGcPause,
            errorCount,
            circuitStatus,
            getRecommendation(status)
        );
    }
    
    /**
     * Gets recommendation based on current status.
     * 
     * @param status current health status
     * @return recommendation string
     */
    private String getRecommendation(HealthStatus status) {
        switch (status) {
            case CRITICAL:
                return "IMMEDIATE ACTION REQUIRED - Reduce workload or restart";
            case POOR:
                return "System under stress - Monitor closely";
            case WARNING:
                return "Performance degrading - Consider optimization";
            case GOOD:
                return "System stable - Normal operation";
            case HEALTHY:
                return "System performing optimally";
            default:
                return "Unknown state";
        }
    }
    
    /**
     * Resets all monitoring data.
     * 
     * <p><b>Thread Safety:</b> Safe to call concurrently.
     */
    public synchronized void reset() {
        errorCounts.clear();
        totalGCTime.set(0);
        gcCount.set(0);
        lastGCTime.set(0);
        criticalCounter.set(0);
        lastHealthScore = 1.0;
        lastHealthCheckTime = 0;
        resetCircuitBreaker();
        
        NozhConstants.LOGGER.info("SystemHealthMonitor reset - all metrics cleared");
    }
    
    /**
     * Health status enumeration.
     */
    public enum HealthStatus {
        /** System performing optimally (>= 80%) */
        HEALTHY,
        
        /** System stable (60-79%) */
        GOOD,
        
        /** Performance degrading (40-59%) */
        WARNING,
        
        /** System under stress (20-39%) */
        POOR,
        
        /** Critical state, immediate action needed (< 20%) */
        CRITICAL
    }
}
