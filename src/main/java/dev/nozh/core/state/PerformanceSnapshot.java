package dev.nozh.core.state;

/**
 * Immutable snapshot of performance metrics at a point in time.
 * Used by PerformancePredictor for trend analysis and predictions.
 * 
 * @param timestamp Unix timestamp in milliseconds when snapshot was taken
 * @param avgFrametime Average frametime in milliseconds
 * @param p95Frametime 95th percentile frametime in milliseconds (worst-case)
 * @param fps Current frames per second
 * @param spikeCount Number of performance spikes detected in this window
 * 
 * @author Nozh Team
 * @since 0.2.0
 */
public record PerformanceSnapshot(
        long timestamp,
        double avgFrametime,
        double p95Frametime,
        double fps,
        int spikeCount
) {
    /**
     * Validates snapshot data on construction.
     * 
     * @throws IllegalArgumentException if any metric is invalid
     */
    public PerformanceSnapshot {
        if (timestamp <= 0) {
            throw new IllegalArgumentException("Timestamp must be positive");
        }
        if (avgFrametime < 0 || p95Frametime < 0) {
            throw new IllegalArgumentException("Frametime cannot be negative");
        }
        if (fps < 0) {
            throw new IllegalArgumentException("FPS cannot be negative");
        }
        if (spikeCount < 0) {
            throw new IllegalArgumentException("Spike count cannot be negative");
        }
    }
    
    /**
     * Creates a snapshot with the current system time.
     * 
     * @param avgFrametime average frametime in ms
     * @param p95Frametime 95th percentile frametime in ms
     * @param fps current FPS
     * @param spikeCount number of spikes
     * @return new PerformanceSnapshot with current timestamp
     */
    public static PerformanceSnapshot now(double avgFrametime, double p95Frametime, 
                                         double fps, int spikeCount) {
        return new PerformanceSnapshot(
            System.currentTimeMillis(),
            avgFrametime,
            p95Frametime,
            fps,
            spikeCount
        );
    }
    
    /**
     * Checks if this snapshot indicates performance degradation.
     * 
     * @param targetFps the target FPS to compare against
     * @return true if FPS is below 90% of target or P95 frametime is high
     */
    public boolean isDegraded(double targetFps) {
        double targetFrametime = 1000.0 / targetFps;
        return fps < targetFps * 0.9 || p95Frametime > targetFrametime * 1.5;
    }
    
    /**
     * Gets the age of this snapshot in milliseconds.
     * 
     * @return milliseconds since snapshot was created
     */
    public long ageMs() {
        return System.currentTimeMillis() - timestamp;
    }
}
