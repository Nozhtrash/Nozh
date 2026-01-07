package dev.nozh.core.profiler;

import dev.nozh.NozhConstants;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

/**
 * Watches for GC pause time using JMX MXBeans.
 * Tracks delta in collection time to detect recent GC activity.
 */
public class GcPauseWatcher {
    
    private final List<GarbageCollectorMXBean> gcBeans;
    private long lastTotalCollectionTime = 0;
    private long lastCheckTime = 0;
    private double recentGcMs = 0;
    
    // Check interval (don't poll MXBeans every frame)
    private static final long CHECK_INTERVAL_MS = 1000; // 1 second
    
    public GcPauseWatcher() {
        this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        this.lastTotalCollectionTime = getTotalCollectionTime();
        this.lastCheckTime = System.currentTimeMillis();
    }
    
    /**
     * Update GC tracking. Call periodically (e.g., every tick or every few frames).
     */
    public java.util.Optional<GcPauseEvent> update() {
        long now = System.currentTimeMillis();
        
        if (now - lastCheckTime < CHECK_INTERVAL_MS) {
            return java.util.Optional.empty();
        }
        
        long totalTime = getTotalCollectionTime();
        long delta = totalTime - lastTotalCollectionTime;
        long elapsed = now - lastCheckTime;
        
        // Calculate GC time as percentage of elapsed time, converted to ms
        // This gives us "how much of the last second was spent in GC"
        recentGcMs = delta; // Direct ms of GC time in the interval
        
        lastTotalCollectionTime = totalTime;
        lastCheckTime = now;
        
        if (delta > 0) {
            NozhConstants.LOGGER.debug("GC activity: {}ms in last {}ms", delta, elapsed);
            return java.util.Optional.of(new GcPauseEvent(now, elapsed, delta));
        }
        
        return java.util.Optional.empty();
    }
    
    /**
     * Get total GC collection time across all collectors
     */
    private long getTotalCollectionTime() {
        long total = 0;
        for (GarbageCollectorMXBean bean : gcBeans) {
            long time = bean.getCollectionTime();
            if (time > 0) {
                total += time;
            }
        }
        return total;
    }
    
    /**
     * Get recent GC pause time in ms (from last check interval)
     */
    public double getRecentGcMs() {
        return recentGcMs;
    }
    
    /**
     * Check if GC is causing significant pauses (> 50ms in last second)
     */
    public boolean isGcCausingPauses() {
        return recentGcMs > 50;
    }
    
    /**
     * Get GC pressure as a 0-1 score
     */
    public double getGcPressureScore() {
        // 100ms of GC in a second = 1.0 (very bad)
        return Math.min(1.0, recentGcMs / 100.0);
    }
}
