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
import dev.nozh.core.governor.OptimizationProfile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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
    private final dev.nozh.core.compatibility.CompatibilityMatrix compatibilityMatrix;
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
            dev.nozh.core.compatibility.CompatibilityMatrix compatibilityMatrix) {
        this.registry = registry;
        this.successTracker = successTracker;
        this.confidenceCalculator = confidenceCalculator;
        this.sessionLearning = sessionLearning;
        this.compatibilityMatrix = compatibilityMatrix != null ? compatibilityMatrix : createCompatibilityMatrixSafe();
    }

    private static dev.nozh.core.compatibility.CompatibilityMatrix createCompatibilityMatrixSafe() {
        try {
            return new dev.nozh.core.compatibility.CompatibilityMatrix();
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
    public List<ActionCandidate> generateCandidates(
            ModePolicy policy,
            String currentBound,
            Scenario scenario,
            OptimizationProfile profile) {
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

            if (compatibilityMatrix != null && compatibilityMatrix.isBlockedByDependencies(metadata)) {
                yieldCandidates.add(ActionCandidate.yield(id,
                        compatibilityMatrix.getDependencySteward(metadata)));
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
            CapabilityValue targetValue = generateTargetValue(id, currentBound, scenario, profile);

            // Skip if no target value could be determined
            if (targetValue == null) {
                continue;
            }

            // INTEGRATION: Conflict Detection
            if (compatibilityMatrix != null && compatibilityMatrix.isExternallyManaged(id, metadata)) {
                // If another mod handles this, we should not touch it
                yieldCandidates.add(ActionCandidate.yield(id, compatibilityMatrix.getSteward(id)));
                continue;
            }

            // INTEGRATION: Session Learning
            if (sessionLearning.shouldAvoid(id, scenario)) {
                // If this action didn't help previously, skip it
                continue;
            }

            double priorityMultiplier = compatibilityMatrix != null
                    ? compatibilityMatrix.getPriorityMultiplier(id)
                    : 1.0;
            double adjustedExpectedGain = metadata.expectedGainMs() * priorityMultiplier;

            // Skip if no expected gain
            if (adjustedExpectedGain <= 0) {
                continue;
            }

            // Create candidate with determined target value
            ActionCandidate candidate = new ActionCandidate(
                    id,
                    targetValue, // Capability value object
                    determineTier(metadata),
                    adjustedExpectedGain,
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

    public List<ActionCandidate> generateReverseCandidates(
            ModePolicy policy,
            Scenario scenario,
            OptimizationProfile profile,
            Map<CapabilityId, CapabilityValue> baselineSettings,
            Map<CapabilityId, CapabilityValue> currentSettings,
            Map<CapabilityId, Long> lastChangeMillis,
            long cooldownMillis,
            long nowMillis) {
        List<ActionCandidate> candidates = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (CapabilityProvider provider : registry.getAllProviders()) {
            CapabilityId id = provider.id();
            ProviderMetadata metadata = provider.metadata();

            if (!policy.allowedTiers().contains(determineTier(metadata))) {
                continue;
            }

            CapabilityValue baseline = baselineSettings.get(id);
            CapabilityValue current = currentSettings.get(id);
            if (baseline == null || current == null) {
                continue;
            }

            if (!isQualityIncrease(id, current, baseline)) {
                continue;
            }

            if (compatibilityMatrix != null && compatibilityMatrix.isExternallyManaged(id, metadata)) {
                continue;
            }
            if (lastChangeMillis != null) {
                long lastChange = lastChangeMillis.getOrDefault(id, 0L);
                if (lastChange > 0 && nowMillis - lastChange < cooldownMillis) {
                    continue;
                }
            }

            double historicalSuccess = successTracker.getSuccessRate(id);
            long lastSuccess = successTracker.getLastSuccessMillis(id);
            long lastFailure = successTracker.getLastFailureMillis(id);
            boolean envChanged = successTracker.isEnvironmentChanged(id);

            double confidence = confidenceCalculator.calculate(
                    1.0,
                    historicalSuccess,
                    provider.status(),
                    lastSuccess,
                    lastFailure,
                    envChanged,
                    now);

            if (confidence < policy.minConfidence()) {
                continue;
            }

            candidates.add(new ActionCandidate(
                    id,
                    baseline,
                    determineTier(metadata),
                    0.0,
                    metadata.safetyLevel(),
                    metadata.rollbackGuarantee(),
                    metadata.gameplayImpact(),
                    metadata.visualImpact(),
                    confidence,
                    String.format("%s → restore baseline (%s profile)", id.name(), profile.name())));
        }

        candidates.sort(Comparator
                .comparingInt(ActionCandidate::tier)
                .thenComparing(Comparator.comparingDouble(ActionCandidate::confidenceScore).reversed()));

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
            Scenario scenario,
            OptimizationProfile profile) {
        boolean cpuBound = "CPU".equals(bound);
        boolean gpuBound = "GPU".equals(bound);
        boolean balanced = "BALANCED".equals(bound);
        boolean aggressive = profile != null && profile.isAggressive();

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
                    return new CapabilityValue.EnumValue(aggressive ? "MINIMAL" : "DECREASED");
                }
                if (gpuBound || balanced) {
                    return new CapabilityValue.EnumValue(aggressive ? "MINIMAL" : "DECREASED");
                }
            }
            case CLOUDS -> {
                if (combat || loading || cpuBound) {
                    return new CapabilityValue.EnumValue("OFF");
                }
                if (gpuBound || balanced) {
                    return new CapabilityValue.EnumValue(aggressive ? "OFF" : "FAST");
                }
            }
            case ENTITY_SHADOWS -> {
                if (combat || gpuBound || cpuBound) {
                    return new CapabilityValue.BoolValue(false);
                }
            }
            case RENDER_DISTANCE -> {
                if (mining) {
                    return new CapabilityValue.IntValue(aggressive ? 4 : 6);
                }
                if (gpuBound || combat) {
                    return new CapabilityValue.IntValue(aggressive ? 6 : 8);
                }
                if (cpuBound) {
                    return new CapabilityValue.IntValue(aggressive ? 8 : 10);
                }
            }
            case SIMULATION_DISTANCE -> {
                if (cpuBound || combat || mining) {
                    return new CapabilityValue.IntValue(aggressive ? 4 : 6);
                }
                if (balanced && !building) {
                    return new CapabilityValue.IntValue(aggressive ? 5 : 7);
                }
            }
            case ENTITY_DISTANCE -> {
                if (combat || afk || loading) {
                    return new CapabilityValue.IntValue(aggressive ? 60 : 70);
                }
                if (gpuBound) {
                    return new CapabilityValue.IntValue(aggressive ? 60 : 75);
                }
                if (cpuBound && !building) {
                    return new CapabilityValue.IntValue(aggressive ? 65 : 80);
                }
            }
            case BIOME_BLEND -> {
                if (combat || gpuBound) {
                    return new CapabilityValue.IntValue(aggressive ? 1 : 2);
                }
                if (cpuBound || balanced) {
                    return new CapabilityValue.IntValue(aggressive ? 2 : 3);
                }
            }
            case MIPMAP_LEVEL -> {
                if (combat || gpuBound) {
                    return new CapabilityValue.IntValue(aggressive ? 1 : 2);
                }
                if (balanced && !building) {
                    return new CapabilityValue.IntValue(aggressive ? 2 : 3);
                }
            }
            case VSYNC -> {
                if (combat || loading || gpuBound) {
                    return new CapabilityValue.BoolValue(false);
                }
            }
            case FOG -> {
                if (combat || gpuBound) {
                    return new CapabilityValue.IntValue(aggressive ? 6 : 8);
                }
                if (balanced && !building) {
                    return new CapabilityValue.IntValue(aggressive ? 8 : 10);
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
            case SMOOTH_LIGHTING -> {
                if (combat || gpuBound || cpuBound) {
                    return new CapabilityValue.EnumValue("OFF");
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
            case FPS_CAP -> {
                if (menu || afk) {
                    return new CapabilityValue.IntValue(60);
                }
                if (gpuBound && !combat) {
                    return new CapabilityValue.IntValue(90);
                }
            }
            case RESOLUTION_SCALE -> {
                if (gpuBound || loading) {
                    return new CapabilityValue.FloatValue(aggressive ? 0.6f : 0.8f);
                }
            }
            case DISTORTION_EFFECT_SCALE -> {
                if (gpuBound || loading) {
                    return new CapabilityValue.FloatValue(aggressive ? 0.0f : 0.5f);
                }
            }
            case DYNAMIC_LIGHTING -> {
                if (combat || gpuBound || cpuBound || loading) {
                    return new CapabilityValue.BoolValue(false);
                }
            }
            case CHUNK_LOADING -> {
                if (loading || cpuBound) {
                    return new CapabilityValue.EnumValue(aggressive ? "AGGRESSIVE" : "BALANCED");
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

    private boolean isQualityIncrease(CapabilityId id, CapabilityValue current, CapabilityValue baseline) {
        if (current.equals(baseline)) {
            return false;
        }
        return switch (id) {
            case PARTICLES -> compareEnum(current, baseline, List.of("MINIMAL", "DECREASED", "ALL"));
            case CLOUDS -> compareEnum(current, baseline, List.of("OFF", "FAST", "FANCY"));
            case GRAPHICS_MODE -> compareEnum(current, baseline, List.of("FAST", "FANCY", "FABULOUS"));
            case ENTITY_SHADOWS, ARMOR_STANDS, ITEM_FRAMES, BLOCK_ENTITIES, ANIMATIONS, VSYNC, DYNAMIC_LIGHTING ->
                    compareBool(current, baseline);
            case RENDER_DISTANCE, SIMULATION_DISTANCE, ENTITY_DISTANCE, BIOME_BLEND, MIPMAP_LEVEL, FOG ->
                    compareInt(current, baseline);
            case RESOLUTION_SCALE, DISTORTION_EFFECT_SCALE -> compareFloat(current, baseline);
            default -> false;
        };
    }

    private boolean compareEnum(CapabilityValue current, CapabilityValue baseline, List<String> ordering) {
        if (!(current instanceof CapabilityValue.EnumValue currentEnum)
                || !(baseline instanceof CapabilityValue.EnumValue baselineEnum)) {
            return false;
        }
        int currentIndex = ordering.indexOf(currentEnum.name());
        int baselineIndex = ordering.indexOf(baselineEnum.name());
        if (currentIndex < 0 || baselineIndex < 0) {
            return false;
        }
        return baselineIndex > currentIndex;
    }

    private boolean compareBool(CapabilityValue current, CapabilityValue baseline) {
        if (!(current instanceof CapabilityValue.BoolValue currentBool)
                || !(baseline instanceof CapabilityValue.BoolValue baselineBool)) {
            return false;
        }
        return baselineBool.value() && !currentBool.value();
    }

    private boolean compareInt(CapabilityValue current, CapabilityValue baseline) {
        if (!(current instanceof CapabilityValue.IntValue currentInt)
                || !(baseline instanceof CapabilityValue.IntValue baselineInt)) {
            return false;
        }
        return baselineInt.value() > currentInt.value();
    }

    private boolean compareFloat(CapabilityValue current, CapabilityValue baseline) {
        if (!(current instanceof CapabilityValue.FloatValue currentFloat)
                || !(baseline instanceof CapabilityValue.FloatValue baselineFloat)) {
            return false;
        }
        return baselineFloat.value() > currentFloat.value();
    }
}
