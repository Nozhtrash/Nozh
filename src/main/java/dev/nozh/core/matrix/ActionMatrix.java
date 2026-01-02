package dev.nozh.core.matrix;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;
import dev.nozh.core.capability.CapabilityProvider;
import dev.nozh.core.capability.ImpactLevel;
import dev.nozh.core.capability.ProviderMetadata;
import dev.nozh.core.capability.ProviderRegistry;
import dev.nozh.core.capability.ProviderStatus;
import dev.nozh.core.capability.SafetyLevel;
import dev.nozh.core.governor.ModePolicy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Action matrix generator (Contract 6).
 * 
 * Generates and filters action candidates based on:
 * - Provider availability
 * - Mode policy
 * - Confidence thresholds
 * - Performance bound
 * 
 * PURE - no MC dependencies.
 */
public final class ActionMatrix {

    private static final double CONFIDENCE_WEIGHT = 0.65;
    private static final double EXPECTED_GAIN_WEIGHT = 0.35;

    private final ProviderRegistry registry;
    private final ActionSuccessTracker successTracker;
    private final ConfidenceCalculator confidenceCalculator;
    private final dev.nozh.core.compatibility.ModConflictDetector conflictDetector;
    private final dev.nozh.core.intelligence.SessionLearning sessionLearning;

    public ActionMatrix(
            ProviderRegistry registry,
            ActionSuccessTracker successTracker,
            ConfidenceCalculator confidenceCalculator,
            dev.nozh.core.intelligence.SessionLearning sessionLearning) {
        this.registry = registry;
        this.successTracker = successTracker;
        this.confidenceCalculator = confidenceCalculator;
        this.sessionLearning = sessionLearning;
        this.conflictDetector = new dev.nozh.core.compatibility.ModConflictDetector();
    }

    /**
     * Generate candidates for current state.
     * 
     * @param policy       Mode policy to enforce
     * @param currentBound Performance bound (CPU_BOUND/GPU_BOUND/BALANCED)
     * @return Sorted candidates (best first), may be empty
     */
    public List<ActionCandidate> generateCandidates(ModePolicy policy, String currentBound) {
        List<ActionCandidate> candidates = new ArrayList<>();
        long now = System.currentTimeMillis();

        // Iterate all registered providers
        for (CapabilityProvider provider : registry.getAllProviders()) {
            CapabilityId id = provider.id();
            ProviderMetadata metadata = provider.metadata();

            // Filter by provider status
            if (provider.status() == ProviderStatus.BROKEN) {
                continue; // Skip broken providers
            }

            // Filter by safety level
            if (!policy.allowExperimental() &&
                    metadata.safetyLevel() == SafetyLevel.EXPERIMENTAL) {
                continue;
            }

            // Calculate confidence
            double historicalSuccess = successTracker.getSuccessRate(id);
            long lastSuccess = successTracker.getLastSuccessMillis(id);
            long lastFailure = successTracker.getLastFailureMillis(id);
            boolean envChanged = successTracker.isEnvironmentChanged(id);

            double confidence = confidenceCalculator.calculate(
                    1.0, // Assume data confidence = 1.0 for now
                    historicalSuccess,
                    provider.status(),
                    lastSuccess,
                    lastFailure,
                    envChanged,
                    now);

            // Filter by confidence threshold
            if (confidence < policy.minConfidence()) {
                continue;
            }

            // Generate target value based on bound + provider type
            String targetValueStr = generateTargetValue(id, currentBound, metadata);

            // Skip if no target value could be determined
            if (targetValueStr == null) {
                continue;
            }

            // Create CapabilityValue from string (most values are enums)
            CapabilityValue targetValue = new CapabilityValue.EnumValue(targetValueStr);

            // INTEGRATION: Conflict Detection
            if (conflictDetector.hasConflict(id)) {
                // If another mod handles this, we should not touch it
                continue;
            }

            // INTEGRATION: Session Learning
            if (sessionLearning.shouldAvoid(id)) {
                // If this action didn't help previously, skip it
                continue;
            }

            // Skip if no expected gain
            if (metadata.expectedGainMs() <= 0) {
                continue;
            }

            // Create candidate with determined target value
            ActionCandidate candidate = new ActionCandidate(
                    id,
                    targetValue, // Capability value object
                    determineTier(metadata),
                    metadata.expectedGainMs(),
                    metadata.safetyLevel(),
                    metadata.rollbackGuarantee(),
                    metadata.gameplayImpact(),
                    metadata.visualImpact(),
                    confidence,
                    generateReason(id, confidence, metadata));

            // Filter by allowed tiers
            if (!policy.allowedTiers().contains(candidate.tier())) {
                continue;
            }

            candidates.add(candidate);
        }

        // Sort by weighted score (descending)
        double maxExpectedGain = candidates.stream()
                .mapToDouble(ActionCandidate::expectedGainMs)
                .max()
                .orElse(0.0);

        candidates.sort(Comparator.comparingDouble(candidate -> scoreCandidate(candidate, maxExpectedGain)).reversed());

        return candidates;
    }

    private int determineTier(ProviderMetadata metadata) {
        // Simple tier logic based on impact
        if (metadata.gameplayImpact() == ImpactLevel.HIGH ||
                metadata.visualImpact() == ImpactLevel.HIGH) {
            return 3; // High impact = Tier 3
        } else if (metadata.gameplayImpact() == ImpactLevel.MED ||
                metadata.visualImpact() == ImpactLevel.MED) {
            return 2; // Medium impact = Tier 2
        } else if (metadata.gameplayImpact() == ImpactLevel.LOW ||
                metadata.visualImpact() == ImpactLevel.LOW) {
            return 1; // Low impact = Tier 1
        } else {
            return 0; // No impact = Tier 0
        }
    }

    /**
     * Generate target value based on bound + provider type.
     * 
     * Simple heuristics for v0.2-beta:
     * - CPU bound → reduce particles/clouds
     * - GPU bound → reduce render distance/shadows
     * - BALANCED → conservative reductions
     */
    private String generateTargetValue(CapabilityId id, String bound, ProviderMetadata metadata) {
        String capName = id.name();

        // CPU-bound heuristics
        if ("CPU".equals(bound)) {
            if ("particles".equals(capName))
                return "MINIMAL";
            if ("clouds".equals(capName))
                return "OFF";
            if ("entity_shadows".equals(capName))
                return "OFF";
        }

        // GPU-bound heuristics
        if ("GPU".equals(bound)) {
            if ("render_distance".equals(capName))
                return "8";
            if ("entity_shadows".equals(capName))
                return "OFF";
            if ("particles".equals(capName))
                return "DECREASED";
        }

        // BALANCED: conservative reductions
        if ("BALANCED".equals(bound)) {
            if ("particles".equals(capName))
                return "DECREASED";
            if ("clouds".equals(capName))
                return "FAST";
        }

        // No clear target
        return null;
    }

    private String generateReason(CapabilityId id, double confidence, ProviderMetadata metadata) {
        return String.format("%s → reduce (confidence: %.2f, expected gain: %.2fms)",
                id.name(), confidence, metadata.expectedGainMs());
    }

    private double scoreCandidate(ActionCandidate candidate, double maxExpectedGain) {
        double normalizedGain = maxExpectedGain > 0 ? candidate.expectedGainMs() / maxExpectedGain : 0.0;
        return (candidate.confidenceScore() * CONFIDENCE_WEIGHT) + (normalizedGain * EXPECTED_GAIN_WEIGHT);
    }
}
