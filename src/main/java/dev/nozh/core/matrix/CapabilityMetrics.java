package dev.nozh.core.matrix;

/**
 * Metrics for capability optimization decisions.
 * 
 * Tracks expected performance gain vs visual/gameplay costs
 * to enable intelligent prioritization of actions.
 * 
 * PRIORITY 3 - Gain/Cost Optimization (Task 6)
 */
public record CapabilityMetrics(
    double expectedGainMs,    // Expected performance improvement in ms
    double visualCost,        // 0-10 visual quality loss
    double gameplayCost,      // 0-10 gameplay impact
    double confidence         // 0-1 confidence based on learning
) {
    /**
     * Create metrics with default confidence.
     * 
     * @param expectedGainMs Expected performance gain in ms
     * @param visualCost Visual quality cost (0-10)
     * @param gameplayCost Gameplay impact cost (0-10)
     * @return New CapabilityMetrics with 0.5 confidence
     */
    public static CapabilityMetrics create(
            double expectedGainMs,
            double visualCost,
            double gameplayCost) {
        return new CapabilityMetrics(expectedGainMs, visualCost, gameplayCost, 0.5);
    }

    /**
     * Calculate efficiency score.
     * 
     * Higher efficiency means better gain/cost ratio.
     * Used for ranking actions in ActionMatrix.
     * 
     * Formula: expectedGainMs / (visualCost + gameplayCost + 0.1)
     * The 0.1 prevents division by zero.
     * 
     * @return Efficiency score (higher is better)
     */
    public double efficiency() {
        double totalCost = visualCost + gameplayCost + 0.1;
        return expectedGainMs / totalCost;
    }

    /**
     * Calculate weighted efficiency considering confidence.
     * 
     * Actions with higher confidence are preferred even if
     * efficiency is slightly lower.
     * 
     * @return Confidence-weighted efficiency score
     */
    public double weightedEfficiency() {
        return efficiency() * confidence;
    }

    /**
     * Get total cost (visual + gameplay).
     * 
     * @return Sum of visual and gameplay costs
     */
    public double totalCost() {
        return visualCost + gameplayCost;
    }

    /**
     * Check if this is a low-cost action.
     * Low-cost means total cost < 5.0.
     * 
     * @return true if total cost is low
     */
    public boolean isLowCost() {
        return totalCost() < 5.0;
    }

    /**
     * Check if this is a high-impact action.
     * High-impact means expected gain > 2ms.
     * 
     * @return true if expected gain is high
     */
    public boolean isHighImpact() {
        return expectedGainMs > 2.0;
    }

    /**
     * Create new metrics with updated confidence.
     * 
     * @param newConfidence New confidence value (0-1)
     * @return New CapabilityMetrics with updated confidence
     */
    public CapabilityMetrics withConfidence(double newConfidence) {
        return new CapabilityMetrics(
            expectedGainMs,
            visualCost,
            gameplayCost,
            Math.max(0.0, Math.min(1.0, newConfidence))
        );
    }

    /**
     * Create new metrics with updated expected gain.
     * 
     * @param newGainMs New expected gain in ms
     * @return New CapabilityMetrics with updated gain
     */
    public CapabilityMetrics withExpectedGain(double newGainMs) {
        return new CapabilityMetrics(
            Math.max(0.0, newGainMs),
            visualCost,
            gameplayCost,
            confidence
        );
    }

    /**
     * Get formatted string for logging/display.
     * 
     * @return Formatted string like "Gain: 3.5ms, Cost: V=2.0 G=1.0, Eff: 1.17"
     */
    public String toDisplayString() {
        return String.format(
            "Gain: %.1fms, Cost: V=%.1f G=%.1f, Eff: %.2f (conf: %.2f)",
            expectedGainMs,
            visualCost,
            gameplayCost,
            efficiency(),
            confidence
        );
    }

    /**
     * Validate metrics are within expected ranges.
     * 
     * @throws IllegalArgumentException if metrics are invalid
     */
    public void validate() {
        if (expectedGainMs < 0.0) {
            throw new IllegalArgumentException("Expected gain cannot be negative: " + expectedGainMs);
        }
        if (visualCost < 0.0 || visualCost > 10.0) {
            throw new IllegalArgumentException("Visual cost must be 0-10: " + visualCost);
        }
        if (gameplayCost < 0.0 || gameplayCost > 10.0) {
            throw new IllegalArgumentException("Gameplay cost must be 0-10: " + gameplayCost);
        }
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be 0-1: " + confidence);
        }
    }
}
