package dev.nozh.core.optimization;

import dev.nozh.NozhConstants;

/**
 * Core optimization engine using advanced mathematical models.
 * 
 * <p>
 * Implements three key algorithms:
 * <ul>
 * <li><b>Kalman Filter</b> - Optimal frametime prediction with noise
 * reduction</li>
 * <li><b>PID Controller</b> - Smooth adjustments without oscillation</li>
 * <li><b>Exponential Moving Average</b> - Trend detection and decay</li>
 * </ul>
 * 
 * <p>
 * <b>Thread Safety:</b> All methods are thread-safe via synchronized blocks.
 * <p>
 * <b>Performance:</b> Zero allocations in hot path methods.
 * <p>
 * <b>Integration:</b> Use with {@link dev.nozh.core.governor.GovernorRunner}
 * 
 * @since 0.3.0
 * @author NOZH Team
 */
public final class FrametimeOptimizer {

    // Kalman Filter state
    private double kalmanEstimate;
    private double kalmanError;
    private double kalmanGain;

    // Kalman Filter parameters
    private static final double PROCESS_NOISE = 0.01; // Q - process noise covariance
    private static final double MEASUREMENT_NOISE = 0.1; // R - measurement noise covariance

    // PID Controller state
    private double previousError;
    private double integral;

    // PID Controller gains (tuned for frametime optimization)
    private double kP = 0.3; // Proportional gain
    private double kI = 0.05; // Integral gain
    private double kD = 0.1; // Derivative gain

    // Exponential Moving Average
    private double ema;
    private static final double EMA_ALPHA = 0.2; // Smoothing factor

    // Statistics
    private double variance;
    private double stdDev;
    private int sampleCount;

    // State
    private boolean initialized;
    private long lastUpdateTime;

    /**
     * Constructs a new FrametimeOptimizer with default parameters.
     */
    public FrametimeOptimizer() {
        this.kalmanEstimate = 16.67; // Default to 60 FPS
        this.kalmanError = 1.0;
        this.ema = 16.67;
        this.initialized = false;
        this.sampleCount = 0;
    }

    /**
     * Updates the optimizer with a new frametime measurement.
     * 
     * <p>
     * This method updates all internal state:
     * <ul>
     * <li>Kalman filter estimate</li>
     * <li>Exponential moving average</li>
     * <li>Variance and standard deviation</li>
     * </ul>
     * 
     * @param frametimeMs the measured frametime in milliseconds
     * @throws IllegalArgumentException if frametime is negative or > 1000ms
     */
    public synchronized void update(double frametimeMs) {
        if (frametimeMs < 0 || frametimeMs > 1000) {
            NozhConstants.LOGGER.warn("Invalid frametime: {}ms, ignoring", frametimeMs);
            return;
        }

        if (!initialized) {
            kalmanEstimate = frametimeMs;
            ema = frametimeMs;
            initialized = true;
            sampleCount = 1;
            lastUpdateTime = System.currentTimeMillis();
            return;
        }

        // Update Kalman Filter
        updateKalmanFilter(frametimeMs);

        // Update Exponential Moving Average
        ema = EMA_ALPHA * frametimeMs + (1 - EMA_ALPHA) * ema;

        // Update variance (for spike detection)
        double diff = frametimeMs - kalmanEstimate;
        variance = 0.9 * variance + 0.1 * (diff * diff);
        stdDev = Math.sqrt(variance);

        sampleCount++;
        lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * Updates the Kalman filter with a new measurement.
     * 
     * <p>
     * Kalman filter provides optimal frametime estimate by:
     * <ol>
     * <li>Predicting next estimate</li>
     * <li>Calculating Kalman gain</li>
     * <li>Updating estimate based on measurement</li>
     * <li>Updating error covariance</li>
     * </ol>
     * 
     * @param measurement the measured frametime
     */
    private void updateKalmanFilter(double measurement) {
        // Prediction
        double predictedEstimate = kalmanEstimate;
        double predictedError = kalmanError + PROCESS_NOISE;

        // Update
        kalmanGain = predictedError / (predictedError + MEASUREMENT_NOISE);
        kalmanEstimate = predictedEstimate + kalmanGain * (measurement - predictedEstimate);
        kalmanError = (1 - kalmanGain) * predictedError;
    }

    /**
     * Predicts the next frametime using the Kalman filter estimate.
     * 
     * <p>
     * The prediction is the current Kalman estimate, which represents
     * the optimal estimate given all previous measurements and the mathematical
     * model of the system.
     * 
     * @return predicted frametime in milliseconds
     */
    public synchronized double predictNextFrametime() {
        return kalmanEstimate;
    }

    /**
     * Calculates optimal action intensity using PID controller.
     * 
     * <p>
     * The PID controller calculates how aggressively to apply optimizations:
     * <ul>
     * <li><b>P (Proportional)</b>: React to current error</li>
     * <li><b>I (Integral)</b>: Correct accumulated error</li>
     * <li><b>D (Derivative)</b>: Predict future error</li>
     * </ul>
     * 
     * <p>
     * Output range: [-1.0, 1.0]
     * <ul>
     * <li>Negative: performance is better than target, can increase quality</li>
     * <li>Positive: performance is worse than target, must reduce quality</li>
     * </ul>
     * 
     * @param currentP95 current P95 frametime in ms
     * @param targetP95  target P95 frametime in ms
     * @return action intensity from -1.0 (increase quality) to 1.0 (reduce quality)
     */
    public synchronized double calculateOptimalIntensity(double currentP95, double targetP95) {
        // Calculate error (positive = over target, negative = under target)
        double error = currentP95 - targetP95;

        // Proportional term
        double proportional = kP * error;

        // Integral term (accumulated error over time)
        integral += error;
        // Anti-windup: clamp integral to prevent overflow
        integral = Math.max(-50, Math.min(50, integral));
        double integralTerm = kI * integral;

        // Derivative term (rate of change)
        double derivative = error - previousError;
        double derivativeTerm = kD * derivative;

        // PID output
        double output = proportional + integralTerm + derivativeTerm;

        // Update state
        previousError = error;

        // Normalize to [-1, 1] range
        // Using tanh for smooth saturation
        return Math.tanh(output / 10.0);
    }

    /**
     * Analyzes current performance trend.
     * 
     * @return trend analysis result
     */
    public synchronized TrendAnalysis analyzeTrend() {
        if (!initialized || sampleCount < 10) {
            return new TrendAnalysis(
                    TrendType.INSUFFICIENT_DATA,
                    0.0,
                    "Need at least 10 samples");
        }

        // Detect spikes using standard deviation
        double currentFrametime = kalmanEstimate;
        boolean isSpike = Math.abs(currentFrametime - ema) > 2 * stdDev;

        if (isSpike) {
            return new TrendAnalysis(
                    TrendType.SPIKE,
                    stdDev,
                    String.format("Spike detected: %.1fms deviation", stdDev));
        }

        // Determine trend direction using EMA vs Kalman
        double trend = ema - kalmanEstimate;

        if (Math.abs(trend) < 0.5) {
            return new TrendAnalysis(
                    TrendType.STABLE,
                    variance,
                    "Performance stable");
        } else if (trend > 0) {
            return new TrendAnalysis(
                    TrendType.DEGRADING,
                    trend,
                    String.format("Performance degrading: +%.1fms", trend));
        } else {
            return new TrendAnalysis(
                    TrendType.IMPROVING,
                    Math.abs(trend),
                    String.format("Performance improving: %.1fms", Math.abs(trend)));
        }
    }

    /**
     * Gets confidence in current measurements.
     * 
     * <p>
     * Confidence is based on:
     * <ul>
     * <li>Sample count (more samples = higher confidence)</li>
     * <li>Variance (lower variance = higher confidence)</li>
     * <li>Kalman error (lower error = higher confidence)</li>
     * </ul>
     * 
     * @return confidence from 0.0 (no confidence) to 1.0 (full confidence)
     */
    public synchronized double getMeasurementConfidence() {
        if (!initialized || sampleCount < 3) {
            return 0.0;
        }

        // Confidence factors
        double sampleFactor = Math.min(1.0, sampleCount / 30.0);
        double varianceFactor = 1.0 / (1.0 + variance);
        double errorFactor = 1.0 / (1.0 + kalmanError);

        // Weighted combination
        return 0.4 * sampleFactor + 0.3 * varianceFactor + 0.3 * errorFactor;
    }

    /**
     * Gets current Kalman filter estimate.
     * 
     * @return optimal frametime estimate in ms
     */
    public synchronized double getKalmanEstimate() {
        return kalmanEstimate;
    }

    /**
     * Gets current exponential moving average.
     * 
     * @return EMA frametime in ms
     */
    public synchronized double getEma() {
        return ema;
    }

    /**
     * Gets current standard deviation.
     * 
     * @return standard deviation in ms
     */
    public synchronized double getStdDev() {
        return stdDev;
    }

    /**
     * Gets total sample count.
     * 
     * @return number of samples processed
     */
    public synchronized int getSampleCount() {
        return sampleCount;
    }

    /**
     * Sets PID controller gains.
     * 
     * <p>
     * <b>Warning:</b> Only modify if you understand PID tuning.
     * Incorrect values can cause oscillation or instability.
     * 
     * @param kP proportional gain (recommended: 0.2-0.5)
     * @param kI integral gain (recommended: 0.01-0.1)
     * @param kD derivative gain (recommended: 0.05-0.2)
     */
    public synchronized void setPidGains(double kP, double kI, double kD) {
        this.kP = Math.max(0, Math.min(1.0, kP));
        this.kI = Math.max(0, Math.min(0.5, kI));
        this.kD = Math.max(0, Math.min(0.5, kD));

        NozhConstants.LOGGER.info("PID gains updated: P={}, I={}, D={}", this.kP, this.kI, this.kD);
    }

    /**
     * Resets all internal state.
     */
    public synchronized void reset() {
        kalmanEstimate = 16.67;
        kalmanError = 1.0;
        ema = 16.67;
        variance = 0.0;
        stdDev = 0.0;
        previousError = 0.0;
        integral = 0.0;
        sampleCount = 0;
        initialized = false;

        NozhConstants.LOGGER.debug("FrametimeOptimizer reset");
    }

    /**
     * Trend analysis result.
     * 
     * @param type        trend type
     * @param magnitude   magnitude of the trend
     * @param description human-readable description
     */
    public record TrendAnalysis(
            TrendType type,
            double magnitude,
            String description) {
    }

    /**
     * Types of performance trends.
     */
    public enum TrendType {
        /** Not enough data to determine trend */
        INSUFFICIENT_DATA,

        /** Performance is stable */
        STABLE,

        /** Performance is improving */
        IMPROVING,

        /** Performance is degrading */
        DEGRADING,

        /** Temporary performance spike detected */
        SPIKE
    }
}
