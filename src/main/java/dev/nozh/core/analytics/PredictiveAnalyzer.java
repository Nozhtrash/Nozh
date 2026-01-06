package dev.nozh.core.analytics;

import dev.nozh.core.monitor.FrameTimeSnapshot;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * PRIORITY 3: Predictive spike algorithm.
 * 
 * Uses simple linear regression to predict FPS drops before they happen.
 * Allows proactive optimizations instead of reactive ones.
 * 
 * Algorithm: Rolling window (60 samples) + trend detection.
 */
public final class PredictiveAnalyzer {

    private static final int WINDOW_SIZE = 60; // 3 seconds at 20 TPS
    private static final double SPIKE_THRESHOLD = 0.15; // 15% drop predicted
    
    private final Queue<DataPoint> history = new LinkedList<>();
    private long lastPredictionTime = 0;
    private static final long PREDICTION_COOLDOWN_MS = 5000; // 5s between predictions

    /**
     * Add a new data point.
     */
    public void addSample(FrameTimeSnapshot snapshot) {
        long now = System.currentTimeMillis();
        double fps = snapshot.avgFps();
        double p95 = snapshot.p95FrameTimeMs();
        
        DataPoint point = new DataPoint(now, fps, p95);
        history.offer(point);

        // Keep window size
        while (history.size() > WINDOW_SIZE) {
            history.poll();
        }
    }

    /**
     * Predict if a spike is incoming.
     * Returns confidence (0.0 to 1.0), or 0 if no spike predicted.
     */
    public double predictSpike() {
        long now = System.currentTimeMillis();
        
        // Cooldown to avoid spam
        if (now - lastPredictionTime < PREDICTION_COOLDOWN_MS) {
            return 0.0;
        }

        // Need at least 30 samples
        if (history.size() < 30) {
            return 0.0;
        }

        // Convert to arrays
        List<DataPoint> points = new ArrayList<>(history);
        int n = points.size();

        // Calculate FPS trend (linear regression)
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < n; i++) {
            double x = i; // time index
            double y = points.get(i).fps;
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        // Linear regression: y = mx + b
        double m = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        
        // Negative slope = FPS declining
        if (m >= 0) {
            return 0.0; // FPS stable or improving
        }

        // Calculate how severe the decline is
        double currentFps = points.get(n - 1).fps;
        double predictedFps = currentFps + (m * 10); // 10 samples ahead
        double dropRatio = (currentFps - predictedFps) / currentFps;

        if (dropRatio < SPIKE_THRESHOLD) {
            return 0.0; // Drop too small to care
        }

        // Calculate confidence based on trend consistency
        double confidence = Math.min(dropRatio / 0.3, 1.0); // Max at 30% drop

        // Check P95 trend too
        double p95Trend = calculateP95Trend(points);
        if (p95Trend > 1.2) { // P95 increasing
            confidence *= 1.2; // Boost confidence
        }

        lastPredictionTime = now;
        return Math.min(confidence, 1.0);
    }

    private double calculateP95Trend(List<DataPoint> points) {
        if (points.size() < 10) return 1.0;

        // Compare recent P95 vs older P95
        double recentP95 = 0;
        double oldP95 = 0;
        int mid = points.size() / 2;

        for (int i = 0; i < mid; i++) {
            oldP95 += points.get(i).p95;
        }
        oldP95 /= mid;

        for (int i = mid; i < points.size(); i++) {
            recentP95 += points.get(i).p95;
        }
        recentP95 /= (points.size() - mid);

        return recentP95 / oldP95;
    }

    /**
     * Get prediction report for display.
     */
    public PredictionReport getReport() {
        double spikeConfidence = predictSpike();
        
        if (spikeConfidence == 0.0) {
            return new PredictionReport(false, 0.0, "Stable");
        }

        String severity;
        if (spikeConfidence < 0.3) {
            severity = "Minor spike predicted";
        } else if (spikeConfidence < 0.6) {
            severity = "Moderate spike predicted";
        } else {
            severity = "Major spike predicted";
        }

        return new PredictionReport(true, spikeConfidence, severity);
    }

    /**
     * Get current trend direction.
     */
    public TrendDirection getTrend() {
        if (history.size() < 10) {
            return TrendDirection.UNKNOWN;
        }

        List<DataPoint> points = new ArrayList<>(history);
        int n = points.size();

        // Simple slope calculation
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = points.get(i).fps;
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double m = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);

        if (Math.abs(m) < 0.1) return TrendDirection.STABLE;
        if (m > 0) return TrendDirection.IMPROVING;
        return TrendDirection.DECLINING;
    }

    /**
     * Clear history (for new sessions).
     */
    public void reset() {
        history.clear();
        lastPredictionTime = 0;
    }

    /**
     * Get sample count.
     */
    public int getSampleCount() {
        return history.size();
    }

    /**
     * Data point record.
     */
    private record DataPoint(long timestamp, double fps, double p95) {}

    /**
     * Prediction report.
     */
    public record PredictionReport(
        boolean spikeIncoming,
        double confidence,
        String description
    ) {}

    /**
     * Trend direction.
     */
    public enum TrendDirection {
        IMPROVING,
        STABLE,
        DECLINING,
        UNKNOWN
    }
}
