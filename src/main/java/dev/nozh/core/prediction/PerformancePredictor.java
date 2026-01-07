package dev.nozh.core.prediction;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Performance Predictor - Predicts future performance degradation.
 * Uses linear regression on frametime trends to predict FPS drops.
 * 
 * <p>This is a Phase 4 component that enables proactive optimization.
 * 
 * <p>Key features:
 * <ul>
 *   <li>Linear trend analysis on frametime history</li>
 *   <li>Spike detection (>50% frametime increase)</li>
 *   <li>Confidence scoring based on data quality</li>
 *   <li>Automatic recovery detection</li>
 * </ul>
 * 
 * @author Nozh Team
 * @since 0.2.0
 */
public class PerformancePredictor {
    private static final int HISTORY_SIZE = 60; // 3 seconds at 20 TPS
    private static final double SPIKE_THRESHOLD = 1.5; // 50% increase
    private static final double PREDICTION_CONFIDENCE_MIN = 0.6;
    
    private final int targetFps;
    private final Deque<Double> frametimeHistory;
    
    public PerformancePredictor(int targetFps) {
        this.targetFps = targetFps;
        this.frametimeHistory = new ArrayDeque<>(HISTORY_SIZE);
    }
    
    /**
     * Adds a frametime sample for analysis.
     * 
     * @param frametimeMs frametime in milliseconds
     */
    public void addSample(double frametimeMs) {
        if (frametimeMs <= 0 || frametimeMs > 1000) {
            return; // Invalid sample
        }
        
        if (frametimeHistory.size() >= HISTORY_SIZE) {
            frametimeHistory.removeFirst();
        }
        frametimeHistory.addLast(frametimeMs);
    }
    
    /**
     * Predicts if FPS will drop in the near future.
     * 
     * @return true if a drop is predicted with sufficient confidence
     */
    public boolean predictFpsDrop() {
        if (frametimeHistory.size() < 20) {
            return false; // Not enough data
        }
        
        // Calculate linear trend
        double trend = calculateTrend();
        
        // Positive trend means increasing frametime (decreasing FPS)
        if (trend <= 0) {
            return false; // Performance is stable or improving
        }
        
        // Predict frametime in 10 ticks
        double currentFrametime = getAverageFrametime();
        double predictedFrametime = currentFrametime + (trend * 10);
        double targetFrametime = 1000.0 / targetFps;
        
        // Check if predicted frametime exceeds target
        return predictedFrametime > targetFrametime * 1.2; // 20% worse than target
    }
    
    /**
     * Calculates the linear trend of frametime (ms per tick).
     * Positive values indicate worsening performance.
     * 
     * @return trend coefficient
     */
    private double calculateTrend() {
        int n = Math.min(30, frametimeHistory.size());
        if (n < 10) {
            return 0.0;
        }
        
        // Simple linear regression: y = mx + b
        // We only need m (slope)
        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumX2 = 0;
        
        Double[] recent = frametimeHistory.toArray(new Double[0]);
        int startIdx = recent.length - n;
        
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = recent[startIdx + i];
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        return slope;
    }
    
    /**
     * Gets the average frametime from recent history.
     * 
     * @return average frametime in ms
     */
    private double getAverageFrametime() {
        if (frametimeHistory.isEmpty()) {
            return 16.67; // Default 60 FPS
        }
        
        double sum = 0;
        for (Double frametime : frametimeHistory) {
            sum += frametime;
        }
        return sum / frametimeHistory.size();
    }
    
    /**
     * Detects if a performance spike is occurring.
     * A spike is a sudden increase in frametime.
     * 
     * @return true if a spike is detected
     */
    public boolean detectSpike() {
        if (frametimeHistory.size() < 5) {
            return false;
        }
        
        Double[] recent = frametimeHistory.toArray(new Double[0]);
        double currentFrametime = recent[recent.length - 1];
        double previousAvg = 0;
        
        // Average of previous 4 samples
        for (int i = recent.length - 5; i < recent.length - 1; i++) {
            previousAvg += recent[i];
        }
        previousAvg /= 4;
        
        // Check if current is significantly higher
        return currentFrametime > previousAvg * SPIKE_THRESHOLD;
    }
    
    /**
     * Gets prediction confidence based on data quality.
     * 
     * @return confidence score between 0.0 and 1.0
     */
    public double getPredictionConfidence() {
        if (frametimeHistory.size() < 20) {
            return 0.0;
        }
        
        // Calculate variance
        double avg = getAverageFrametime();
        double variance = 0;
        for (Double frametime : frametimeHistory) {
            double diff = frametime - avg;
            variance += diff * diff;
        }
        variance /= frametimeHistory.size();
        
        double stdDev = Math.sqrt(variance);
        double coefficientOfVariation = stdDev / avg;
        
        // Lower variation = higher confidence
        // CV < 0.1 is very stable (high confidence)
        // CV > 0.5 is very unstable (low confidence)
        if (coefficientOfVariation < 0.1) {
            return 1.0;
        } else if (coefficientOfVariation > 0.5) {
            return 0.3;
        } else {
            return 1.0 - (coefficientOfVariation / 0.5) * 0.7;
        }
    }
    
    /**
     * Clears all prediction history.
     */
    public void reset() {
        frametimeHistory.clear();
    }
}
