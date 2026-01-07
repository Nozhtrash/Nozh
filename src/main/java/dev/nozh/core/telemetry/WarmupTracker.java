package dev.nozh.core.telemetry;

/**
 * Tracks warmup phase to separate unstable early telemetry.
 * 
 * JVM warmup (JIT compilation, class loading) and chunk
 * loading causes frametime instability in first 30-60s.
 * This tracker marks samples as 'warmup' vs 'stable'.
 * 
 * Warmup is complete after:
 * - Minimum time elapsed (30s)
 * - Frametime variance stabilizes
 * - Minimum stable samples collected
 * 
 * TASK 2: Telemetry precision - avoids early noise
 */
public final class WarmupTracker {

    private static final long WARMUP_MIN_DURATION_MS = 30_000; // 30s
    private static final int WARMUP_MIN_SAMPLES = 300; // ~15s @ 20 TPS
    private static final double STABILITY_VARIANCE_THRESHOLD = 4.0; // ms²

    private final long startTime;
    private int sampleCount = 0;
    private boolean warmupComplete = false;

    private double recentSum = 0.0;
    private double recentSumSquares = 0.0;
    private int recentCount = 0;
    private static final int RECENT_WINDOW = 60; // 3s window

    public WarmupTracker() {
        this.startTime = System.currentTimeMillis();
    }

    /**
     * Record telemetry sample.
     * Call this for every telemetry sample added.
     */
    public void recordSample(double frametimeMs) {
        sampleCount++;

        // Track recent variance
        recentSum += frametimeMs;
        recentSumSquares += frametimeMs * frametimeMs;
        recentCount++;

        if (recentCount > RECENT_WINDOW) {
            // Shift window (simple approach - reset)
            recentSum = frametimeMs * RECENT_WINDOW;
            recentSumSquares = frametimeMs * frametimeMs * RECENT_WINDOW;
            recentCount = RECENT_WINDOW;
        }

        // Check if warmup complete
        if (!warmupComplete) {
            warmupComplete = checkWarmupComplete();
        }
    }

    /**
     * Check if warmup phase is complete.
     */
    private boolean checkWarmupComplete() {
        long elapsed = System.currentTimeMillis() - startTime;

        // Must meet ALL criteria:
        if (elapsed < WARMUP_MIN_DURATION_MS) {
            return false;
        }
        if (sampleCount < WARMUP_MIN_SAMPLES) {
            return false;
        }
        if (recentCount < RECENT_WINDOW) {
            return false;
        }

        // Check variance stability
        double mean = recentSum / recentCount;
        double variance = (recentSumSquares / recentCount) - (mean * mean);

        return variance < STABILITY_VARIANCE_THRESHOLD;
    }

    /**
     * Check if system is still in warmup phase.
     */
    public boolean isWarmup() {
        return !warmupComplete;
    }

    /**
     * Check if telemetry is stable (warmup complete).
     */
    public boolean isStable() {
        return warmupComplete;
    }

    /**
     * Get current variance of recent samples.
     */
    public double getRecentVariance() {
        if (recentCount < 2) {
            return 0.0;
        }
        double mean = recentSum / recentCount;
        return (recentSumSquares / recentCount) - (mean * mean);
    }

    /**
     * Get elapsed time since tracking started (ms).
     */
    public long getElapsedMs() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * Get number of samples recorded.
     */
    public int getSampleCount() {
        return sampleCount;
    }
}
