package dev.nozh.core.util;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Analyzes FPS stability and frame pacing.
 * 
 * Detects:
 * - Frame time variance
 * - Micro-stutters
 * - Consistent frame pacing
 * - 1% and 0.1% lows
 * 
 * UTILITY: Frame pacing analysis
 */
public final class FpsStabilityAnalyzer {

    private final Deque<Double> recentFrametimes = new ArrayDeque<>(120);
    private final int windowSize;

    public FpsStabilityAnalyzer(int windowSize) {
        this.windowSize = windowSize;
    }

    public FpsStabilityAnalyzer() {
        this(120); // 2 seconds at 60 FPS
    }

    /**
     * Add frametime sample.
     */
    public void addSample(double frametimeMs) {
        recentFrametimes.addLast(frametimeMs);
        while (recentFrametimes.size() > windowSize) {
            recentFrametimes.removeFirst();
        }
    }

    /**
     * Calculate stability score (0.0-1.0).
     * 1.0 = perfectly stable
     * 0.0 = highly unstable
     */
    public double getStabilityScore() {
        if (recentFrametimes.size() < 10) {
            return 0.5; // Not enough data
        }

        double variance = calculateVariance();
        double coefficient = calculateCoefficientOfVariation();

        // Lower variance = higher stability
        double varianceScore = 1.0 / (1.0 + variance / 10.0);
        double coefficientScore = 1.0 / (1.0 + coefficient);

        return (varianceScore * 0.6 + coefficientScore * 0.4);
    }

    /**
     * Get 1% low FPS (99th percentile worst frames).
     */
    public double get1PercentLow() {
        if (recentFrametimes.isEmpty()) {
            return 0.0;
        }

        double[] sorted = recentFrametimes.stream()
                .mapToDouble(Double::doubleValue)
                .sorted()
                .toArray();

        int index = (int) Math.ceil(sorted.length * 0.99) - 1;
        index = Math.max(0, Math.min(index, sorted.length - 1));

        return 1000.0 / sorted[index];
    }

    /**
     * Detect micro-stutters.
     */
    public boolean hasMicroStutters() {
        if (recentFrametimes.size() < 30) {
            return false;
        }

        double avgFrametime = calculateAverage();
        int stutterCount = 0;

        for (double frametime : recentFrametimes) {
            if (frametime > avgFrametime * 1.5) {
                stutterCount++;
            }
        }

        // More than 10% of frames are stutters
        return stutterCount > recentFrametimes.size() * 0.1;
    }

    /**
     * Get frame pacing quality (0.0-1.0).
     */
    public double getFramePacingQuality() {
        if (recentFrametimes.size() < 10) {
            return 0.5;
        }

        // Check consistency of frame times
        double avgFrametime = calculateAverage();
        int consistentFrames = 0;

        for (double frametime : recentFrametimes) {
            // Within 10% of average = consistent
            if (Math.abs(frametime - avgFrametime) / avgFrametime < 0.1) {
                consistentFrames++;
            }
        }

        return (double) consistentFrames / recentFrametimes.size();
    }

    /**
     * Clear history.
     */
    public void clear() {
        recentFrametimes.clear();
    }

    private double calculateAverage() {
        return recentFrametimes.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    private double calculateVariance() {
        double avg = calculateAverage();
        double sumSquaredDiff = recentFrametimes.stream()
                .mapToDouble(ft -> Math.pow(ft - avg, 2))
                .sum();
        return sumSquaredDiff / recentFrametimes.size();
    }

    private double calculateCoefficientOfVariation() {
        double avg = calculateAverage();
        if (avg == 0) return 0;
        double stdDev = Math.sqrt(calculateVariance());
        return stdDev / avg;
    }

    /**
     * Get analysis summary.
     */
    public StabilityReport getReport() {
        return new StabilityReport(
                getStabilityScore(),
                get1PercentLow(),
                getFramePacingQuality(),
                hasMicroStutters(),
                recentFrametimes.size()
        );
    }

    public record StabilityReport(
            double stabilityScore,
            double onePercentLow,
            double framePacingQuality,
            boolean hasMicroStutters,
            int sampleCount
    ) {}
}
