package dev.nozh.core.governor;

import dev.nozh.core.context.Scenario;

/**
 * Detailed reasoning for a governor decision.
 * 
 * Used for logging, debugging, and learning analysis.
 * 
 * @author Nozh Team
 * @since 0.4.0
 */
public record DecisionReasoning(
    Scenario scenario,
    double currentFps,
    double targetFps,
    double utilityScore,
    double qValue,
    boolean predictedDrop,
    int spikeCount,
    String rationale
) {
    
    /**
     * Create reasoning with automatic rationale generation.
     */
    public static DecisionReasoning create(
            Scenario scenario,
            double currentFps,
            double targetFps,
            double utilityScore,
            double qValue,
            boolean predictedDrop,
            int spikeCount) {
        
        StringBuilder rationale = new StringBuilder();
        rationale.append("Scenario=").append(scenario);
        rationale.append(", FPS=").append(String.format("%.1f", currentFps));
        rationale.append("/").append(String.format("%.0f", targetFps));
        rationale.append(", Utility=").append(String.format("%.2f", utilityScore));
        rationale.append(", Q=").append(String.format("%.2f", qValue));
        
        if (predictedDrop) {
            rationale.append(", PREDICTED_DROP");
        }
        
        if (spikeCount > 0) {
            rationale.append(", Spikes=").append(spikeCount);
        }
        
        return new DecisionReasoning(
            scenario,
            currentFps,
            targetFps,
            utilityScore,
            qValue,
            predictedDrop,
            spikeCount,
            rationale.toString()
        );
    }
    
    @Override
    public String toString() {
        return rationale;
    }
}