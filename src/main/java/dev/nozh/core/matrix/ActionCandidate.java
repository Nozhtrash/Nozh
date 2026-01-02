package dev.nozh.core.matrix;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;
import dev.nozh.core.capability.ImpactLevel;
import dev.nozh.core.capability.RollbackGuarantee;
import dev.nozh.core.capability.SafetyLevel;

/**
 * Action candidate for governor decision-making (Contract 5).
 * 
 * Represents a single possible action the governor might take.
 * Immutable, sortable by confidence/expected gain.
 */
public record ActionCandidate(
        CapabilityId capabilityId,
        CapabilityValue targetValue,
        int tier, // 0-3
        double expectedGainMs,
        SafetyLevel safetyLevel,
        RollbackGuarantee rollbackGuarantee,
        ImpactLevel gameplayImpact,
        ImpactLevel visualImpact,
        double confidenceScore, // [0..1]
        String reason // Human-readable explanation
) {
    /**
     * Score for sorting candidates.
     * Higher is better.
     */
    public double score() {
        return confidenceScore * expectedGainMs;
    }
}
