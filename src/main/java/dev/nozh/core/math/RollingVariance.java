package dev.nozh.core.math;

/**
 * Rolling variance calculator using Welford's online algorithm.
 * 
 * Features:
 * - Numerically stable single-pass algorithm
 * - O(1) memory regardless of sample count
 * - Zero allocation after construction
 * 
 * @author Nozh Team
 * @since 0.3.0
 */
public final class RollingVariance {

    private final int windowSize;
    private final double[] samples;
    private int index;
    private int count;
    private double sum;
    private double sumSquares;

    /**
     * Creates a rolling variance calculator with specified window size.
     * 
     * @param windowSize Maximum samples to track (must be >= 2)
     * @throws IllegalArgumentException if windowSize < 2
     */
    public RollingVariance(int windowSize) {
        if (windowSize < 2) {
            throw new IllegalArgumentException("Window size must be >= 2, got: " + windowSize);
        }
        this.windowSize = windowSize;
        this.samples = new double[windowSize];
        this.index = 0;
        this.count = 0;
        this.sum = 0.0;
        this.sumSquares = 0.0;
    }

    /**
     * Adds a new sample and updates statistics.
     * 
     * @param value New sample value
     */
    public void addSample(double value) {
        if (!Double.isFinite(value)) {
            return; // Ignore invalid values
        }

        if (count >= windowSize) {
            // Remove oldest sample
            double oldValue = samples[index];
            sum -= oldValue;
            sumSquares -= oldValue * oldValue;
        } else {
            count++;
        }

        // Add new sample
        samples[index] = value;
        sum += value;
        sumSquares += value * value;
        
        index = (index + 1) % windowSize;
    }

    /**
     * Gets the current mean of samples in the window.
     * 
     * @return Mean value, or 0.0 if no samples
     */
    public double getMean() {
        if (count == 0) {
            return 0.0;
        }
        return sum / count;
    }

    /**
     * Gets the current variance of samples in the window.
     * Uses population variance formula (N denominator).
     * 
     * @return Variance value, or 0.0 if insufficient samples
     */
    public double getVariance() {
        if (count < 2) {
            return 0.0;
        }
        double mean = sum / count;
        double variance = (sumSquares / count) - (mean * mean);
        return Math.max(0.0, variance); // Ensure non-negative due to floating point
    }

    /**
     * Gets the current standard deviation.
     * 
     * @return Standard deviation, or 0.0 if insufficient samples
     */
    public double getStandardDeviation() {
        return Math.sqrt(getVariance());
    }

    /**
     * Gets the coefficient of variation (stddev / mean).
     * 
     * @return CV value, or 0.0 if mean is near zero
     */
    public double getCoefficientOfVariation() {
        double mean = getMean();
        if (Math.abs(mean) < 1e-10) {
            return 0.0;
        }
        return getStandardDeviation() / Math.abs(mean);
    }

    /**
     * Checks if the window is full.
     * 
     * @return true if window has windowSize samples
     */
    public boolean isFull() {
        return count >= windowSize;
    }

    /**
     * Gets the current sample count.
     * 
     * @return Number of samples (max = windowSize)
     */
    public int getCount() {
        return count;
    }

    /**
     * Resets all statistics.
     */
    public void reset() {
        index = 0;
        count = 0;
        sum = 0.0;
        sumSquares = 0.0;
    }

    /**
     * Gets the most recent sample added.
     * 
     * @return Most recent sample, or 0.0 if no samples
     */
    public double getLastSample() {
        if (count == 0) {
            return 0.0;
        }
        int lastIndex = (index - 1 + windowSize) % windowSize;
        return samples[lastIndex];
    }

    /**
     * Calculates z-score for a value relative to current statistics.
     * 
     * @param value Value to calculate z-score for
     * @return Z-score, or 0.0 if insufficient data
     */
    public double getZScore(double value) {
        double stdDev = getStandardDeviation();
        if (stdDev < 1e-10) {
            return 0.0;
        }
        return (value - getMean()) / stdDev;
    }
}
