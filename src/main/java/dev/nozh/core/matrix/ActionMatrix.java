package dev.nozh.core.matrix;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;
import dev.nozh.core.capability.CapabilityProvider;
import dev.nozh.core.capability.ImpactLevel;
import dev.nozh.core.capability.ProviderMetadata;
import dev.nozh.core.capability.ProviderRegistry;
import dev.nozh.core.capability.ProviderStatus;
import dev.nozh.core.capability.SafetyLevel;
import dev.nozh.core.context.Scenario;
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
        this(registry, successTracker, confidenceCalculator, sessionLearning, null);
    }

    public ActionMatrix(
            ProviderRegistry registry,
            ActionSuccessTracker successTracker,
            ConfidenceCalculator confidenceCalculator,
            dev.nozh.core.intelligence.SessionLearning sessionLearning,
            dev.nozh.core.compatibility.ModConflictDetector conflictDetector) {
        this.registry = registry;
        this.successTracker = successTracker;
        this.confidenceCalculator = confidenceCalculator;
        this.sessionLearning = sessionLearning;
        this.conflictDetector = conflictDetector != null ? conflictDetector : createConflictDetectorSafe();
    }

    private static dev.nozh.core.compatibility.ModConflictDetector createConflictDetectorSafe() {
        try {
            return new dev.nozh.core.compatibility.ModConflictDetector();
        } catch (Throwable e) {
            // Tests don't have FabricLoader available
            return null;
        }
    }

    /**
     * Generate candidates for current state.
     * 
     * @param policy       Mode policy to enforce
     * @param currentBound Performance bound (CPU/GPU/BALANCED)
     * @param scenario     Current scenario (combat/building/etc.)
     * @return Sorted candidates (best first), may be empty
     */
    public List<ActionCandidate> generateCandidates(ModePolicy policy, String currentBound, Scenario scenario) {
        List<ActionCandidate> candidates = new ArrayList<>();
        List<ActionCandidate> yieldCandidates = new ArrayList<>();
        long now = System.currentTimeMillis();

        // Iterate all registered providers
        for (CapabilityProvider provider : registry.getAllProviders()) {
            CapabilityId id = provider.id();
            ProviderMetadata metadata = provider.metadata();

            // Filter by provider status
            if (provider.status() == ProviderStatus.BROKEN) {
                continue; // Skip broken providers
            }

            if (!provider.isAvailable()) {
                continue;
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
            CapabilityValue targetValue = generateTargetValue(id, currentBound, scenario);

            // Skip if no target value could be determined
            if (targetValue == null) {
                continue;
            }

            // INTEGRATION: Conflict Detection
            if (conflictDetector != null && conflictDetector.hasConflict(id)) {
                // If another mod handles this, we should not touch it
                yieldCandidates.add(ActionCandidate.yield(id, conflictDetector.getSteward(id)));
                continue;
            }

            // INTEGRATION: Session Learning
            if (sessionLearning.shouldAvoid(id, scenario)) {
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

        candidates.sort(Comparator
                .comparingDouble((ActionCandidate candidate) -> scoreCandidate(candidate, maxExpectedGain)).reversed());

        if (candidates.isEmpty() && !yieldCandidates.isEmpty()) {
            return List.of(yieldCandidates.get(0));
        }

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
    private CapabilityValue generateTargetValue(
            CapabilityId id,
            String bound,
            Scenario scenario) {
        boolean cpuBound = "CPU".equals(bound);
        boolean gpuBound = "GPU".equals(bound);
        boolean balanced = "BALANCED".equals(bound);

        boolean combat = scenario == Scenario.COMBAT;
        boolean mining = scenario == Scenario.MINING;
        boolean building = scenario == Scenario.BUILDING;
        boolean afk = scenario == Scenario.AFK;
        boolean menu = scenario == Scenario.MENU;
        boolean loading = scenario == Scenario.LOADING;

        switch (id) {
            case PARTICLES -> {
                if (combat || loading) {
                    return new CapabilityValue.EnumValue("MINIMAL");
                }
                if (cpuBound) {
                    return new CapabilityValue.EnumValue("MINIMAL");
                }
                if (gpuBound || balanced) {
                    return new CapabilityValue.EnumValue("DECREASED");
                }
            }
            case CLOUDS -> {
                if (combat || loading || cpuBound) {
                    return new CapabilityValue.EnumValue("OFF");
                }
                if (gpuBound || balanced) {
                    return new CapabilityValue.EnumValue("FAST");
                }
            }
            case ENTITY_SHADOWS -> {
                if (combat || gpuBound || cpuBound) {
                    return new CapabilityValue.EnumValue("OFF");
                }
            }
            case RENDER_DISTANCE -> {
                if (mining) {
                    return new CapabilityValue.IntValue(6);
                }
                if (gpuBound || combat) {
                    return new CapabilityValue.IntValue(8);
                }
                if (cpuBound) {
                    return new CapabilityValue.IntValue(10);
                }
            }
            case SIMULATION_DISTANCE -> {
                if (cpuBound || combat || mining) {
                    return new CapabilityValue.IntValue(6);
                }
                if (balanced && !building) {
                    return new CapabilityValue.IntValue(7);
                }
            }
            case ENTITY_DISTANCE -> {
                if (combat || afk || loading) {
                    return new CapabilityValue.IntValue(70);
                }
                if (gpuBound) {
                    return new CapabilityValue.IntValue(75);
                }
                if (cpuBound && !building) {
                    return new CapabilityValue.IntValue(80);
                }
            }
            case BIOME_BLEND -> {
                if (combat || gpuBound) {
                    return new CapabilityValue.IntValue(2);
                }
                if (cpuBound || balanced) {
                    return new CapabilityValue.IntValue(3);
                }
            }
            case MIPMAP_LEVEL -> {
                if (combat || gpuBound) {
                    return new CapabilityValue.IntValue(2);
                }
                if (balanced && !building) {
                    return new CapabilityValue.IntValue(3);
                }
            }
            case VSYNC -> {
                if (combat || loading || gpuBound) {
                    return new CapabilityValue.BoolValue(false);
                }
            }
            case FOG -> {
                if (combat || gpuBound) {
                    return new CapabilityValue.IntValue(8);
                }
                if (balanced && !building) {
                    return new CapabilityValue.IntValue(10);
                }
            }
            case GRAPHICS_MODE -> {
                if (combat || gpuBound) {
                    return new CapabilityValue.EnumValue("FAST");
                }
                if (balanced && !building) {
                    return new CapabilityValue.EnumValue("FAST");
                }
            }
            case ARMOR_STANDS -> {
                if (combat || afk || loading || (cpuBound && !building)) {
                    return new CapabilityValue.BoolValue(false);
                }
            }
            case ITEM_FRAMES -> {
                if (combat || afk || loading || (cpuBound && !building)) {
                    return new CapabilityValue.BoolValue(false);
                }
            }
            case BLOCK_ENTITIES -> {
                if (!building && (combat || afk || loading || cpuBound)) {
                    return new CapabilityValue.BoolValue(false);
                }
            }
            case ANIMATIONS -> {
                if (combat || afk || loading || (cpuBound && !building) || menu) {
                    return new CapabilityValue.BoolValue(false);
                }
            }
            default -> {
            }
        }

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
