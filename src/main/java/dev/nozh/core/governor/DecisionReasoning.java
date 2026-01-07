package dev.nozh.core.governor;

import java.util.ArrayList;
import java.util.List;

/**
 * Captures reasoning behind a governor decision.
 * 
 * Makes the governor explainable by tracking:
 * - Why was this action suggested?
 * - What signals triggered it?
 * - What was the expected outcome?
 * - What alternatives were considered?
 * 
 * TASK 6: Explainable decisions - transparency
 */
public final class DecisionReasoning {

    private final String actionId;
    private final String scenario;
    private final List<String> triggers = new ArrayList<>();
    private final List<String> signals = new ArrayList<>();
    private final String expectedOutcome;
    private final List<String> alternatives = new ArrayList<>();
    private final double confidenceScore;
    private final long timestamp;

    private DecisionReasoning(Builder builder) {
        this.actionId = builder.actionId;
        this.scenario = builder.scenario;
        this.triggers.addAll(builder.triggers);
        this.signals.addAll(builder.signals);
        this.expectedOutcome = builder.expectedOutcome;
        this.alternatives.addAll(builder.alternatives);
        this.confidenceScore = builder.confidenceScore;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Get action ID.
     */
    public String getActionId() {
        return actionId;
    }

    /**
     * Get detected scenario.
     */
    public String getScenario() {
        return scenario;
    }

    /**
     * Get list of triggers.
     */
    public List<String> getTriggers() {
        return new ArrayList<>(triggers);
    }

    /**
     * Get list of signals.
     */
    public List<String> getSignals() {
        return new ArrayList<>(signals);
    }

    /**
     * Get expected outcome.
     */
    public String getExpectedOutcome() {
        return expectedOutcome;
    }

    /**
     * Get alternatives considered.
     */
    public List<String> getAlternatives() {
        return new ArrayList<>(alternatives);
    }

    /**
     * Get confidence score.
     */
    public double getConfidenceScore() {
        return confidenceScore;
    }

    /**
     * Get timestamp.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Format as human-readable explanation.
     */
    public String toExplanation() {
        StringBuilder sb = new StringBuilder();
        sb.append("[DECISION] ").append(actionId).append("\n");
        sb.append("Scenario: ").append(scenario).append("\n");
        sb.append("Confidence: ").append(String.format("%.1f%%", confidenceScore * 100)).append("\n");
        
        if (!triggers.isEmpty()) {
            sb.append("Triggers:\n");
            for (String trigger : triggers) {
                sb.append("  - ").append(trigger).append("\n");
            }
        }

        if (!signals.isEmpty()) {
            sb.append("Signals:\n");
            for (String signal : signals) {
                sb.append("  - ").append(signal).append("\n");
            }
        }

        sb.append("Expected: ").append(expectedOutcome).append("\n");

        if (!alternatives.isEmpty()) {
            sb.append("Alternatives considered: ").append(alternatives.size()).append("\n");
        }

        return sb.toString();
    }

    /**
     * Builder for DecisionReasoning.
     */
    public static class Builder {
        private String actionId = "unknown";
        private String scenario = "STANDARD";
        private final List<String> triggers = new ArrayList<>();
        private final List<String> signals = new ArrayList<>();
        private String expectedOutcome = "Unknown outcome";
        private final List<String> alternatives = new ArrayList<>();
        private double confidenceScore = 0.5;

        public Builder actionId(String actionId) {
            this.actionId = actionId;
            return this;
        }

        public Builder scenario(String scenario) {
            this.scenario = scenario;
            return this;
        }

        public Builder addTrigger(String trigger) {
            this.triggers.add(trigger);
            return this;
        }

        public Builder addSignal(String signal) {
            this.signals.add(signal);
            return this;
        }

        public Builder expectedOutcome(String outcome) {
            this.expectedOutcome = outcome;
            return this;
        }

        public Builder addAlternative(String alternative) {
            this.alternatives.add(alternative);
            return this;
        }

        public Builder confidenceScore(double score) {
            this.confidenceScore = Math.max(0.0, Math.min(1.0, score));
            return this;
        }

        public DecisionReasoning build() {
            return new DecisionReasoning(this);
        }
    }
}
