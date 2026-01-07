package dev.nozh.core.prediction;

import dev.nozh.core.state.PerformanceSnapshot;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Predicts future performance trends using historical data and simple regression.
 * Helps the governor make proactive decisions before performance degrades.
 * 
 * <p>Uses linear regression on recent performance snapshots to predict:
 * <ul>
 *   <li>Next frametime values (P95)</li>
 *   <li>Potential performance spikes</li>
 *   <li>Auto-recovery trends</li>
 * </ul>
 * 
 * <p>Thread-safe and designed for real-time operation with minimal overhead.
 * 
 * @author Nozh Team
 * @since 0.2.0
 */
public class PerformancePredictor {
    private static final int HISTORY_SIZE = 60; // 60 seconds of data
    private static final int PREDICTION_HORIZON = 10; // Predict 10 seconds ahead
    private static final double SPIKE_THRESHOLD = 1.5; // 50% increase considered a spike
    private static final int MIN_SAMPLES_FOR_PREDICTION = 5;
    private static final int REGRESSION_WINDOW = 20; // Use last 20 snapshots for regression
    private static final double MAX_PREDICTION_DEVIATION = 0.3; // 30% max deviation
    
    private final Deque<PerformanceSnapshot> history;
    private double lastPrediction;
    private long lastPredictionTime;
    
    public PerformancePredictor() {
        this.history = new ConcurrentLinkedDeque<>();
        this.lastPrediction = 0.0;
        this.lastPredictionTime = 0;
    }
    
    /**
     * Adds a new snapshot to the prediction history.
     * Automatically trims history to maintain fixed window size.
     * 
     * @param snapshot the performance snapshot to add
     */
    public void addSnapshot(PerformanceSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        
        history.addLast(snapshot);
        
        while (history.size() > HISTORY_SIZE) {
            history.removeFirst();
        }
    }
    
    /**
     * Predicts the next frametime using linear regression on recent data.
     * 
     * <p>Uses least-squares regression on the most recent samples to
     * extrapolate future frametime. Predictions are clamped to prevent
     * unrealistic values.
     * 
     * @return Predicted P95 frametime in milliseconds, or current P95 if insufficient data
     */
    public double predictNextFrametime() {
        if (history.size() < MIN_SAMPLES_FOR_PREDICTION) {
            // Not enough data for prediction
            return history.isEmpty() ? 16.67 : history.getLast().p95Frametime;
        }
        
        long now = System.currentTimeMillis();
        if (now - lastPredictionTime < 1000) {
            // Cache predictions for 1 second to reduce computation
            return lastPrediction;
        }
        
        // Simple linear regression on recent data
        List<PerformanceSnapshot> recentSnapshots = new ArrayList<>(history);
        int n = Math.min(recentSnapshots.size(), REGRESSION_WINDOW);
        
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = recentSnapshots.get(recentSnapshots.size() - n + i).p95Frametime;
            
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        
        // Calculate slope and intercept using least squares
        double denominator = (n * sumX2 - sumX * sumX);
        double slope = denominator != 0 ? (n * sumXY - sumX * sumY) / denominator : 0;
        double intercept = (sumY - slope * sumX) / n;
        
        // Predict for next time step
        double prediction = slope * n + intercept;
        
        // Clamp prediction to reasonable bounds (prevent wild predictions)
        double currentP95 = recentSnapshots.get(recentSnapshots.size() - 1).p95Frametime;
        double maxDeviation = currentP95 * MAX_PREDICTION_DEVIATION;
        prediction = Math.max(currentP95 - maxDeviation, 
                             Math.min(currentP95 + maxDeviation, prediction));
        
        // Ensure prediction is positive
        prediction = Math.max(1.0, prediction);
        
        lastPrediction = prediction;
        lastPredictionTime = now;
        
        return prediction;
    }
    
    /**
     * Detects if a performance spike is likely to occur soon.
     * 
     * <p>A spike is predicted when the forecasted frametime exceeds
     * the current frametime by more than {@link #SPIKE_THRESHOLD}.
     * 
     * @return true if a spike is predicted within the next few seconds
     */
    public boolean isPredictingSpike() {
        if (history.size() < 10) {
            return false;
        }
        
        double prediction = predictNextFrametime();
        double currentP95 = history.getLast().p95Frametime;
        
        return prediction > currentP95 * SPIKE_THRESHOLD;
    }
    
    /**
     * Determines if the system should wait for auto-recovery.
     * 
     * <p>Auto-recovery is detected when recent performance shows
     * a clear improving trend (>10% improvement) without intervention.
     * This prevents unnecessary actions when the system is already
     * recovering naturally.
     * 
     * @return true if performance is expected to improve without intervention
     */
    public boolean shouldWaitForRecovery() {
        if (history.size() < 15) {
            return false;
        }
        
        // Check if performance is already improving
        List<PerformanceSnapshot> recent = new ArrayList<>(history).subList(
            Math.max(0, history.size() - 10), history.size());
        
        if (recent.size() < 5) {
            return false;
        }
        
        // Calculate trend: is frametime decreasing?
        double firstHalfAvg = recent.subList(0, recent.size() / 2).stream()
            .mapToDouble(s -> s.p95Frametime)
            .average()
            .orElse(0.0);
            
        double secondHalfAvg = recent.subList(recent.size() / 2, recent.size()).stream()
            .mapToDouble(s -> s.p95Frametime)
            .average()
            .orElse(0.0);
        
        // If recent performance is improving by >10%, wait
        return secondHalfAvg < firstHalfAvg * 0.9;
    }
    
    /**
     * Gets the confidence level of the current prediction.
     * 
     * <p>Confidence is based on the stability (low variance) of recent data.
     * Lower variance indicates more predictable behavior and higher confidence.
     * 
     * <p>Confidence levels:
     * <ul>
     *   <li>0.9-1.0: Very stable, highly predictable</li>
     *   <li>0.7-0.9: Stable, good predictions</li>
     *   <li>0.5-0.7: Moderate stability</li>
     *   <li>0.0-0.5: High variance, low confidence</li>
     * </ul>
     * 
     * @return Confidence score between 0.0 (no confidence) and 1.0 (high confidence)
     */
    public double getPredictionConfidence() {
        if (history.size() < 10) {
            return 0.0;
        }
        
        // Calculate variance in recent data
        List<Double> recentFrametimes = new ArrayList<>();
        history.forEach(s -> recentFrametimes.add(s.p95Frametime));
        
        double mean = recentFrametimes.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
        
        double variance = recentFrametimes.stream()
            .mapToDouble(f -> Math.pow(f - mean, 2))
            .average()
            .orElse(0.0);
        
        double stdDev = Math.sqrt(variance);
        double cv = mean > 0 ? stdDev / mean : 1.0; // Coefficient of variation
        
        // Lower variance = higher confidence
        // CV of 0.1 or less = high confidence (0.9+)
        // CV of 0.5 or more = low confidence (0.2-)
        return Math.max(0.0, Math.min(1.0, 1.0 - (cv * 2.0)));
    }
    
    /**
     * Clears all historical data and resets the predictor.
     * Useful when switching scenarios or after major game state changes.
     */
    public void reset() {
        history.clear();
        lastPrediction = 0.0;
        lastPredictionTime = 0;
    }
    
    /**
     * Gets the size of the prediction history.
     * 
     * @return number of snapshots currently stored
     */
    public int getHistorySize() {
        return history.size();
    }
    
    /**
     * Checks if the predictor has enough data for reliable predictions.
     * 
     * @return true if sufficient data is available
     */
    public boolean hasEnoughData() {
        return history.size() >= MIN_SAMPLES_FOR_PREDICTION;
    }
}
