package dev.nozh.core.monitoring;

import net.minecraft.client.MinecraftClient;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
 * </ul>
 * 
 * <p>Thread-safe and designed for minimal overhead during monitoring.
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
    private static final long HEALTH_CACHE_MS = 1000; // Cache health score for 1 second
    
    private final MemoryMXBean memoryBean;
    private final ConcurrentHashMap<Long, AtomicInteger> errorCounts;
    
    private long lastGCTime;
    private long totalGCTime;
    private int gcCount;
    private double lastHealthScore;
    private long lastHealthCheckTime;
    
    public SystemHealthMonitor() {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.errorCounts = new ConcurrentHashMap<>();
        this.lastGCTime = 0;
        this.totalGCTime = 0;
        this.gcCount = 0;
        this.lastHealthScore = 1.0;
        this.lastHealthCheckTime = 0;
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
     * @return Health score between 0.0 (critical) and 1.0 (healthy)
     */
    public double getHealthScore() {
        long now = System.currentTimeMillis();
        if (now - lastHealthCheckTime < HEALTH_CACHE_MS) {
            return lastHealthScore; // Cache for performance
        }
        
        double memoryHealth = calculateMemoryHealth();
        double gcHealth = calculateGCHealth();
        double errorHealth = calculateErrorHealth();
        double performanceHealth = calculatePerformanceHealth();
        
        // Weighted average of all components
        lastHealthScore = (memoryHealth * 0.30) +
                         (gcHealth * 0.25) +
                         (errorHealth * 0.25) +
                         (performanceHealth * 0.20);
        
        lastHealthCheckTime = now;
        return lastHealthScore;
    }
    
    /**
     * Calculates memory health based on heap usage.
     * Uses progressive scaling:
     * - 0-85% usage: Health 0.5-1.0 (linear)
     * - 85-95% usage: Health 0.0-0.5 (linear, warning zone)
     * - 95%+ usage: Health 0.0 (critical)
     */
    private double calculateMemoryHealth() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        long maxMemory = heapUsage.getMax();
        
        if (maxMemory <= 0) {
            return 1.0; // Cannot determine, assume healthy
        }
        
        double usageRatio = (double) heapUsage.getUsed() / maxMemory;
        
        if (usageRatio >= CRITICAL_MEMORY_THRESHOLD) {
            return 0.0; // Critical: imminent OOM
        } else if (usageRatio >= WARNING_MEMORY_THRESHOLD) {
            // Linear scale from 0.5 (at warning) to 0.0 (at critical)
            double range = CRITICAL_MEMORY_THRESHOLD - WARNING_MEMORY_THRESHOLD;
            double position = usageRatio - WARNING_MEMORY_THRESHOLD;
            return 0.5 - (0.5 * (position / range));
        } else {
            // Linear scale from 1.0 (at 0%) to 0.5 (at warning)
            return 0.5 + (0.5 * (1.0 - (usageRatio / WARNING_MEMORY_THRESHOLD)));
        }
    }
    
    /**
     * Calculates GC health based on average pause duration.
     * Longer and more frequent pauses indicate poor GC health.
     */
    private double calculateGCHealth() {
        if (gcCount == 0) {
            return 1.0; // No GC activity yet
        }
        
        double avgGCTime = (double) totalGCTime / gcCount;
        
        if (avgGCTime >= GC_WARNING_THRESHOLD) {
            // Poor GC health: frequent long pauses
            // Scale from 1.0 (at threshold) to 0.0 (at 2x threshold)
            double ratio = avgGCTime / (GC_WARNING_THRESHOLD * 2);
            return Math.max(0.0, 1.0 - ratio);
        }
        
        return 1.0;
    }
    
    /**
     * Calculates error health based on recent error rates.
     * Uses a sliding 1-minute window for error counting.
     */
    private double calculateErrorHealth() {
        long now = System.currentTimeMillis();
        long cutoff = now - ERROR_WINDOW_MS;
        
        // Clean old error counts (older than 1 minute)
        errorCounts.entrySet().removeIf(e -> e.getKey() < cutoff);
        
        int recentErrors = errorCounts.values().stream()
            .mapToInt(AtomicInteger::get)
            .sum();
        
        if (recentErrors == 0) {
            return 1.0; // No errors, perfect health
        } else if (recentErrors >= MAX_ERRORS_PER_MINUTE) {
            return 0.0; // Too many errors, critical
        } else {
            // Linear scale from 1.0 (0 errors) to 0.0 (max errors)
            return 1.0 - ((double) recentErrors / MAX_ERRORS_PER_MINUTE);
        }
    }
    
    /**
     * Calculates performance health based on current FPS vs target.
     * Lower FPS indicates performance issues.
     */
    private double calculatePerformanceHealth() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return 1.0; // Cannot determine, assume healthy
        }
        
        int currentFps = client.getCurrentFps();
        int targetFps = 60; // Standard target, could be configurable
        
        if (currentFps >= targetFps) {
            return 1.0; // Meeting or exceeding target
        } else if (currentFps < targetFps / 2) {
            return 0.0; // Less than half target, critical
        } else {
            // Linear scale from 1.0 (at target) to 0.0 (at half target)
            return (double) currentFps / targetFps;
        }
    }
    
    /**
     * Records a GC pause for health monitoring.
     * 
     * @param durationMs duration of the GC pause in milliseconds
     */
    public void recordGCPause(long durationMs) {
        if (durationMs < 0) {
            return;
        }
        
        totalGCTime += durationMs;
        gcCount++;
        lastGCTime = System.currentTimeMillis();
    }
    
    /**
     * Records an error for health monitoring.
     * Errors are tracked with timestamps for time-windowing.
     * 
     * @param errorType type or category of the error (currently unused, for future expansion)
     */
    public void recordError(String errorType) {
        long timestamp = System.currentTimeMillis();
        errorCounts.computeIfAbsent(timestamp, k -> new AtomicInteger()).incrementAndGet();
    }
    
    /**
     * Checks if the system is in a critical state requiring immediate intervention.
     * 
     * @return true if health score is below 0.3 (critical threshold)
     */
    public boolean isCritical() {
        return getHealthScore() < 0.3;
    }
    
    /**
     * Checks if the system needs attention but is not yet critical.
     * 
     * @return true if health score is below 0.6 (warning threshold)
     */
    public boolean needsAttention() {
        return getHealthScore() < 0.6;
    }
    
    /**
     * Gets current memory usage as a percentage.
     * 
     * @return memory usage ratio (0.0 = empty, 1.0 = full)
     */
    public double getMemoryUsagePercent() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        long maxMemory = heapUsage.getMax();
        
        if (maxMemory <= 0) {
            return 0.0;
        }
        
        return (double) heapUsage.getUsed() / maxMemory;
    }
    
    /**
     * Suggests whether a garbage collection should be triggered.
     * Based on memory pressure threshold.
     * 
     * @return true if memory usage is above warning threshold
     */
    public boolean shouldSuggestGC() {
        return getMemoryUsagePercent() >= WARNING_MEMORY_THRESHOLD;
    }
    
    /**
     * Gets a human-readable health status description.
     * 
     * @return status string: HEALTHY, GOOD, WARNING, POOR, or CRITICAL
     */
    public String getHealthStatus() {
        double score = getHealthScore();
        
        if (score >= 0.8) {
            return "HEALTHY";
        } else if (score >= 0.6) {
            return "GOOD";
        } else if (score >= 0.4) {
            return "WARNING";
        } else if (score >= 0.2) {
            return "POOR";
        } else {
            return "CRITICAL";
        }
    }
    
    /**
     * Gets the number of GC pauses recorded.
     * 
     * @return total GC pause count since creation or last reset
     */
    public int getGCCount() {
        return gcCount;
    }
    
    /**
     * Gets the average GC pause duration.
     * 
     * @return average duration in milliseconds, or 0 if no GC pauses recorded
     */
    public double getAverageGCPause() {
        return gcCount > 0 ? (double) totalGCTime / gcCount : 0.0;
    }
    
    /**
     * Resets all monitoring data.
     * Useful when starting a new monitoring session or after major state changes.
     */
    public void reset() {
        errorCounts.clear();
        totalGCTime = 0;
        gcCount = 0;
        lastGCTime = 0;
        lastHealthScore = 1.0;
        lastHealthCheckTime = 0;
    }
}
