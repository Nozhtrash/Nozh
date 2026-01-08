package dev.nozh.core.governor;

import dev.nozh.NozhConstants;
import dev.nozh.core.context.Scenario;

/**
 * Logs governor decisions for transparency and debugging.
 * 
 * @author Nozh Team
 * @since 0.4.0
 */
public final class DecisionLogger {

    private DecisionLogger() {
        // Utility class
    }

    /**
     * Log a decision with full reasoning.
     * 
     * @param actionId action taken
     * @param reasoning decision reasoning
     */
    public static void logDecision(String actionId, DecisionReasoning reasoning) {
        if (actionId == null || reasoning == null) {
            return;
        }

        // FIX: Use toString() instead of non-existent toExplanation()
        // toString() returns the rationale field
        NozhConstants.LOGGER.info("GOVERNOR DECISION: [{}] - {}",
            actionId,
            reasoning.toString()
        );
    }

    /**
     * Log decision with context.
     */
    public static void logDecisionWithContext(
            String actionId,
            Scenario scenario,
            double currentFps,
            double targetFps,
            DecisionReasoning reasoning) {
        
        if (actionId == null) {
            return;
        }

        NozhConstants.LOGGER.info(
            "GOVERNOR: {} | Scenario={} | FPS={}/{} | Reasoning: {}",
            actionId,
            scenario,
            String.format("%.1f", currentFps),
            String.format("%.0f", targetFps),
            reasoning != null ? reasoning.toString() : "N/A"
        );
    }

    /**
     * Log decision failure.
     */
    public static void logDecisionFailure(String reason) {
        NozhConstants.LOGGER.warn("GOVERNOR: Decision failed - {}", reason);
    }
}
