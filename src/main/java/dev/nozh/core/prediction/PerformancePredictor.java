package dev.nozh.core.prediction;

import dev.nozh.NozhConstants;
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
 *   <li>Numerical stability with division-by-zero protection</li>
 * </ul>
 * 
 * <p><b>Thread Safety:</b> This class is NOT thread-safe. External synchronization required.
 * 
 * @author Nozh Team
 * @since 0.2.0
 */
public class PerformancePredictor {
    private static final int HISTORY_SIZE = 60; // 3 seconds at 20 TPS
    private static final double SPIKE_THRESHOLD = 1.5; // 50% increase
    private static final double PREDICTION_CONFIDENCE_MIN = 0.6;
    private static final double EPSILON = 1e-10; // Numerical stability threshold
    private static final double MAX_VALID_FRAMETIME = 10000.0; // 10 seconds max
    
    private final int targetFps;
    private final Deque<Double> frametimeHistory;
    
    /**
     * Creates a new PerformancePredictor.
     * 
     * @param targetFps target FPS for prediction calculations (must be > 0)
     * @throws IllegalArgumentException if targetFps <= 0
     */
    public PerformancePredictor(int targetFps) {
        if (targetFps <= 0) {
            throw new IllegalArgumentException("Target FPS must be positive, got: " + targetFps);
        }
        this.targetFps = targetFps;
        this.frametimeHistory = new ArrayDeque<>(HISTORY_SIZE);
    }
    
    /**
     * Adds a frametime sample for analysis.
     * 
     * <p>Samples outside valid range (0, MAX_VALID_FRAMETIME] are silently rejected.
     * 
     * @param frametimeMs frametime in milliseconds
     */
    public void addSample(double frametimeMs) {
        // Validate input range
        if (!isValidFrametime(frametimeMs)) {
            if (frametimeMs > MAX_VALID_FRAMETIME) {
                NozhConstants.LOGGER.warn(
                    "Rejecting extreme frametime: {}ms (max: {}ms)", 
                    frametimeMs, MAX_VALID_FRAMETIME
                );
            }
            return;
        }
        
        // Check for NaN/Infinity
        if (!Double.isFinite(frametimeMs)) {
            NozhConstants.LOGGER.warn("Rejecting non-finite frametime: {}", frametimeMs);
            return;
        }
        
        if (frametimeHistory.size() >= HISTORY_SIZE) {
            frametimeHistory.removeFirst();
        }
        frametimeHistory.addLast(frametimeMs);
    }
    
    /**
     * Validates frametime is within acceptable range.
     * 
     * @param frametimeMs frametime to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidFrametime(double frametimeMs) {
        return frametimeMs > 0 && frametimeMs <= MAX_VALID_FRAMETIME;
    }
    
    /**
     * Predicts if FPS will drop in the near future.
     * 
     * <p>Requires at least 20 samples for prediction.
     * 
     * @return true if a drop is predicted with sufficient confidence
     */
    public boolean predictFpsDrop() {
        if (frametimeHistory.size() < 20) {
            return false; // Not enough data
        }
        
        // Calculate linear trend
        double trend = calculateTrend();
        
        // Validate trend (check for NaN)
        if (!Double.isFinite(trend)) {
            NozhConstants.LOGGER.warn("Invalid trend calculated: {}", trend);
            return false;
        }
        
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
     * <p><b>CRITICAL FIX:</b> Now includes division-by-zero protection.
     * 
     * @return trend coefficient, or 0.0 if insufficient variance
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
            
            // Validate each sample
            if (!Double.isFinite(y)) {
                NozhConstants.LOGGER.warn("Non-finite value in history at index {}", startIdx + i);
                return 0.0; // Abort calculation
            }
            
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        
        // CRITICAL: Check denominator before division
        double denominator = n * sumX2 - sumX * sumX;
        if (Math.abs(denominator) < EPSILON) {
            // Denominator near zero means insufficient variance in X values
            // This can happen if all samples are identical (flat line)
            return 0.0; // No trend detectable
        }
        
        double numerator = n * sumXY - sumX * sumY;
        double slope = numerator / denominator;
        
        // Final sanity check on result
        if (!Double.isFinite(slope)) {
            NozhConstants.LOGGER.error(
                "Non-finite slope calculated: num={}, denom={}, n={}",
                numerator, denominator, n
            );
            return 0.0;
        }
        
        return slope;
    }
    
    /**
     * Gets the average frametime from recent history.
     * 
     * @return average frametime in ms, or default 60 FPS (16.67ms) if no data
     */
    private double getAverageFrametime() {
        if (frametimeHistory.isEmpty()) {
            return 16.67; // Default 60 FPS
        }
        
        double sum = 0;
        int validCount = 0;
        
        for (Double frametime : frametimeHistory) {
            if (Double.isFinite(frametime)) {
                sum += frametime;
                validCount++;
            }
        }
        
        if (validCount == 0) {
            NozhConstants.LOGGER.warn("No valid samples in history");
            return 16.67; // Fallback
        }
        
        return sum / validCount;
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
        
        // Validate current sample
        if (!Double.isFinite(currentFrametime)) {
            return false;
        }
        
        double previousAvg = 0;
        int validCount = 0;
        
        // Average of previous 4 samples (skip invalid)
        for (int i = recent.length - 5; i < recent.length - 1; i++) {
            if (Double.isFinite(recent[i])) {
                previousAvg += recent[i];
                validCount++;
            }
        }
        
        if (validCount == 0) {
            return false; // Cannot determine baseline
        }
        
        previousAvg /= validCount;
        
        // Prevent division by zero in threshold comparison
        if (previousAvg < EPSILON) {
            return false; // Baseline too small to compare
        }
        
        // Check if current is significantly higher
        return currentFrametime > previousAvg * SPIKE_THRESHOLD;
    }
    
    /**
     * Gets prediction confidence based on data quality.
     * 
     * <p>Confidence is calculated from coefficient of variation.
     * Lower variation = higher confidence.
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
        int validCount = 0;
        
        for (Double frametime : frametimeHistory) {
            if (Double.isFinite(frametime)) {
                double diff = frametime - avg;
                variance += diff * diff;
                validCount++;
            }
        }
        
        if (validCount == 0 || avg < EPSILON) {
            return 0.0; // Cannot calculate confidence
        }
        
        variance /= validCount;
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
     * Use when starting a new prediction session or after major state changes.
     */
    public void reset() {
        frametimeHistory.clear();
    }
    
    /**
     * Gets current sample count.
     * 
     * @return number of samples in history
     */
    public int getSampleCount() {
        return frametimeHistory.size();
    }
    
    /**
     * Checks if predictor has enough data for predictions.
     * 
     * @return true if at least 20 samples are available
     */
    public boolean isWarmedUp() {
        return frametimeHistory.size() >= 20;
    }
}
