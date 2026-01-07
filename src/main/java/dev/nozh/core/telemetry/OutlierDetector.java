package dev.nozh.core.telemetry;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Statistical outlier detector using 3-sigma rule.
 * 
 * Identifies and filters anomalous samples (loading spikes,
 * GC pauses, disk I/O stalls) that would skew telemetry.
 * 
 * Uses sliding window for real-time mean/stddev calculation.
 * Outliers are defined as values > mean + 3×σ.
 * 
 * TASK 2: Telemetry precision - removes ~5-10% outliers
 */
public final class OutlierDetector {

    private static final int DEFAULT_WINDOW_SIZE = 100;
    private static final double SIGMA_THRESHOLD = 3.0; // 3-sigma rule

    private final Deque<Double> window;
    private final int windowSize;
    private double sum = 0.0;
    private double sumSquares = 0.0;

    public OutlierDetector() {
        this(DEFAULT_WINDOW_SIZE);
    }

    public OutlierDetector(int windowSize) {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("Window size must be positive");
        }
        this.windowSize = windowSize;
        this.window = new ArrayDeque<>(windowSize);
    }

    /**
     * Add sample and check if it's an outlier.
     * Returns true if sample is an outlier (should be discarded).
     */
    public boolean isOutlier(double value) {
        if (window.size() < 10) {
            // Not enough data - accept all samples
            addSample(value);
            return false;
        }

        double mean = sum / window.size();
        double variance = (sumSquares / window.size()) - (mean * mean);
        double stddev = Math.sqrt(Math.max(0, variance));

        double threshold = mean + SIGMA_THRESHOLD * stddev;
        boolean outlier = value > threshold;

        // Always add to window (even outliers, for future stats)
        addSample(value);

        return outlier;
    }

    /**
     * Add sample to sliding window.
     */
    private void addSample(double value) {
        if (window.size() >= windowSize) {
            double removed = window.pollFirst();
            sum -= removed;
            sumSquares -= removed * removed;
        }

        window.offer(value);
        sum += value;
        sumSquares += value * value;
    }

    /**
     * Get current mean.
     */
    public double getMean() {
        return window.isEmpty() ? 0.0 : sum / window.size();
    }

    /**
     * Get current standard deviation.
     */
    public double getStdDev() {
        if (window.size() < 2) {
            return 0.0;
        }
        double mean = getMean();
        double variance = (sumSquares / window.size()) - (mean * mean);
        return Math.sqrt(Math.max(0, variance));
    }

    /**
     * Get outlier threshold (mean + 3σ).
     */
    public double getThreshold() {
        return getMean() + SIGMA_THRESHOLD * getStdDev();
    }

    /**
     * Reset detector state.
     */
    public void reset() {
        window.clear();
        sum = 0.0;
        sumSquares = 0.0;
    }
}
