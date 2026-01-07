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
    private static final int LONG_WINDOW_SAMPLES = 120;

    private final FeatureState[] featureStates;
    private final Prediction prediction = new Prediction();

    public PredictiveAnalyzer() {
        Feature[] features = Feature.values();
        this.featureStates = new FeatureState[features.length];
        for (int i = 0; i < features.length; i++) {
            featureStates[i] = buildFeatureState(features[i]);
        }
    }

    /**
     * Add a frametime sample to the prediction buffer.
     * ZERO ALLOCATION: Ring buffer with primitives.
     */
    public void addSample(double frametimeMs) {
        addSample(Feature.FRAME, frametimeMs);
    }

    /**
     * Add a feature sample to the prediction buffer.
     * ZERO ALLOCATION: Ring buffer with primitives.
     */
    public void addSample(Feature feature, double value) {
        if (feature == null) {
            return;
        }
        featureStates[feature.ordinal()].addSample(value);
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
        boolean anyReady = false;
        boolean anyLikely = false;
        double bestConfidence = 0.0;
        Feature bestFeature = Feature.FRAME;
        Window bestWindow = Window.NONE;
        int likelyFeatures = 0;

        for (FeatureState state : featureStates) {
            FeaturePrediction featurePrediction = state.evaluate();
            if (!featurePrediction.ready()) {
                continue;
            }
            anyReady = true;
            if (featurePrediction.isLikely()) {
                anyLikely = true;
                likelyFeatures++;
            }
            if (featurePrediction.confidence() > bestConfidence) {
                bestConfidence = featurePrediction.confidence();
                bestFeature = state.feature();
                bestWindow = featurePrediction.window();
            }
        }

        if (!anyReady) {
            prediction.reset();
            return prediction;
        }

        prediction.update(bestFeature, bestWindow, bestConfidence, anyLikely, likelyFeatures, true);
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
        for (FeatureState state : featureStates) {
            state.applyLearning(adjustment);
        }
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

    private FeatureState buildFeatureState(Feature feature) {
        return switch (feature) {
            case FRAME -> new FeatureState(feature, 0.35, 0.25, 0.18, 0.15, 0.6, 1.0);
            case TICK -> new FeatureState(feature, 0.4, 0.3, 0.22, 0.18, 0.7, 0.9);
            case RENDER -> new FeatureState(feature, 0.35, 0.25, 0.18, 0.15, 0.6, 1.0);
            case ENTITY -> new FeatureState(feature, 2.5, 1.8, 1.2, 0.5, 6.0, 0.8);
            case GC -> new FeatureState(feature, 0.02, 0.015, 0.01, 0.005, 0.08, 0.6);
        };
    }

    /**
     * Reset prediction state (e.g., after governor action).
     * ZERO ALLOCATION.
     */
    public void reset() {
        for (FeatureState state : featureStates) {
            state.reset();
        }
        prediction.reset();
    }

    /**
     * Get current trend description for logging.
     * Only allocates when called (logging only).
     */
    public String getTrendDescription() {
        StringBuilder builder = new StringBuilder();
        for (FeatureState state : featureStates) {
            String label = state.getTrendDescription();
            if (label.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(label);
        }
        if (builder.length() == 0) {
            return "INSUFFICIENT_DATA";
        }
        return builder.toString();
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

    public enum Feature {
        FRAME,
        TICK,
        RENDER,
        ENTITY,
        GC
    }

    public enum Window {
        NONE,
        SHORT,
        MEDIUM,
        LONG,
        MULTI
    }

    public static final class Prediction {
        private Feature feature = Feature.FRAME;
        private Window window = Window.NONE;
        private double confidence;
        private boolean likely;
        private boolean ready;
        private int likelyFeatures;

        private void update(Feature feature, Window window, double confidence, boolean likely, int likelyFeatures,
                boolean ready) {
            this.feature = feature;
            this.window = window;
            this.confidence = confidence;
            this.likely = likely;
            this.likelyFeatures = likelyFeatures;
            this.ready = ready;
        }

        private void reset() {
            feature = Feature.FRAME;
            window = Window.NONE;
            confidence = 0.0;
            likely = false;
            ready = false;
            likelyFeatures = 0;
        }

        public boolean ready() {
            return ready;
        }

        public boolean isLikely() {
            return likely;
        }

        public double confidence() {
            return confidence;
        }

        public Feature feature() {
            return feature;
        }

        public Window window() {
            return window;
        }

        public int likelyFeatures() {
            return likelyFeatures;
        }
    }

    private final class FeatureState {
        private final Feature feature;
        private final double[] shortWindow;
        private final double[] mediumWindow;
        private final double[] longWindow;
        private final double baseShortThreshold;
        private final double baseMediumThreshold;
        private final double baseLongThreshold;
        private final double minThreshold;
        private final double maxThreshold;
        private final double learningScale;
        private double shortThreshold;
        private double mediumThreshold;
        private double longThreshold;
        private int shortSampleCount = 0;
        private int mediumSampleCount = 0;
        private int longSampleCount = 0;
        private int shortWriteIndex = 0;
        private int mediumWriteIndex = 0;
        private int longWriteIndex = 0;
        private final FeaturePrediction prediction = new FeaturePrediction();

        private FeatureState(Feature feature, double shortThreshold, double mediumThreshold, double longThreshold,
                double minThreshold, double maxThreshold, double learningScale) {
            this.feature = feature;
            this.baseShortThreshold = shortThreshold;
            this.baseMediumThreshold = mediumThreshold;
            this.baseLongThreshold = longThreshold;
            this.shortThreshold = shortThreshold;
            this.mediumThreshold = mediumThreshold;
            this.longThreshold = longThreshold;
            this.minThreshold = minThreshold;
            this.maxThreshold = maxThreshold;
            this.learningScale = learningScale;
            this.shortWindow = new double[SHORT_WINDOW_SAMPLES];
            this.mediumWindow = new double[MEDIUM_WINDOW_SAMPLES];
            this.longWindow = new double[LONG_WINDOW_SAMPLES];
        }

        private Feature feature() {
            return feature;
        }

        private void addSample(double value) {
            shortWindow[shortWriteIndex] = value;
            shortWriteIndex = (shortWriteIndex + 1) % SHORT_WINDOW_SAMPLES;
            if (shortSampleCount < SHORT_WINDOW_SAMPLES) {
                shortSampleCount++;
            }

            mediumWindow[mediumWriteIndex] = value;
            mediumWriteIndex = (mediumWriteIndex + 1) % MEDIUM_WINDOW_SAMPLES;
            if (mediumSampleCount < MEDIUM_WINDOW_SAMPLES) {
                mediumSampleCount++;
            }

            longWindow[longWriteIndex] = value;
            longWriteIndex = (longWriteIndex + 1) % LONG_WINDOW_SAMPLES;
            if (longSampleCount < LONG_WINDOW_SAMPLES) {
                longSampleCount++;
            }
        }

        private FeaturePrediction evaluate() {
            boolean shortReady = shortSampleCount >= SHORT_WINDOW_SAMPLES;
            boolean mediumReady = mediumSampleCount >= MEDIUM_WINDOW_SAMPLES;
            boolean longReady = longSampleCount >= LONG_WINDOW_SAMPLES;
            if (!shortReady && !mediumReady && !longReady) {
                prediction.reset();
                return prediction;
            }

            double shortSlope = shortReady
                    ? calculateSlope(shortWindow, shortSampleCount, SHORT_WINDOW_SAMPLES, shortWriteIndex)
                    : 0.0;
            double mediumSlope = mediumReady
                    ? calculateSlope(mediumWindow, mediumSampleCount, MEDIUM_WINDOW_SAMPLES, mediumWriteIndex)
                    : 0.0;
            double longSlope = longReady
                    ? calculateSlope(longWindow, longSampleCount, LONG_WINDOW_SAMPLES, longWriteIndex)
                    : 0.0;

            boolean shortLikely = shortReady && shortSlope > shortThreshold;
            boolean mediumLikely = mediumReady && mediumSlope > mediumThreshold;
            boolean longLikely = longReady && longSlope > longThreshold;

            double shortConfidence = shortReady ? normalizeSlope(shortSlope, shortThreshold) : 0.0;
            double mediumConfidence = mediumReady ? normalizeSlope(mediumSlope, mediumThreshold) : 0.0;
            double longConfidence = longReady ? normalizeSlope(longSlope, longThreshold) : 0.0;

            Window window = Window.NONE;
            int likelyCount = (shortLikely ? 1 : 0) + (mediumLikely ? 1 : 0) + (longLikely ? 1 : 0);
            if (likelyCount > 1) {
                window = Window.MULTI;
            } else if (shortLikely) {
                window = Window.SHORT;
            } else if (mediumLikely) {
                window = Window.MEDIUM;
            } else if (longLikely) {
                window = Window.LONG;
            }

            double confidence = Math.max(shortConfidence, Math.max(mediumConfidence, longConfidence));
            prediction.update(shortSlope, mediumSlope, longSlope, shortLikely, mediumLikely, longLikely, confidence,
                    window, shortReady, mediumReady, longReady, true);
            return prediction;
        }

        private void applyLearning(double adjustment) {
            double scaled = adjustment * learningScale;
            shortThreshold = clamp(baseShortThreshold + scaled, minThreshold, maxThreshold);
            mediumThreshold = clamp(baseMediumThreshold + scaled, minThreshold, maxThreshold);
            longThreshold = clamp(baseLongThreshold + scaled, minThreshold, maxThreshold);
        }

        private void reset() {
            shortSampleCount = 0;
            mediumSampleCount = 0;
            longSampleCount = 0;
            shortWriteIndex = 0;
            mediumWriteIndex = 0;
            longWriteIndex = 0;
            prediction.reset();
        }

        private String getTrendDescription() {
            FeaturePrediction current = evaluate();
            if (!current.ready()) {
                return "";
            }
            StringBuilder builder = new StringBuilder();
            builder.append(feature.name().toLowerCase());
            builder.append("[");
            boolean appended = false;
            if (current.shortReady()) {
                builder.append(formatTrend("short", current.shortSlope(), shortThreshold));
                appended = true;
            }
            if (current.mediumReady()) {
                if (appended) {
                    builder.append("/");
                }
                builder.append(formatTrend("medium", current.mediumSlope(), mediumThreshold));
                appended = true;
            }
            if (current.longReady()) {
                if (appended) {
                    builder.append("/");
                }
                builder.append(formatTrend("long", current.longSlope(), longThreshold));
            }
            builder.append("]");
            builder.append("(confidence:");
            builder.append(String.format("%.2f", current.confidence()));
            builder.append(")");
            return builder.toString();
        }
    }

    private static final class FeaturePrediction {
        private double shortSlope;
        private double mediumSlope;
        private double longSlope;
        private boolean shortLikely;
        private boolean mediumLikely;
        private boolean longLikely;
        private double confidence;
        private Window window = Window.NONE;
        private boolean ready;

        private boolean shortReady;
        private boolean mediumReady;
        private boolean longReady;

        private void update(double shortSlope, double mediumSlope, double longSlope, boolean shortLikely,
                boolean mediumLikely, boolean longLikely, double confidence, Window window, boolean shortReady,
                boolean mediumReady, boolean longReady, boolean ready) {
            this.shortSlope = shortSlope;
            this.mediumSlope = mediumSlope;
            this.longSlope = longSlope;
            this.shortLikely = shortLikely;
            this.mediumLikely = mediumLikely;
            this.longLikely = longLikely;
            this.confidence = confidence;
            this.window = window;
            this.shortReady = shortReady;
            this.mediumReady = mediumReady;
            this.longReady = longReady;
            this.ready = ready;
        }

        private void reset() {
            shortSlope = 0.0;
            mediumSlope = 0.0;
            longSlope = 0.0;
            shortLikely = false;
            mediumLikely = false;
            longLikely = false;
            confidence = 0.0;
            window = Window.NONE;
            shortReady = false;
            mediumReady = false;
            longReady = false;
            ready = false;
        }

        private boolean ready() {
            return ready;
        }

        private boolean isLikely() {
            return shortLikely || mediumLikely || longLikely;
        }

        private double confidence() {
            return confidence;
        }

        private Window window() {
            return window;
        }

        private double shortSlope() {
            return shortSlope;
        }

        private double mediumSlope() {
            return mediumSlope;
        }

        private double longSlope() {
            return longSlope;
        }

        private boolean shortReady() {
            return shortReady;
        }

        private boolean mediumReady() {
            return mediumReady;
        }

        private boolean longReady() {
            return longReady;
        }
    }
}
