package dev.nozh.core.intelligence;

import dev.nozh.core.monitoring.NetworkLatencyTracker;

/**
 * Intelligent detector to classify the source of lag.
 * <p>
 * Prevents the optimizer from degrading visual quality when the issue is
 * network-related or server-side, rather than client rendering.
 */
public class AnomalyDetector {
    private final NetworkLatencyTracker latencyTracker;

    // Thresholds
    private static final double RENDER_LAG_THRESHOLD_MS = 35.0; // ~28 FPS
    private static final long NETWORK_LAG_THRESHOLD_MS = 250; // 250ms ping
    private static final double SYSTEM_STUTTER_THRESHOLD_MS = 100.0; // 100ms frame catch-up

    public enum LagType {
        NONE,
        RENDER_LAG, // GPU/CPU render bound -> OPTIMIZE
        NETWORK_LAG, // Bad ping/packet loss -> IGNORE
        SERVER_LAG, // Low TPS -> ADJUST TICK BEHAVIOR
        SYSTEM_GC // GC spike -> IGNORE (transient)
    }

    public AnomalyDetector(NetworkLatencyTracker latencyTracker) {
        this.latencyTracker = latencyTracker;
    }

    /**
     * Analyze current state to determine the root cause of poor performance.
     * 
     * @param lastFrameTimeMs Time taken to render the last frame
     * @return The classified type of lag
     */
    public LagType analyze(double lastFrameTimeMs) {
        // 1. Check if frame time is actually bad
        if (lastFrameTimeMs < RENDER_LAG_THRESHOLD_MS) {
            return LagType.NONE;
        }

        // 2. Check for Network Lag
        // If ping is spiking simultaneously, it's likely a connection issue causing
        // entity update stutters
        // rather than raw rendering load.
        if (latencyTracker.getAveragePingMs() > NETWORK_LAG_THRESHOLD_MS) {
            // However, we must be careful: High Ping doesn't ALWAYS cause low FPS.
            // But if we have High Ping AND low FPS, optimizing chunks might not help.
            return LagType.NETWORK_LAG;
        }

        // 3. Check for massive GC spikes (System Lag)
        // If the frame time is HUGE (e.g., >100ms) but the previous frames were fine,
        // it's likely a GC pause or IO stutter, not a sustained load.
        // (Simple heuristic implementation; a real GC monitor would be better)
        if (lastFrameTimeMs > SYSTEM_STUTTER_THRESHOLD_MS) {
            return LagType.SYSTEM_GC;
        }

        // 4. If ping is fine and it's not a single massive spike, it's likely true
        // Render Lag.
        return LagType.RENDER_LAG;
    }
}
