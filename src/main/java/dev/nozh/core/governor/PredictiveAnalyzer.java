package dev.nozh.core.governor;

/**
 * Predictive analyzer for FPS trend detection - ZERO ALLOCATION VERSION.
 * 
 * Uses simple linear regression to predict FPS drops BEFORE they become severe.
 * This allows the governor to act proactively instead of reactively.
 * 
 * Performance: Uses primitive array (no boxing), zero allocations after init.
 */
public final class PredictiveAnalyzer {

    private static final int SAMPLES_FOR_PREDICTION = 30; // Last 30 frames
    private static final double PREDICTIVE_THRESHOLD = 0.3; // ms/frame slope

    // ZERO ALLOCATION: Use primitive array instead of ArrayList<Double>
    private final double[] recentFrametimes;
    private int sampleCount = 0;
    private int writeIndex = 0;

    public PredictiveAnalyzer() {
        this.recentFrametimes = new double[SAMPLES_FOR_PREDICTION];
    }

    /**
     * Add a frametime sample to the prediction buffer.
     * ZERO ALLOCATION: Ring buffer with primitives.
     */
    public void addSample(double frametimeMs) {
        recentFrametimes[writeIndex] = frametimeMs;
        writeIndex = (writeIndex + 1) % SAMPLES_FOR_PREDICTION;

        if (sampleCount < SAMPLES_FOR_PREDICTION) {
            sampleCount++;
        }
    }

    /**
     * Predict if FPS will drop based on frametime trend.
     * ZERO ALLOCATION: Uses primitive operations only.
     * 
     * @return true if trend indicates worsening performance
     */
    public boolean predictFPSDrop() {
        if (sampleCount < SAMPLES_FOR_PREDICTION) {
            return false; // Not enough data
        }

        double slope = calculateSlope();

        // Positive slope = increasing frametime = worsening FPS
        return slope > PREDICTIVE_THRESHOLD;
    }

    /**
     * Get prediction confidence (0.0 to 1.0).
     * ZERO ALLOCATION.
     */
    public double getConfidence() {
        if (sampleCount < SAMPLES_FOR_PREDICTION) {
            return 0.0;
        }

        double slope = Math.abs(calculateSlope());

        // Normalize confidence: slope 0.0-1.0 → confidence 0.0-1.0
        return Math.min(slope / 1.0, 1.0);
    }

    /**
     * Calculate slope of frametime trend using simple linear regression.
     * ZERO ALLOCATION: No streams, no boxing, pure primitives.
     * 
     * Formula: slope = Σ((x - x̄)(y - ȳ)) / Σ((x - x̄)²)
     */
    private double calculateSlope() {
        int n = sampleCount;

        // Calculate mean Y (average frametime)
        double sumY = 0.0;
        for (int i = 0; i < n; i++) {
            sumY += recentFrametimes[i];
        }
        double meanY = sumY / n;

        // Mean X is always (n-1)/2 for indices 0..n-1
        double meanX = (n - 1) / 2.0;

        // Calculate slope components
        double numerator = 0.0;
        double denominator = 0.0;

        for (int i = 0; i < n; i++) {
            double x = i;
            double y = recentFrametimes[i];

            double dx = x - meanX;
            double dy = y - meanY;

            numerator += dx * dy;
            denominator += dx * dx;
        }

        if (denominator == 0.0) {
            return 0.0;
        }

        return numerator / denominator;
    }

    /**
     * Reset prediction state (e.g., after governor action).
     * ZERO ALLOCATION.
     */
    public void reset() {
        sampleCount = 0;
        writeIndex = 0;
        // No need to clear array - old data will be overwritten
    }

    /**
     * Get current trend description for logging.
     * Only allocates when called (logging only).
     */
    public String getTrendDescription() {
        if (sampleCount < SAMPLES_FOR_PREDICTION) {
            return "INSUFFICIENT_DATA";
        }

        double slope = calculateSlope();

        if (slope > PREDICTIVE_THRESHOLD) {
            return "WORSENING (slope: " + String.format("%.3f", slope) + ")";
        } else if (slope < -PREDICTIVE_THRESHOLD) {
            return "IMPROVING (slope: " + String.format("%.3f", slope) + ")";
        } else {
            return "STABLE (slope: " + String.format("%.3f", slope) + ")";
        }
    }
}
