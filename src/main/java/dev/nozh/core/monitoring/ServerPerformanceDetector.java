package dev.nozh.core.monitoring;

import dev.nozh.NozhConstants;

/**
 * Server Performance Detector - Monitors server-side performance from client.
 * 
 * Since NOZH is client-side only, this class estimates server performance by:
 * 1. Tracking time between server tick packets
 * 2. Measuring response latency for actions
 * 3. Detecting TPS drops from timing irregularities
 * 
 * This helps distinguish server lag from client lag, preventing
 * unnecessary client optimizations during server-side issues.
 * 
 * THREAD SAFETY NOTE:
 * Written from Netty thread (onServerTick).
 * Read from Render thread (getEstimatedTps, etc).
 * All shared state access must be synchronized.
 */
public final class ServerPerformanceDetector {

    /**
     * Server health classification based on estimated TPS.
     */
    public enum ServerHealth {
        EXCELLENT,  // TPS >= 19
        GOOD,       // TPS >= 17
        DEGRADED,   // TPS >= 14
        POOR,       // TPS >= 10
        CRITICAL    // TPS < 10
    }

    // TPS estimation constants
    private static final double IDEAL_TPS = 20.0;
    private static final double TICK_INTERVAL_MS = 50.0; // 1000ms / 20 TPS
    private static final int TPS_SAMPLE_WINDOW = 100; // Track last 100 ticks
    private static final double TPS_SMOOTHING_ALPHA = 0.1; // EMA smoothing
    
    // Health thresholds
    private static final double EXCELLENT_TPS = 19.0;
    private static final double GOOD_TPS = 17.0;
    private static final double DEGRADED_TPS = 14.0;
    private static final double POOR_TPS = 10.0;

    // Tick timing tracking
    private final long[] tickTimestamps = new long[TPS_SAMPLE_WINDOW];
    private int tickIndex = 0;
    private int tickCount = 0;
    private long lastTickTimestamp = 0;
    
    // Smoothed TPS estimate
    private double smoothedTps = IDEAL_TPS;
    private boolean initialized = false;
    
    // Lag spike detection
    private int consecutiveLagTicks = 0;
    private static final int LAG_SPIKE_THRESHOLD_MS = 100; // 2x normal tick time
    private static final int LAG_SPIKE_COUNT_THRESHOLD = 3;
    
    // Server response tracking
    private long lastActionTimestamp = 0;
    private long lastResponseTimestamp = 0;
    private double avgResponseLatencyMs = 0.0;
    private int responseCount = 0;

    /**
     * Call when a server tick is detected (e.g., from time update packet).
     * 
     * In Minecraft, the server sends time updates every tick, which we can
     * use to estimate server TPS.
     */
    public synchronized void onServerTick() {
        long now = System.currentTimeMillis();
        
        if (lastTickTimestamp > 0) {
            long delta = now - lastTickTimestamp;
            
            // Store in ring buffer
            tickTimestamps[tickIndex] = delta;
            tickIndex = (tickIndex + 1) % TPS_SAMPLE_WINDOW;
            if (tickCount < TPS_SAMPLE_WINDOW) {
                tickCount++;
            }
            
            // Detect lag spike
            if (delta > LAG_SPIKE_THRESHOLD_MS) {
                consecutiveLagTicks++;
            } else {
                consecutiveLagTicks = Math.max(0, consecutiveLagTicks - 1);
            }
            
            // Update smoothed TPS estimate
            double instantTps = 1000.0 / Math.max(delta, 1);
            instantTps = Math.min(instantTps, IDEAL_TPS); // Cap at 20 TPS
            
            if (!initialized) {
                smoothedTps = instantTps;
                initialized = true;
            } else {
                smoothedTps = (TPS_SMOOTHING_ALPHA * instantTps) + 
                             ((1.0 - TPS_SMOOTHING_ALPHA) * smoothedTps);
            }
        }
        
        lastTickTimestamp = now;
    }

    /**
     * Get estimated server TPS.
     * 
     * @return TPS estimate (0-20), or 20 if not enough data
     */
    public synchronized double getEstimatedTps() {
        if (!initialized || tickCount < 10) {
            return IDEAL_TPS; // Assume good until proven otherwise
        }
        return Math.max(0, Math.min(IDEAL_TPS, smoothedTps));
    }

    /**
     * Get calculated TPS from recent tick intervals.
     * More accurate than smoothed but more volatile.
     * 
     * @return Calculated TPS based on recent samples
     */
    public synchronized double getCalculatedTps() {
        if (tickCount < 5) {
            return IDEAL_TPS;
        }
        
        // Calculate average tick interval from recent samples
        long sum = 0;
        int count = Math.min(tickCount, 20); // Use last 20 ticks
        int idx = (tickIndex - 1 + TPS_SAMPLE_WINDOW) % TPS_SAMPLE_WINDOW;
        
        for (int i = 0; i < count; i++) {
            sum += tickTimestamps[idx];
            idx = (idx - 1 + TPS_SAMPLE_WINDOW) % TPS_SAMPLE_WINDOW;
        }
        
        double avgInterval = (double) sum / count;
        if (avgInterval <= 0) return IDEAL_TPS;
        
        return Math.min(IDEAL_TPS, 1000.0 / avgInterval);
    }

    /**
     * Get server health classification.
     */
    public synchronized ServerHealth getServerHealth() {
        double tps = getEstimatedTps();
        
        if (tps >= EXCELLENT_TPS) return ServerHealth.EXCELLENT;
        if (tps >= GOOD_TPS) return ServerHealth.GOOD;
        if (tps >= DEGRADED_TPS) return ServerHealth.DEGRADED;
        if (tps >= POOR_TPS) return ServerHealth.POOR;
        return ServerHealth.CRITICAL;
    }

    /**
     * Check if server is currently experiencing a lag spike.
     * 
     * @return true if multiple consecutive lag ticks detected
     */
    public synchronized boolean isServerLagging() {
        return consecutiveLagTicks >= LAG_SPIKE_COUNT_THRESHOLD;
    }

    /**
     * Check if the server is healthy enough for client optimizations.
     * 
     * When server is lagging, client optimizations may be useless or
     * counterproductive.
     * 
     * @return true if server is healthy enough for optimization
     */
    public synchronized boolean isServerHealthyForOptimization() {
        ServerHealth health = getServerHealth();
        return health == ServerHealth.EXCELLENT || health == ServerHealth.GOOD;
    }

    /**
     * Record when a player action is sent to server.
     * Used for response latency tracking.
     */
    public synchronized void onActionSent() {
        lastActionTimestamp = System.currentTimeMillis();
    }

    /**
     * Record when server acknowledges/responds to an action.
     */
    public synchronized void onServerResponse() {
        if (lastActionTimestamp > 0) {
            long now = System.currentTimeMillis();
            long latency = now - lastActionTimestamp;
            
            responseCount++;
            double weight = 1.0 / Math.min(responseCount, 20);
            avgResponseLatencyMs = (avgResponseLatencyMs * (1 - weight)) + (latency * weight);
            
            lastResponseTimestamp = now;
            lastActionTimestamp = 0;
        }
    }

    /**
     * Get average response latency in milliseconds.
     */
    public synchronized double getAvgResponseLatencyMs() {
        return avgResponseLatencyMs;
    }

    /**
     * Get time since last server tick.
     * 
     * @return milliseconds since last tick, or -1 if no tick received
     */
    public synchronized long getTimeSinceLastTick() {
        if (lastTickTimestamp <= 0) {
            return -1;
        }
        return System.currentTimeMillis() - lastTickTimestamp;
    }

    /**
     * Get tick interval variance - higher means less stable server.
     * 
     * @return variance of tick intervals, or 0 if not enough data
     */
    public synchronized double getTickIntervalVariance() {
        if (tickCount < 10) {
            return 0.0;
        }
        
        // Calculate mean
        double sum = 0;
        for (int i = 0; i < tickCount; i++) {
            sum += tickTimestamps[i];
        }
        double mean = sum / tickCount;
        
        // Calculate variance
        double variance = 0;
        for (int i = 0; i < tickCount; i++) {
            double diff = tickTimestamps[i] - mean;
            variance += diff * diff;
        }
        
        return variance / tickCount;
    }

    /**
     * Check if tick timing is stable.
     * 
     * @return true if variance is low (consistent tick rate)
     */
    public synchronized boolean isTickTimingStable() {
        double variance = getTickIntervalVariance();
        // Variance > 500 means high instability (std dev > ~22ms)
        return variance < 500;
    }

    /**
     * Reset all tracking data.
     * Call when changing servers or on disconnect.
     */
    public synchronized void reset() {
        tickIndex = 0;
        tickCount = 0;
        lastTickTimestamp = 0;
        smoothedTps = IDEAL_TPS;
        initialized = false;
        consecutiveLagTicks = 0;
        lastActionTimestamp = 0;
        lastResponseTimestamp = 0;
        avgResponseLatencyMs = 0.0;
        responseCount = 0;
    }

    /**
     * Get comprehensive server status.
     */
    public synchronized ServerStatus getStatus() {
        return new ServerStatus(
            getEstimatedTps(),
            getCalculatedTps(),
            getServerHealth(),
            isServerLagging(),
            isServerHealthyForOptimization(),
            getAvgResponseLatencyMs(),
            getTimeSinceLastTick(),
            isTickTimingStable(),
            tickCount
        );
    }

    /**
     * Server status snapshot.
     */
    public record ServerStatus(
        double estimatedTps,
        double calculatedTps,
        ServerHealth health,
        boolean lagging,
        boolean healthyForOptimization,
        double avgResponseLatencyMs,
        long timeSinceLastTickMs,
        boolean tickTimingStable,
        int sampleCount
    ) {
        public String summary() {
            return String.format(
                "TPS: %.1f (%s) | Lag: %s | Latency: %.0fms | Stable: %s",
                estimatedTps,
                health.name(),
                lagging ? "YES" : "No",
                avgResponseLatencyMs,
                tickTimingStable ? "Yes" : "NO"
            );
        }
        
        public boolean hasEnoughData() {
            return sampleCount >= 20;
        }
    }
}
