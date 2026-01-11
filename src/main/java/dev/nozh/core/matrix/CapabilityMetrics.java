package dev.nozh.core.matrix;

/**
 * Metrics for capability performance impact analysis.
 * 
 * Used to evaluate the cost/benefit of applying a capability change.
 * 
 * PRIORITY 3: Advanced metrics for optimization decisions.
 */
public record CapabilityMetrics(
    double expectedGainMs,
    double visualCost,
    double gameplayCost,
    double confidence
) {
    public static CapabilityMetrics create(double gain, double visual, double gameplay) {
        return new CapabilityMetrics(gain, visual, gameplay, 0.5);
    }
    
    /**
     * Calculate efficiency as gain divided by total cost.
     * Higher is better.
     */
    public double efficiency() {
        return expectedGainMs / (visualCost + gameplayCost + 0.1);
    }
    
    /**
     * Get weighted efficiency (efficiency * confidence).
     */
    public double weightedEfficiency() { return efficiency() * confidence; }
    
    /**
     * Get total cost (visual + gameplay).
     */
    public double totalCost() { return visualCost + gameplayCost; }
    
    /**
     * Check if this is a low-cost change (< 5.0).
     */
    public boolean isLowCost() { return totalCost() < 5.0; }
    
    /**
     * Check if this is a high-impact change (> 2.0ms gain).
     */
    public boolean isHighImpact() { return expectedGainMs > 2.0; }
    
    /**
     * Create a new metrics with updated confidence.
     */
    public CapabilityMetrics withConfidence(double newConf) {
        return new CapabilityMetrics(expectedGainMs, visualCost, gameplayCost, 
            Math.max(0.0, Math.min(1.0, newConf)));
    }
    
    /**
     * Validate metrics ranges.
     * Throws IllegalArgumentException if invalid.
     */
    public void validate() {
        if (expectedGainMs < 0) throw new IllegalArgumentException("Gain cannot be negative");
        if (visualCost < 0 || visualCost > 10) throw new IllegalArgumentException("Visual cost must be 0-10");
        if (gameplayCost < 0 || gameplayCost > 10) throw new IllegalArgumentException("Gameplay cost must be 0-10");
        if (confidence < 0 || confidence > 1) throw new IllegalArgumentException("Confidence must be 0-1");
    }
}
