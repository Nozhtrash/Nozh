package dev.nozh.core.context;

/**
 * Calculates confidence score for scenario detection.
 * 
 * Combines multiple signals:
 * - Signal count (more signals = higher confidence)
 * - Signal strength (strong signals boost confidence)
 * - Signal consistency (conflicting signals reduce confidence)
 * - Temporal stability (stable over time = higher confidence)
 * 
 * Output: confidence score 0.0-1.0
 * - 0.0-0.3 = low confidence (uncertain)
 * - 0.3-0.7 = medium confidence (likely correct)
 * - 0.7-1.0 = high confidence (very certain)
 * 
 * TASK 3: Scenario confidence - multi-signal scoring
 */
public final class ScenarioConfidenceCalculator {

    private static final double BASE_CONFIDENCE = 0.5;
    private static final double STRONG_SIGNAL_BONUS = 0.15;
    private static final double WEAK_SIGNAL_BONUS = 0.05;
    private static final double CONFLICT_PENALTY = 0.10;
    private static final double STABILITY_BONUS = 0.20;

    /**
     * Calculate confidence based on signal analysis.
     * 
     * @param strongSignals Number of strong supporting signals
     * @param weakSignals Number of weak supporting signals
     * @param conflictingSignals Number of contradicting signals
     * @param stabilityFactor Temporal stability (0.0-1.0)
     * @return Confidence score (0.0-1.0)
     */
    public static double calculate(
            int strongSignals,
            int weakSignals,
            int conflictingSignals,
            double stabilityFactor) {

        double confidence = BASE_CONFIDENCE;

        // Add bonus for supporting signals
        confidence += strongSignals * STRONG_SIGNAL_BONUS;
        confidence += weakSignals * WEAK_SIGNAL_BONUS;

        // Subtract penalty for conflicts
        confidence -= conflictingSignals * CONFLICT_PENALTY;

        // Add stability bonus
        confidence += stabilityFactor * STABILITY_BONUS;

        // Clamp to [0.0, 1.0]
        return Math.max(0.0, Math.min(1.0, confidence));
    }

    /**
     * Calculate confidence for scenario with detailed weights.
     */
    public static double calculateWeighted(
            ScenarioSignal[] signals,
            double stabilityFactor) {

        if (signals == null || signals.length == 0) {
            return BASE_CONFIDENCE;
        }

        double totalWeight = 0.0;
        double weightedSum = 0.0;

        for (ScenarioSignal signal : signals) {
            weightedSum += signal.strength * signal.weight * signal.confidence;
            totalWeight += signal.weight;
        }

        double baseScore = totalWeight > 0 ? weightedSum / totalWeight : BASE_CONFIDENCE;

        // Apply stability multiplier
        double finalScore = baseScore * (0.7 + 0.3 * stabilityFactor);

        return Math.max(0.0, Math.min(1.0, finalScore));
    }

    /**
     * Represents a signal contributing to scenario detection.
     */
    public static class ScenarioSignal {
        public final String name;
        public final double strength; // 0.0-1.0
        public final double weight; // relative importance
        public final double confidence; // how reliable this signal is

        public ScenarioSignal(String name, double strength, double weight, double confidence) {
            this.name = name;
            this.strength = Math.max(0.0, Math.min(1.0, strength));
            this.weight = Math.max(0.0, weight);
            this.confidence = Math.max(0.0, Math.min(1.0, confidence));
        }
    }

    /**
     * Helper: Create strong signal.
     */
    public static ScenarioSignal strongSignal(String name, double strength) {
        return new ScenarioSignal(name, strength, 1.0, 0.9);
    }

    /**
     * Helper: Create weak signal.
     */
    public static ScenarioSignal weakSignal(String name, double strength) {
        return new ScenarioSignal(name, strength, 0.5, 0.6);
    }
}
