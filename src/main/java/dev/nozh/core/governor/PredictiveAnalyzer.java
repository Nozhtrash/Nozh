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

    private static final int SHORT_WINDOW_SAMPLES = 20;
    private static final int MEDIUM_WINDOW_SAMPLES = 60;
    private static final double BASE_SHORT_THRESHOLD = 0.35;
    private static final double BASE_MEDIUM_THRESHOLD = 0.25;
    private static final double MIN_THRESHOLD = 0.15;
    private static final double MAX_THRESHOLD = 0.6;

    // ZERO ALLOCATION: Use primitive array instead of ArrayList<Double>
    private final double[] shortWindow;
    private final double[] mediumWindow;
    private int shortSampleCount = 0;
    private int mediumSampleCount = 0;
    private int shortWriteIndex = 0;
    private int mediumWriteIndex = 0;
    private double shortThreshold = BASE_SHORT_THRESHOLD;
    private double mediumThreshold = BASE_MEDIUM_THRESHOLD;
    private final Prediction prediction = new Prediction();

    public PredictiveAnalyzer() {
        this.shortWindow = new double[SHORT_WINDOW_SAMPLES];
        this.mediumWindow = new double[MEDIUM_WINDOW_SAMPLES];
    }

    /**
     * Add a frametime sample to the prediction buffer.
     * ZERO ALLOCATION: Ring buffer with primitives.
     */
    public void addSample(double frametimeMs) {
        shortWindow[shortWriteIndex] = frametimeMs;
        shortWriteIndex = (shortWriteIndex + 1) % SHORT_WINDOW_SAMPLES;
        if (shortSampleCount < SHORT_WINDOW_SAMPLES) {
            shortSampleCount++;
        }

        mediumWindow[mediumWriteIndex] = frametimeMs;
        mediumWriteIndex = (mediumWriteIndex + 1) % MEDIUM_WINDOW_SAMPLES;
        if (mediumSampleCount < MEDIUM_WINDOW_SAMPLES) {
            mediumSampleCount++;
        }
    }

    /**
     * Predict if FPS will drop based on frametime trend.
     * ZERO ALLOCATION: Uses primitive operations only.
     * 
     * @return true if trend indicates worsening performance
     */
    public boolean predictFPSDrop() {
        return evaluate().isLikely();
    }

    /**
     * Get prediction confidence (0.0 to 1.0).
     * ZERO ALLOCATION.
     */
    public double getConfidence() {
        return evaluate().confidence();
    }

    /**
     * Evaluate predictive windows.
     * ZERO ALLOCATION: returns reusable prediction instance.
     */
    public Prediction evaluate() {
        boolean shortReady = shortSampleCount >= SHORT_WINDOW_SAMPLES;
        boolean mediumReady = mediumSampleCount >= MEDIUM_WINDOW_SAMPLES;
        if (!shortReady && !mediumReady) {
            prediction.reset();
            return prediction;
        }

        double shortSlope = shortReady ? calculateSlope(shortWindow, shortSampleCount, SHORT_WINDOW_SAMPLES,
                shortWriteIndex) : 0.0;
        double mediumSlope = mediumReady ? calculateSlope(mediumWindow, mediumSampleCount, MEDIUM_WINDOW_SAMPLES,
                mediumWriteIndex) : 0.0;

        boolean shortLikely = shortReady && shortSlope > shortThreshold;
        boolean mediumLikely = mediumReady && mediumSlope > mediumThreshold;

        double shortConfidence = shortReady ? normalizeSlope(shortSlope, shortThreshold) : 0.0;
        double mediumConfidence = mediumReady ? normalizeSlope(mediumSlope, mediumThreshold) : 0.0;
        double confidence = Math.max(shortConfidence, mediumConfidence);

        Window window = Window.NONE;
        if (shortLikely && mediumLikely) {
            window = Window.BOTH;
        } else if (shortLikely) {
            window = Window.SHORT;
        } else if (mediumLikely) {
            window = Window.MEDIUM;
        }

        prediction.update(shortSlope, mediumSlope, shortLikely, mediumLikely, confidence, window);
        return prediction;
    }

    /**
     * Adjust thresholds based on learning feedback.
     * ZERO ALLOCATION.
     */
    public void applyLearning(double accuracy, double avgConfidence) {
        double accuracyBias = 0.5 - accuracy;
        double confidenceBias = 0.5 - avgConfidence;
        double adjustment = (accuracyBias * 0.2) + (confidenceBias * 0.1);
        shortThreshold = clamp(BASE_SHORT_THRESHOLD + adjustment, MIN_THRESHOLD, MAX_THRESHOLD);
        mediumThreshold = clamp(BASE_MEDIUM_THRESHOLD + adjustment, MIN_THRESHOLD, MAX_THRESHOLD);
    }

    /**
     * Calculate slope of frametime trend using simple linear regression.
     * ZERO ALLOCATION: No streams, no boxing, pure primitives.
     * 
     * Formula: slope = Σ((x - x̄)(y - ȳ)) / Σ((x - x̄)²)
     */
    private double calculateSlope(double[] samples, int sampleCount, int windowSize, int writeIndex) {
        int n = sampleCount;

        // Calculate mean Y (average frametime)
        double sumY = 0.0;
        for (int i = 0; i < n; i++) {
            sumY += readSample(samples, sampleCount, windowSize, writeIndex, i);
        }
        double meanY = sumY / n;

        // Mean X is always (n-1)/2 for indices 0..n-1
        double meanX = (n - 1) / 2.0;

        // Calculate slope components
        double numerator = 0.0;
        double denominator = 0.0;

        for (int i = 0; i < n; i++) {
            double x = i;
            double y = readSample(samples, sampleCount, windowSize, writeIndex, i);

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

    private double readSample(double[] samples, int sampleCount, int windowSize, int writeIndex, int offset) {
        int startIndex = sampleCount < windowSize ? 0 : writeIndex;
        int index = startIndex + offset;
        if (index >= windowSize) {
            index -= windowSize;
        }
        return samples[index];
    }

    private double normalizeSlope(double slope, double threshold) {
        if (threshold <= 0.0 || slope <= 0.0) {
            return 0.0;
        }
        return Math.min(slope / threshold, 1.0);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Reset prediction state (e.g., after governor action).
     * ZERO ALLOCATION.
     */
    public void reset() {
        shortSampleCount = 0;
        mediumSampleCount = 0;
        shortWriteIndex = 0;
        mediumWriteIndex = 0;
        prediction.reset();
        // No need to clear arrays - old data will be overwritten
    }

    /**
     * Get current trend description for logging.
     * Only allocates when called (logging only).
     */
    public String getTrendDescription() {
        Prediction current = evaluate();
        if (!current.ready()) {
            return "INSUFFICIENT_DATA";
        }

        String shortLabel = formatTrend("short", current.shortSlope(), shortThreshold);
        String mediumLabel = formatTrend("medium", current.mediumSlope(), mediumThreshold);
        return shortLabel + ", " + mediumLabel + " (confidence: " + String.format("%.2f", current.confidence()) + ")";
    }

    private String formatTrend(String label, double slope, double threshold) {
        if (slope > threshold) {
            return label + "=WORSENING(" + String.format("%.3f", slope) + ")";
        }
        if (slope < -threshold) {
            return label + "=IMPROVING(" + String.format("%.3f", slope) + ")";
        }
        return label + "=STABLE(" + String.format("%.3f", slope) + ")";
    }

    public enum Window {
        NONE,
        SHORT,
        MEDIUM,
        BOTH
    }

    public static final class Prediction {
        private double shortSlope;
        private double mediumSlope;
        private boolean shortLikely;
        private boolean mediumLikely;
        private double confidence;
        private Window window = Window.NONE;
        private boolean ready;

        private void update(double shortSlope, double mediumSlope, boolean shortLikely, boolean mediumLikely,
                double confidence, Window window) {
            this.shortSlope = shortSlope;
            this.mediumSlope = mediumSlope;
            this.shortLikely = shortLikely;
            this.mediumLikely = mediumLikely;
            this.confidence = confidence;
            this.window = window;
            this.ready = true;
        }

        private void reset() {
            shortSlope = 0.0;
            mediumSlope = 0.0;
            shortLikely = false;
            mediumLikely = false;
            confidence = 0.0;
            window = Window.NONE;
            ready = false;
        }

        public boolean ready() {
            return ready;
        }

        public boolean isLikely() {
            return shortLikely || mediumLikely;
        }

        public double shortSlope() {
            return shortSlope;
        }

        public double mediumSlope() {
            return mediumSlope;
        }

        public double confidence() {
            return confidence;
        }

        public Window window() {
            return window;
        }
    }
}
