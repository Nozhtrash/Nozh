package dev.nozh.core.governor;

import dev.nozh.core.state.RuntimeState;

/**
 * Adaptive observation window calculator.
 * 
 * Adjusts the governor's observation window based on FPS stability.
 * - Chaotic FPS (high variance) → shorter window (10s) for quick response
 * - Stable FPS (low variance) → longer window (30s) to avoid overreaction
 * - Normal variance → default window (20s)
 */
public final class AdaptiveWindowCalculator {

    // Variance thresholds (ms²)
    private static final double LOW_VARIANCE_THRESHOLD = 5.0; // Very stable
    private static final double HIGH_VARIANCE_THRESHOLD = 25.0; // Chaotic

    // Window durations (ms)
    private static final long SHORT_WINDOW = 10_000; // 10s - fast response
    private static final long NORMAL_WINDOW = 20_000; // 20s - balanced
    private static final long LONG_WINDOW = 30_000; // 30s - cautious

    /**
     * Calculate adaptive observation window based on FPS variance.
     * 
     * @param state Current runtime state with FPS metrics
     * @return Observation window in milliseconds
     */
    public long calculateWindow(RuntimeState state) {
        double variance = calculateVariance(state);

        if (variance > HIGH_VARIANCE_THRESHOLD) {
            // Chaotic FPS → act quickly
            return SHORT_WINDOW;
        } else if (variance < LOW_VARIANCE_THRESHOLD) {
            // Stable FPS → be cautious, avoid unnecessary changes
            return LONG_WINDOW;
        } else {
            // Normal variance → default behavior
            return NORMAL_WINDOW;
        }
    }

    /**
     * Calculate FPS variance from runtime state.
     * Uses simple variance formula: Var = E[X²] - E[X]²
     */
    private double calculateVariance(RuntimeState state) {
        double avgMs = state.avgFrametimeMs();
        double p95Ms = state.p95FrametimeMs();

        // No data yet
        if (avgMs < 0 || p95Ms < 0) {
            return 0.0; // Default to normal window
        }

        // Simple variance approximation using P95 spread
        // P95 much higher than avg → high variance
        double spread = p95Ms - avgMs;
        return spread * spread; // Variance ≈ spread²
    }

    /**
     * Get human-readable description of current window mode.
     */
    public String getWindowMode(long windowMs) {
        if (windowMs <= SHORT_WINDOW) {
            return "FAST (chaotic FPS)";
        } else if (windowMs >= LONG_WINDOW) {
            return "CAUTIOUS (stable FPS)";
        } else {
            return "NORMAL";
        }
    }
}
