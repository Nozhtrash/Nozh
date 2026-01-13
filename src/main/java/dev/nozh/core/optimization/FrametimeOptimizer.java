package dev.nozh.core.optimization;

import dev.nozh.NozhConstants;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Core optimization engine using advanced mathematical models.
 * 
 * <p>
 * Implements "NOZH Ultimate" intelligence features:
 * <ul>
 * <li><b>Kalman Filter</b> - Optimal frametime prediction</li>
 * <li><b>Dual EMA</b> - Fast (α=0.4) vs Slow (α=0.1) trend detection</li>
 * <li><b>Bayesian Confidence</b> - Adaptive confidence scoring based on
 * prediction accuracy</li>
 * <li><b>Welford's Variance</b> - Numerically stable rolling variance for
 * jitter detection</li>
 * <li><b>PID Controller</b> - Smooth adjustments</li>
 * </ul>
 * 
 * <p>
 * <b>Thread Safety:</b> Fully thread-safe via synchronized blocks and atomics.
 * 
 * @since 0.3.1
 * @author NOZH Team
 */
public final class FrametimeOptimizer {

    // --- Kalman Filter State ---
    private double kalmanEstimate;
    private double kalmanError;
    private double kalmanGain;

    private static final double PROCESS_NOISE = 0.01; // Q
    private static final double MEASUREMENT_NOISE = 0.1; // R

    // --- PID Controller State ---
    private double previousError;
    private double integral;

    // Tuned gains
    private double kP = 0.35;
    private double kI = 0.04;
    private double kD = 0.12;

    // --- Dual EMA System ---
    private double fastEma; // Alpha = 0.4 (Responsive)
    private double slowEma; // Alpha = 0.1 (Stable)

    private static final double EMA_FAST_ALPHA = 0.4;
    private static final double EMA_SLOW_ALPHA = 0.1;

    // --- Rolling Variance (Welford's Algorithm) ---
    private double m2; // Sum of squares of differences from the current mean
    private double mean;
    private double variance;
    private double stdDev;

    // --- Bayesian Confidence System ---
    private double confidenceScore; // 0.0 to 1.0
    private double predictionAccuracy; // Moving average of prediction accuracy
    private int successfulPredictionsInARow;

    // Confidence parameters
    private static final double CONFIDENCE_DECAY = 0.98;
    private static final double PREDICTION_ACCURACY_ALPHA = 0.05;

    // --- State & Statistics ---
    private boolean initialized;
    private long sampleCount;
    private final AtomicLong lastUpdateTime = new AtomicLong();

    public FrametimeOptimizer() {
        reset();
    }

    /**
     * Updates the optimizer with a new frametime measurement.
     * 
     * @param frametimeMs the measured frametime in milliseconds
     */
    public synchronized void update(double frametimeMs) {
        if (frametimeMs < 0 || frametimeMs > 2000) { // Limit to 2s to catch massive freezes but ignore obvious errors
            // Log only periodically to avoid spam? For now just return.
            return;
        }

        if (!initialized) {
            initialize(frametimeMs);
            return;
        }

        // 1. Calculate prediction error BEFORE updating state (for Bayesian Confidence)
        double predictionError = Math.abs(frametimeMs - kalmanEstimate);
        updateConfidence(predictionError, frametimeMs);

        // 2. Update Kalman Filter
        updateKalmanFilter(frametimeMs);

        // 3. Update Dual EMAs
        fastEma = EMA_FAST_ALPHA * frametimeMs + (1 - EMA_FAST_ALPHA) * fastEma;
        slowEma = EMA_SLOW_ALPHA * frametimeMs + (1 - EMA_SLOW_ALPHA) * slowEma;

        // 4. Update Rolling Variance (Welford's Algorithm)
        sampleCount++;
        double delta = frametimeMs - mean;
        mean += delta / sampleCount;
        double delta2 = frametimeMs - mean;
        m2 += delta * delta2;

        // Variance is M2 / (count - 1), but for stability we use population variance or
        // just M2/count for simple stats
        // Using sample variance (unbiased)
        if (sampleCount > 1) {
            variance = m2 / (sampleCount - 1);
            stdDev = Math.sqrt(variance);
        }

        lastUpdateTime.set(System.currentTimeMillis());
    }

    private void initialize(double initialFrametime) {
        kalmanEstimate = initialFrametime;
        kalmanError = 1.0;

        fastEma = initialFrametime;
        slowEma = initialFrametime;

        mean = initialFrametime;
        m2 = 0.0;

        confidenceScore = 0.5; // Start neutral
        predictionAccuracy = 0.8; // Assume reasonable start

        initialized = true;
        sampleCount = 1;
        lastUpdateTime.set(System.currentTimeMillis());
    }

    private void updateKalmanFilter(double measurement) {
        // Predict
        double predictedEstimate = kalmanEstimate;
        double predictedError = kalmanError + PROCESS_NOISE;

        // Update
        kalmanGain = predictedError / (predictedError + MEASUREMENT_NOISE);
        kalmanEstimate = predictedEstimate + kalmanGain * (measurement - predictedEstimate);
        kalmanError = (1 - kalmanGain) * predictedError;
    }

    private void updateConfidence(double error, double actual) {
        // Calculate accuracy for this frame (1.0 = perfect, 0.0 = >50% error)
        double relativeError = error / Math.max(1.0, actual);
        double frameAccuracy = Math.max(0.0, 1.0 - (relativeError * 2)); // Steep penalty

        // Update moving average of accuracy
        predictionAccuracy = (PREDICTION_ACCURACY_ALPHA * frameAccuracy) +
                ((1 - PREDICTION_ACCURACY_ALPHA) * predictionAccuracy);

        // Bayes-like update: posterior = likelihood * prior
        // Here we simplify: confidence grows if accuracy is high, decays if low

        if (frameAccuracy > 0.8) {
            successfulPredictionsInARow++;
            // Boost confidence on streaks
            double streakBonus = Math.min(0.2, successfulPredictionsInARow * 0.01);
            confidenceScore = Math.min(1.0, confidenceScore * (1.0 + 0.05 + streakBonus));
        } else {
            successfulPredictionsInARow = 0;
            // Decay confidence on misses
            confidenceScore *= CONFIDENCE_DECAY;
            // Extra penalty for big misses
            if (frameAccuracy < 0.4) {
                confidenceScore *= 0.9;
            }
        }

        // Clamp
        confidenceScore = Math.max(0.1, Math.min(1.0, confidenceScore));
    }

    public synchronized double predictNextFrametime() {
        return kalmanEstimate;
    }

    public synchronized double calculateOptimalIntensity(double currentP95, double targetP95) {
        double error = currentP95 - targetP95;

        // Proportional
        double proportional = kP * error;

        // Integral
        integral += error;
        integral = Math.max(-50, Math.min(50, integral)); // Anti-windup
        double integralTerm = kI * integral;

        // Derivative
        double derivative = error - previousError;
        double derivativeTerm = kD * derivative;

        double output = proportional + integralTerm + derivativeTerm;
        previousError = error;

        return Math.tanh(output / 10.0);
    }

    public synchronized TrendAnalysis analyzeTrend() {
        if (!initialized || sampleCount < 20) {
            return new TrendAnalysis(TrendType.INSUFFICIENT_DATA, 0.0, "Gathering samples...");
        }

        // 1. Check for Dual EMA Crossover (Degradation signal)
        // If Fast EMA is significantly above Slow EMA, we are spiking/degrading
        double emaDelta = fastEma - slowEma;

        if (emaDelta > Math.max(2.0, slowEma * 0.1)) { // >2ms or >10% rise
            return new TrendAnalysis(TrendType.DEGRADING, emaDelta, "Fast degradation detected");
        }

        if (emaDelta < -Math.max(2.0, slowEma * 0.1)) {
            return new TrendAnalysis(TrendType.IMPROVING, Math.abs(emaDelta), "Rapid improvement");
        }

        // 2. Check for Micro-stutters via Variance
        // If variance is high but EMAs are close, we have jitter/stutter
        if (stdDev > Math.max(3.0, slowEma * 0.15)) {
            return new TrendAnalysis(TrendType.UNSTABLE, stdDev, "High variance / Micro-stutters");
        }

        // 3. Spike detection (Single frame)
        if (Math.abs(kalmanEstimate - slowEma) > 3 * stdDev) {
            return new TrendAnalysis(TrendType.SPIKE, stdDev, "Sudden spike");
        }

        return new TrendAnalysis(TrendType.STABLE, variance, "Stable");
    }

    public synchronized double getMeasurementConfidence() {
        return confidenceScore;
    }

    public synchronized double getKalmanEstimate() {
        return kalmanEstimate;
    }

    public synchronized double getFastEma() {
        return fastEma;
    }

    public synchronized double getSlowEma() {
        return slowEma;
    }

    public synchronized double getStdDev() {
        return stdDev;
    }

    public synchronized long getSampleCount() {
        return sampleCount;
    }

    public synchronized void reset() {
        kalmanEstimate = 16.67;
        kalmanError = 1.0;
        fastEma = 16.67;
        slowEma = 16.67;
        mean = 16.67;
        m2 = 0.0;
        variance = 0.0;
        stdDev = 0.0;

        previousError = 0.0;
        integral = 0.0;

        confidenceScore = 0.5;
        predictionAccuracy = 0.8;
        successfulPredictionsInARow = 0;

        sampleCount = 0;
        initialized = false;

        NozhConstants.LOGGER.debug("FrametimeOptimizer reset (Ultimate)");
    }

    public record TrendAnalysis(TrendType type, double magnitude, String description) {
    }

    public enum TrendType {
        INSUFFICIENT_DATA,
        STABLE,
        IMPROVING,
        DEGRADING,
        UNSTABLE, // New: High variance but stable average (jitter)
        SPIKE
    }
}
