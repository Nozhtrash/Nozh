package dev.nozh.core.governor;

import dev.nozh.core.state.RuntimeState;
import dev.nozh.core.state.StabilityStats;

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
    private static final double UNSTABLE_SCORE_THRESHOLD = 0.4;
    private static final int FLAP_COUNT_THRESHOLD = 3;
    private static final long FLAP_WINDOW_MS = 30_000L;
    private static final double UNSTABLE_MULTIPLIER = 1.25;
    private static final double FLAP_MULTIPLIER = 1.6;
    private static final double SEVERE_FLAP_MULTIPLIER = 2.0;

    /**
     * Calculate adaptive observation window based on FPS variance.
     * 
     * @param state Current runtime state with FPS metrics
     * @return Observation window in milliseconds
     */
    public long calculateWindow(RuntimeState state) {
        double variance = calculateVariance(state);
        long baseWindow;

        if (variance > HIGH_VARIANCE_THRESHOLD) {
            // Chaotic FPS → act quickly
            baseWindow = SHORT_WINDOW;
        } else if (variance < LOW_VARIANCE_THRESHOLD) {
            // Stable FPS → be cautious, avoid unnecessary changes
            baseWindow = LONG_WINDOW;
        } else {
            // Normal variance → default behavior
            baseWindow = NORMAL_WINDOW;
        }
        return scaleWindowForStability(state, baseWindow);
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

    private long scaleWindowForStability(RuntimeState state, long baseWindow) {
        StabilityStats stats = state.stabilityStats() != null ? state.stabilityStats() : StabilityStats.defaults();
        long scaled = baseWindow;
        if (stats.score() > 0 && stats.score() <= UNSTABLE_SCORE_THRESHOLD) {
            scaled = Math.round(scaled * UNSTABLE_MULTIPLIER);
        }
        if (isFlapping(stats)) {
            double multiplier = stats.flapCount() >= 5 ? SEVERE_FLAP_MULTIPLIER : FLAP_MULTIPLIER;
            scaled = Math.round(scaled * multiplier);
        }
        return Math.max(baseWindow, scaled);
    }

    private boolean isFlapping(StabilityStats stats) {
        if (stats.flapCount() < FLAP_COUNT_THRESHOLD || stats.lastFlapTimestamp() <= 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        return now - stats.lastFlapTimestamp() <= FLAP_WINDOW_MS;
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
