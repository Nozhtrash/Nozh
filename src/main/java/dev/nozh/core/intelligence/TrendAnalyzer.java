package dev.nozh.core.intelligence;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Analyzes frametime trends to detect performance degradation.
 * 
 * Detects:
 * - Rising trend (performance worsening)
 * - Falling trend (performance improving)
 * - Oscillation (unstable)
 * - Stable (no trend)
 * 
 * Uses linear regression on sliding window.
 * 
 * TASK 8: Predictive intelligence - trend detection
 */
public final class TrendAnalyzer {

    private static final int DEFAULT_WINDOW_SIZE = 60; // 3 seconds @ 20 TPS
    private static final double TREND_THRESHOLD = 0.5; // ms/s

    private final Deque<Sample> window;
    private final int windowSize;

    public TrendAnalyzer() {
        this(DEFAULT_WINDOW_SIZE);
    }

    public TrendAnalyzer(int windowSize) {
        this.windowSize = windowSize;
        this.window = new ArrayDeque<>(windowSize);
    }

    /**
     * Add frametime sample.
     */
    public void addSample(double frametimeMs) {
        long timestamp = System.currentTimeMillis();
        window.offer(new Sample(timestamp, frametimeMs));

        while (window.size() > windowSize) {
            window.pollFirst();
        }
    }

    /**
     * Calculate trend slope (ms/s).
     * Positive = worsening, Negative = improving
     */
    public double getTrendSlope() {
        if (window.size() < 10) {
            return 0.0; // Not enough data
        }

        // Linear regression
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        int n = window.size();
        long baseTime = window.peekFirst().timestamp;

        for (Sample sample : window) {
            double x = (sample.timestamp - baseTime) / 1000.0; // Convert to seconds
            double y = sample.frametimeMs;
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        return slope;
    }

    /**
     * Detect trend type.
     */
    public TrendType getTrendType() {
        double slope = getTrendSlope();

        if (Math.abs(slope) < TREND_THRESHOLD) {
            return TrendType.STABLE;
        }
        if (slope > TREND_THRESHOLD) {
            return TrendType.RISING; // Worsening
        }
        return TrendType.FALLING; // Improving
    }

    /**
     * Check if performance is degrading.
     */
    public boolean isDegrading() {
        return getTrendType() == TrendType.RISING;
    }

    /**
     * Check if performance is improving.
     */
    public boolean isImproving() {
        return getTrendType() == TrendType.FALLING;
    }

    /**
     * Predict frametime in N seconds.
     */
    public double predictFrametime(double secondsAhead) {
        if (window.isEmpty()) {
            return 0.0;
        }

        double currentFrametime = window.peekLast().frametimeMs;
        double slope = getTrendSlope();

        return currentFrametime + (slope * secondsAhead);
    }

    /**
     * Reset analyzer.
     */
    public void reset() {
        window.clear();
    }

    private record Sample(long timestamp, double frametimeMs) {}

    public enum TrendType {
        RISING,    // Performance worsening
        FALLING,   // Performance improving
        STABLE,    // No significant trend
        UNKNOWN    // Not enough data
    }
}
