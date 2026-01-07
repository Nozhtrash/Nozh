package dev.nozh.core.intelligence;

/**
 * Predicts performance spikes before they occur.
 * 
 * Uses TrendAnalyzer to forecast frametime and
 * determines if preventive action is needed.
 * 
 * Prediction model:
 * - If frametime rising > 0.5 ms/s → spike in ~5s
 * - If predicted FPS < target → suggest action
 * - If trend accelerating → urgent action
 * 
 * TASK 8: Predictive intelligence - spike forecasting
 */
public final class PerformancePredictor {

    private final TrendAnalyzer trendAnalyzer;
    private final int targetFps;

    private static final double PREDICTION_HORIZON_SECONDS = 5.0;
    private static final double SPIKE_THRESHOLD_MS = 20.0; // Predicted spike

    public PerformancePredictor(int targetFps) {
        this.trendAnalyzer = new TrendAnalyzer();
        this.targetFps = targetFps;
    }

    /**
     * Add telemetry sample.
     */
    public void addSample(double frametimeMs) {
        trendAnalyzer.addSample(frametimeMs);
    }

    /**
     * Predict if spike will occur soon.
     */
    public boolean predictSpike() {
        double predictedFrametime = trendAnalyzer.predictFrametime(PREDICTION_HORIZON_SECONDS);
        return predictedFrametime > SPIKE_THRESHOLD_MS;
    }

    /**
     * Predict if FPS will drop below target.
     */
    public boolean predictFpsDrop() {
        double predictedFrametime = trendAnalyzer.predictFrametime(PREDICTION_HORIZON_SECONDS);
        double predictedFps = 1000.0 / predictedFrametime;
        return predictedFps < targetFps;
    }

    /**
     * Get urgency level (0.0-1.0).
     * 0.0 = no action needed, 1.0 = urgent action
     */
    public double getUrgency() {
        if (!trendAnalyzer.isDegrading()) {
            return 0.0;
        }

        double slope = trendAnalyzer.getTrendSlope();
        double predictedFrametime = trendAnalyzer.predictFrametime(PREDICTION_HORIZON_SECONDS);

        // Urgency based on prediction severity
        double targetFrametime = 1000.0 / targetFps;
        double overshoot = Math.max(0, predictedFrametime - targetFrametime);

        double urgency = overshoot / targetFrametime;
        return Math.min(1.0, urgency);
    }

    /**
     * Get prediction confidence (0.0-1.0).
     */
    public double getPredictionConfidence() {
        // Confidence based on data availability and trend stability
        // For now, simple heuristic
        return trendAnalyzer.getTrendType() != TrendAnalyzer.TrendType.UNKNOWN ? 0.7 : 0.0;
    }

    /**
     * Get recommended action urgency.
     */
    public ActionUrgency getRecommendedUrgency() {
        double urgency = getUrgency();

        if (urgency > 0.8) {
            return ActionUrgency.CRITICAL; // Immediate action
        }
        if (urgency > 0.5) {
            return ActionUrgency.HIGH; // Soon
        }
        if (urgency > 0.2) {
            return ActionUrgency.MEDIUM; // Preventive
        }
        return ActionUrgency.LOW; // Monitor
    }

    public enum ActionUrgency {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}
