package dev.nozh.core.matrix;

import dev.nozh.core.capability.CapabilityId;
import dev.nozh.core.capability.ProviderStatus;

/**
 * Confidence calculator (Contract 5).
 * 
 * Formula:
 * confidence = min(dataConfidence, historicalConfidence, providerConfidence)
 * * decayFactor
 * * antiFlappingPenalty
 * 
 * PURE - deterministic calculations only.
 */
public final class ConfidenceCalculator {

    private static final double DECAY_CONSTANT_MS = 300_000; // 5 minutes
    private static final double ANTI_FLAP_WINDOW_MS = 120_000; // 2 minutes
    private static final double ANTI_FLAP_PENALTY = 0.5;

    /**
     * Calculate confidence for an action.
     * 
     * @param dataConfidence        Confidence in perf data [0..1]
     * @param historicalSuccessRate Historical success rate [0..1]
     * @param providerStatus        Provider operational status
     * @param lastSuccessMillis     Last time this action succeeded
     * @param lastFailureMillis     Last time this action failed
     * @param environmentChanged    Whether environment (mods) changed since last
     *                              success
     * @param nowMillis             Current timestamp
     * @return Confidence score [0..1]
     */
    public double calculate(
            double dataConfidence,
            double historicalSuccessRate,
            ProviderStatus providerStatus,
            long lastSuccessMillis,
            long lastFailureMillis,
            boolean environmentChanged,
            long nowMillis) {
        // Provider confidence
        double providerConfidence = switch (providerStatus) {
            case HEALTHY -> 1.0;
            case DEGRADED -> 0.6;
            case BROKEN -> 0.0;
        };

        // Historical confidence (with environment decay)
        double historicConfidence = environmentChanged ? historicalSuccessRate * 0.5 : // Decay to 50% if env changed
                historicalSuccessRate;

        // Base confidence (min of all factors)
        double baseConfidence = Math.min(
                Math.min(dataConfidence, historicConfidence),
                providerConfidence);

        // Time decay (exponential decay since last success)
        double decayFactor = 1.0;
        if (lastSuccessMillis > 0) {
            long timeSinceSuccess = nowMillis - lastSuccessMillis;
            decayFactor = Math.exp(-timeSinceSuccess / DECAY_CONSTANT_MS);
        }

        // Anti-flapping penalty (if recently failed)
        double antiFlappingPenalty = 1.0;
        if (lastFailureMillis > 0) {
            long timeSinceFailure = nowMillis - lastFailureMillis;
            if (timeSinceFailure < ANTI_FLAP_WINDOW_MS) {
                antiFlappingPenalty = ANTI_FLAP_PENALTY;
            }
        }

        // Final confidence
        return baseConfidence * decayFactor * antiFlappingPenalty;
    }

    /**
     * Simple overload for testing with minimal params.
     */
    public double calculate(
            double dataConfidence,
            double historicalSuccessRate,
            ProviderStatus providerStatus) {
        return calculate(
                dataConfidence,
                historicalSuccessRate,
                providerStatus,
                0, // No time-based decay
                0, // No anti-flap penalty
                false,
                System.currentTimeMillis());
    }
}
