package dev.nozh.api.governor;

/**
 * Immutable proof of intent.
 * Represents a decision made by the Governor.
 */
public record Decision(
        ActionType type,
        ReasonCode reasonCode,
        DecisionSeverity severity,
        DecisionConfidence confidence,
        ActionScope scope,
        String explanation,
        long timestampMillis) {
    /**
     * Factory for a "do nothing" decision.
     */
    public static Decision none(long timestampMillis, ReasonCode code, String explanation) {
        return new Decision(
                ActionType.NONE,
                code,
                DecisionSeverity.INFO,
                DecisionConfidence.HIGH,
                ActionScope.SESSION,
                explanation,
                timestampMillis);
    }
}
