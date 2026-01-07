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
import dev.nozh.core.governor.DecisionBudget;
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
    private static final double LEARNED_CONFIDENCE_WEIGHT = 0.3;
    private static final double LEARNED_RANKING_WEIGHT = 0.2;
    private static final double LEARNED_SUCCESS_WEIGHT = 0.1;
    private static final double LEARNED_GAIN_WEIGHT = 0.1;
    private static final double REAL_GAIN_WEIGHT = 0.15;
    private static final double SPIKE_PENALTY_WEIGHT = 0.2;
    private static final double PERSISTENT_PENALTY_WEIGHT = 0.25;
    private static final double SCENARIO_PRIORITY_WEIGHT = 0.15;
    private static final double PERFORMANCE_PRESSURE_WEIGHT = 0.25;
    private static final double BASELINE_FRAME_MS = 16.67;
    private static final double MAX_SPIKE_PRESSURE = 5.0;
    private static final String[] PARTICLE_ORDER = {"MINIMAL", "DECREASED", "ALL"};
    private static final String[] CLOUD_ORDER = {"OFF", "FAST", "FANCY"};
    private static final String[] GRAPHICS_ORDER = {"FAST", "FANCY", "FABULOUS"};

    private final ProviderRegistry registry;
    private final ActionSuccessTracker successTracker;
    private final ConfidenceCalculator confidenceCalculator;
    private final dev.nozh.core.compatibility.CompatibilityMatrix compatibilityMatrix;
    private final dev.nozh.core.intelligence.SessionLearning sessionLearning;
    private final ActionMatrixRules scenarioRules;

    public ActionMatrix(
            ProviderRegistry registry,
            ActionSuccessTracker successTracker,
            ConfidenceCalculator confidenceCalculator,
            dev.nozh.core.intelligence.SessionLearning sessionLearning) {
        this(registry, successTracker, confidenceCalculator, sessionLearning, null, ActionMatrixRules.defaultRules());
    }

    public ActionMatrix(
            ProviderRegistry registry,
            ActionSuccessTracker successTracker,
            ConfidenceCalculator confidenceCalculator,
            dev.nozh.core.intelligence.SessionLearning sessionLearning,
            dev.nozh.core.compatibility.CompatibilityMatrix compatibilityMatrix) {
        this(registry, successTracker, confidenceCalculator, sessionLearning, compatibilityMatrix,
                ActionMatrixRules.defaultRules());
    }

    public ActionMatrix(
            ProviderRegistry registry,
            ActionSuccessTracker successTracker,
            ConfidenceCalculator confidenceCalculator,
            dev.nozh.core.intelligence.SessionLearning sessionLearning,
            dev.nozh.core.compatibility.CompatibilityMatrix compatibilityMatrix,
            ActionMatrixRules scenarioRules) {
        this.registry = registry;
        this.successTracker = successTracker;
        this.confidenceCalculator = confidenceCalculator;
        this.sessionLearning = sessionLearning;
        this.compatibilityMatrix = compatibilityMatrix != null ? compatibilityMatrix : createCompatibilityMatrixSafe();
        this.scenarioRules = scenarioRules != null ? scenarioRules : ActionMatrixRules.defaultRules();
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
     * @param profile      Optimization profile (aggressive/balanced)
     * @param p95FrametimeMs Current p95 frametime (ms)
     * @param spikeCount   Current spike count
     * @return Sorted candidates (best first), may be empty
     */
    public List<ActionCandidate> generateCandidates(
            ModePolicy policy,
            String currentBound,
            Scenario scenario,
            OptimizationProfile profile,
            double p95FrametimeMs,
            int spikeCount,
            ActionMatrixTuning tuning,
            DecisionBudget budget) {
        List<ActionCandidate> candidates = new ArrayList<>();
        ActionCandidate yieldCandidate = null;
        long now = System.currentTimeMillis();
        ActionMatrixTuning resolvedTuning = tuning != null ? tuning : ActionMatrixTuning.defaults();

        // Iterate all registered providers
        for (CapabilityProvider provider : registry.getAllProviders()) {
            if (budget != null && budget.isOverBudget()) {
                return List.of();
            }
            CapabilityId id = provider.id();
            ProviderMetadata metadata = provider.metadata();

            // Filter by provider status
            if (provider.status() == ProviderStatus.BROKEN) {
                continue; // Skip broken providers
            }
            if (provider.status() == ProviderStatus.DEGRADED) {
                continue; // Skip unstable providers
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
                if (yieldCandidate == null) {
                    yieldCandidate = ActionCandidate.yield(id,
                            compatibilityMatrix.getDependencySteward(metadata));
                }
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
            double learnedConfidence = sessionLearning.getSuccessRate(id, scenario);
            double blendedConfidence = clampConfidence(blendConfidence(confidence, learnedConfidence)
                    + resolvedTuning.confidenceBonus());

            // Filter by confidence threshold
            if (blendedConfidence < policy.minConfidence()) {
                continue;
            }

            if (shouldSkipForScenario(metadata, scenario, profile)) {
                continue;
            }

            // Generate target value based on bound + provider type
            CapabilityValue targetValue = generateTargetValue(id, currentBound, scenario, profile, resolvedTuning);

            // Skip if no target value could be determined
            if (targetValue == null) {
                continue;
            }

            // INTEGRATION: Conflict Detection
            if (compatibilityMatrix != null && compatibilityMatrix.isExternallyManaged(id, metadata)) {
                // If another mod handles this, we should not touch it
                if (yieldCandidate == null) {
                    yieldCandidate = ActionCandidate.yield(id, compatibilityMatrix.getSteward(id));
                }
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
                    blendedConfidence,
                    generateReason(id, blendedConfidence, metadata));

            // Filter by allowed tiers
            if (!policy.allowedTiers().contains(candidate.tier())) {
                continue;
            }

            candidates.add(candidate);
        }

        if (budget != null && budget.isOverBudget()) {
            return List.of();
        }

        // Sort by weighted score (descending)
        double maxExpectedGain = 0.0;
        double maxRanking = 0.0;
        double maxLearnedGain = 0.0;
        double maxP95Gain = 0.0;
        double maxSpikePenalty = 0.0;
        for (ActionCandidate candidate : candidates) {
            maxExpectedGain = Math.max(maxExpectedGain, candidate.expectedGainMs());
            double ranking = sessionLearning.getRanking(candidate.capabilityId(), scenario);
            maxRanking = Math.max(maxRanking, ranking);
            double learnedGain = sessionLearning.getAvgFpsGain(candidate.capabilityId(), scenario);
            maxLearnedGain = Math.max(maxLearnedGain, learnedGain);
            double p95Gain = sessionLearning.getAvgP95Gain(candidate.capabilityId(), scenario);
            maxP95Gain = Math.max(maxP95Gain, p95Gain);
            double spikePenalty = Math.max(0.0, sessionLearning.getAvgSpikeDelta(candidate.capabilityId(), scenario));
            maxSpikePenalty = Math.max(maxSpikePenalty, spikePenalty);
        }

        final double maxExpectedGainFinal = maxExpectedGain;
        final double maxRankingFinal = maxRanking;
        final double maxLearnedGainFinal = maxLearnedGain;
        final double maxP95GainFinal = maxP95Gain;
        final double maxSpikePenaltyFinal = maxSpikePenalty;

        ActionSelectionContext selectionContext = new ActionSelectionContext(scenario, profile, p95FrametimeMs,
                spikeCount, resolvedTuning);
        candidates.sort(Comparator
                .comparingDouble((ActionCandidate candidate) -> scoreCandidate(candidate, maxExpectedGainFinal,
                        maxRankingFinal, maxLearnedGainFinal, maxP95GainFinal, maxSpikePenaltyFinal, selectionContext))
                .reversed());

        if (candidates.isEmpty() && yieldCandidate != null) {
            return List.of(yieldCandidate);
        }

        return candidates;
    }

    public List<ActionCandidate> generateCandidates(
            ModePolicy policy,
            String currentBound,
            Scenario scenario,
            OptimizationProfile profile,
            double p95FrametimeMs,
            int spikeCount) {
        return generateCandidates(policy, currentBound, scenario, profile, p95FrametimeMs, spikeCount,
                ActionMatrixTuning.defaults(), null);
    }

    public List<ActionCandidate> generateCandidates(
            ModePolicy policy,
            String currentBound,
            Scenario scenario,
            OptimizationProfile profile,
            double p95FrametimeMs,
            int spikeCount,
            ActionMatrixTuning tuning) {
        return generateCandidates(policy, currentBound, scenario, profile, p95FrametimeMs, spikeCount,
                tuning, null);
    }

    public List<ActionCandidate> generateReverseCandidates(
            ModePolicy policy,
            Scenario scenario,
            OptimizationProfile profile,
            dev.nozh.core.state.BaselineSnapshot baselineSnapshot,
            Map<CapabilityId, CapabilityValue> currentSettings,
            ActionMatrixTuning tuning,
            DecisionBudget budget) {
        List<ActionCandidate> candidates = new ArrayList<>();
        if (baselineSnapshot == null || baselineSnapshot.isEmpty()) {
            return candidates;
        }
        long now = System.currentTimeMillis();
        ActionMatrixTuning resolvedTuning = tuning != null ? tuning : ActionMatrixTuning.defaults();

        for (CapabilityProvider provider : registry.getAllProviders()) {
            if (budget != null && budget.isOverBudget()) {
                return List.of();
            }
            CapabilityId id = provider.id();
            ProviderMetadata metadata = provider.metadata();

            if (!policy.allowedTiers().contains(determineTier(metadata))) {
                continue;
            }

            if (provider.status() == ProviderStatus.DEGRADED) {
                continue;
            }

            CapabilityValue baseline = baselineSnapshot.get(id).orElse(null);
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
            double learnedConfidence = sessionLearning.getSuccessRate(id, scenario);
            double blendedConfidence = clampConfidence(blendConfidence(confidence, learnedConfidence)
                    + resolvedTuning.confidenceBonus());

            if (blendedConfidence < policy.minConfidence()) {
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
                    blendedConfidence,
                    String.format("%s → restore baseline (%s profile)", id.name(), profile.name())));
        }

        candidates.sort(Comparator
                .comparingInt(ActionCandidate::tier)
                .thenComparing(Comparator.comparingDouble(ActionCandidate::confidenceScore).reversed()));

        return candidates;
    }

    public List<ActionCandidate> generateReverseCandidates(
            ModePolicy policy,
            Scenario scenario,
            OptimizationProfile profile,
            dev.nozh.core.state.BaselineSnapshot baselineSnapshot,
            Map<CapabilityId, CapabilityValue> currentSettings) {
        return generateReverseCandidates(policy, scenario, profile, baselineSnapshot, currentSettings,
                ActionMatrixTuning.defaults(), null);
    }

    public List<ActionCandidate> generateReverseCandidates(
            ModePolicy policy,
            Scenario scenario,
            OptimizationProfile profile,
            dev.nozh.core.state.BaselineSnapshot baselineSnapshot,
            Map<CapabilityId, CapabilityValue> currentSettings,
            ActionMatrixTuning tuning) {
        return generateReverseCandidates(policy, scenario, profile, baselineSnapshot, currentSettings,
                tuning, null);
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
            OptimizationProfile profile,
            ActionMatrixTuning tuning) {
        boolean cpuBound = "CPU".equals(bound);
        boolean gpuBound = "GPU".equals(bound);
        boolean balanced = "BALANCED".equals(bound);
        boolean aggressive = profile != null && profile.isAggressive();
        boolean combat = scenario == Scenario.COMBAT;
        boolean building = scenario == Scenario.BUILDING;
        boolean menu = scenario == Scenario.MENU;
        boolean loading = scenario == Scenario.LOADING;

        CapabilityValue scenarioTarget = scenarioRules.resolveTarget(id, scenario, profile);
        CapabilityValue targetValue = scenarioTarget;
        if (targetValue != null) {
            CapabilityValue limitedValue = scenarioRules.applyLimits(id, scenario, profile, targetValue);
            return applyTuningLimits(id, limitedValue, tuning);
        }

        switch (id) {
            case PARTICLES -> {
                if (loading) {
                    targetValue = new CapabilityValue.EnumValue("MINIMAL");
                    break;
                }
                if (cpuBound) {
                    targetValue = new CapabilityValue.EnumValue("MINIMAL");
                    break;
                }
                if (gpuBound || balanced) {
                    targetValue = new CapabilityValue.EnumValue(aggressive ? "MINIMAL" : "DECREASED");
                    break;
                }
            }
            case CLOUDS -> {
                if (loading || cpuBound) {
                    targetValue = new CapabilityValue.EnumValue("OFF");
                    break;
                }
                if (gpuBound || balanced) {
                    targetValue = new CapabilityValue.EnumValue(aggressive ? "OFF" : "FAST");
                    break;
                }
            }
            case ENTITY_SHADOWS -> {
                if (gpuBound || cpuBound) {
                    targetValue = new CapabilityValue.BoolValue(false);
                    break;
                }
            }
            case RENDER_DISTANCE -> {
                if (gpuBound) {
                    targetValue = new CapabilityValue.IntValue(aggressive ? 6 : 8);
                    break;
                }
                if (cpuBound) {
                    targetValue = new CapabilityValue.IntValue(aggressive ? 8 : 10);
                    break;
                }
            }
            case SIMULATION_DISTANCE -> {
                if (cpuBound) {
                    targetValue = new CapabilityValue.IntValue(aggressive ? 4 : 6);
                    break;
                }
                if (balanced && !building) {
                    targetValue = new CapabilityValue.IntValue(aggressive ? 5 : 7);
                    break;
                }
            }
            case ENTITY_DISTANCE -> {
                if (gpuBound) {
                    targetValue = new CapabilityValue.IntValue(aggressive ? 60 : 75);
                    break;
                }
                if (cpuBound && !building) {
                    targetValue = new CapabilityValue.IntValue(aggressive ? 65 : 80);
                    break;
                }
            }
            case BIOME_BLEND -> {
                if (gpuBound) {
                    targetValue = new CapabilityValue.IntValue(aggressive ? 1 : 2);
                    break;
                }
                if (cpuBound || balanced) {
                    targetValue = new CapabilityValue.IntValue(aggressive ? 2 : 3);
                    break;
                }
            }
            case MIPMAP_LEVEL -> {
                if (gpuBound) {
                    targetValue = new CapabilityValue.IntValue(aggressive ? 1 : 2);
                    break;
                }
                if (balanced && !building) {
                    targetValue = new CapabilityValue.IntValue(aggressive ? 2 : 3);
                    break;
                }
            }
            case VSYNC -> {
                if (loading || gpuBound) {
                    targetValue = new CapabilityValue.BoolValue(false);
                    break;
                }
            }
            case FOG -> {
                if (gpuBound) {
                    targetValue = new CapabilityValue.IntValue(aggressive ? 6 : 8);
                    break;
                }
                if (balanced && !building) {
                    targetValue = new CapabilityValue.IntValue(aggressive ? 8 : 10);
                    break;
                }
            }
            case GRAPHICS_MODE -> {
                if (gpuBound) {
                    targetValue = new CapabilityValue.EnumValue("FAST");
                    break;
                }
                if (balanced && !building) {
                    targetValue = new CapabilityValue.EnumValue("FAST");
                    break;
                }
            }
            case SMOOTH_LIGHTING -> {
                if (gpuBound || cpuBound) {
                    targetValue = new CapabilityValue.EnumValue("OFF");
                    break;
                }
            }
            case ARMOR_STANDS -> {
                if (loading || (cpuBound && !building)) {
                    targetValue = new CapabilityValue.BoolValue(false);
                    break;
                }
            }
            case ITEM_FRAMES -> {
                if (loading || (cpuBound && !building)) {
                    targetValue = new CapabilityValue.BoolValue(false);
                    break;
                }
            }
            case BLOCK_ENTITIES -> {
                if (!building && (loading || cpuBound)) {
                    targetValue = new CapabilityValue.BoolValue(false);
                    break;
                }
            }
            case ANIMATIONS -> {
                if (loading || (cpuBound && !building) || menu) {
                    targetValue = new CapabilityValue.BoolValue(false);
                    break;
                }
            }
            case FPS_CAP -> {
                if (menu) {
                    targetValue = new CapabilityValue.IntValue(60);
                    break;
                }
                if (gpuBound && !combat) {
                    targetValue = new CapabilityValue.IntValue(90);
                    break;
                }
            }
            case RESOLUTION_SCALE -> {
                if (gpuBound || loading) {
                    targetValue = new CapabilityValue.FloatValue(aggressive ? 0.6f : 0.8f);
                    break;
                }
            }
            case DISTORTION_EFFECT_SCALE -> {
                if (gpuBound || loading) {
                    targetValue = new CapabilityValue.FloatValue(aggressive ? 0.0f : 0.5f);
                    break;
                }
            }
            case DYNAMIC_LIGHTING -> {
                if (gpuBound || cpuBound || loading) {
                    targetValue = new CapabilityValue.BoolValue(false);
                    break;
                }
            }
            default -> {
            }
        }
        if (targetValue == null) {
            return null;
        }
        CapabilityValue limitedValue = scenarioRules.applyLimits(id, scenario, profile, targetValue);
        return applyTuningLimits(id, limitedValue, tuning);
    }

    private String generateReason(CapabilityId id, double confidence, ProviderMetadata metadata) {
        return String.format("%s → reduce (confidence: %.2f, expected gain: %.2fms)",
                id.name(), confidence, metadata.expectedGainMs());
    }

    private double scoreCandidate(ActionCandidate candidate, double maxExpectedGain, double maxRanking,
            double maxLearnedGain, double maxP95Gain, double maxSpikePenalty, ActionSelectionContext context) {
        double normalizedGain = maxExpectedGain > 0 ? candidate.expectedGainMs() / maxExpectedGain : 0.0;
        double ranking = sessionLearning.getRanking(candidate.capabilityId(), context.scenario());
        double normalizedRanking = maxRanking > 0 ? ranking / maxRanking : 0.0;
        double learnedSuccess = sessionLearning.getSuccessRate(candidate.capabilityId(), context.scenario());
        double learnedGain = sessionLearning.getAvgFpsGain(candidate.capabilityId(), context.scenario());
        double normalizedLearnedGain = maxLearnedGain > 0 ? learnedGain / maxLearnedGain : 0.0;
        double p95Gain = sessionLearning.getAvgP95Gain(candidate.capabilityId(), context.scenario());
        double normalizedP95Gain = maxP95Gain > 0 ? p95Gain / maxP95Gain : 0.0;
        double spikePenalty = Math.max(0.0, sessionLearning.getAvgSpikeDelta(candidate.capabilityId(), context.scenario()));
        double normalizedSpikePenalty = maxSpikePenalty > 0 ? spikePenalty / maxSpikePenalty : 0.0;
        double persistentPenalty = sessionLearning.getPersistentPenalty(candidate.capabilityId(), context.scenario());
        double scenarioBoost = scenarioRules.ruleWeight(candidate.capabilityId(), context.scenario(),
                context.profile()) * context.tuning().scenarioWeightMultiplier();
        double pressure = calculatePerformancePressure(context.p95FrametimeMs(), context.spikeCount())
                * context.tuning().pressureMultiplier();
        double profilePressureWeight = context.profile() != null && context.profile().isAggressive()
                ? PERFORMANCE_PRESSURE_WEIGHT * 1.2
                : PERFORMANCE_PRESSURE_WEIGHT;

        double baseScore = (candidate.confidenceScore() * CONFIDENCE_WEIGHT)
                + (normalizedGain * EXPECTED_GAIN_WEIGHT)
                + (normalizedRanking * LEARNED_RANKING_WEIGHT)
                + (learnedSuccess * LEARNED_SUCCESS_WEIGHT)
                + (normalizedLearnedGain * LEARNED_GAIN_WEIGHT)
                + (normalizedP95Gain * REAL_GAIN_WEIGHT)
                + (scenarioBoost * SCENARIO_PRIORITY_WEIGHT);

        double score = baseScore + (pressure * profilePressureWeight * (normalizedGain + scenarioBoost) / 2.0);
        return score - (normalizedSpikePenalty * SPIKE_PENALTY_WEIGHT)
                - (persistentPenalty * PERSISTENT_PENALTY_WEIGHT);
    }

    public ActionCandidate selectCandidateBandit(List<ActionCandidate> candidates, Scenario scenario,
            double explorationRate) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (explorationRate <= 0.0) {
            return candidates.get(0);
        }
        double roll = java.util.concurrent.ThreadLocalRandom.current().nextDouble();
        if (roll >= explorationRate) {
            return candidates.get(0);
        }
        return pickExplorationCandidate(candidates, scenario);
    }

    private ActionCandidate pickExplorationCandidate(List<ActionCandidate> candidates, Scenario scenario) {
        ActionCandidate bestCandidate = candidates.get(0);
        double bestScore = Double.NEGATIVE_INFINITY;
        for (ActionCandidate candidate : candidates) {
            int attempts = sessionLearning.getTotalAttempts(candidate.capabilityId(), scenario);
            double penalty = sessionLearning.getPersistentPenalty(candidate.capabilityId(), scenario);
            double explorationScore = (1.0 / (1.0 + Math.max(0, attempts))) * (1.0 - penalty);
            explorationScore += java.util.concurrent.ThreadLocalRandom.current().nextDouble() * 0.01;
            if (explorationScore > bestScore) {
                bestScore = explorationScore;
                bestCandidate = candidate;
            }
        }
        return bestCandidate;
    }

    private double blendConfidence(double baseConfidence, double learnedConfidence) {
        return (baseConfidence * (1.0 - LEARNED_CONFIDENCE_WEIGHT))
                + (learnedConfidence * LEARNED_CONFIDENCE_WEIGHT);
    }

    private double calculatePerformancePressure(double p95FrametimeMs, int spikeCount) {
        double p95Pressure = 0.0;
        if (p95FrametimeMs > 0) {
            p95Pressure = Math.min(1.0, Math.max(0.0, (p95FrametimeMs - BASELINE_FRAME_MS) / BASELINE_FRAME_MS));
        }
        double spikePressure = spikeCount > 0 ? Math.min(1.0, spikeCount / MAX_SPIKE_PRESSURE) : 0.0;
        return Math.min(1.0, Math.max(p95Pressure, spikePressure));
    }

    private record ActionSelectionContext(
            Scenario scenario,
            OptimizationProfile profile,
            double p95FrametimeMs,
            int spikeCount,
            ActionMatrixTuning tuning) {
    }

    private CapabilityValue applyTuningLimits(CapabilityId id, CapabilityValue value, ActionMatrixTuning tuning) {
        if (value == null || tuning == null) {
            return value;
        }
        if (value instanceof CapabilityValue.IntValue intValue) {
            int resolved = intValue.value();
            switch (id) {
                case RENDER_DISTANCE -> resolved = Math.min(resolved, tuning.maxRenderDistance());
                case SIMULATION_DISTANCE -> resolved = Math.min(resolved, tuning.maxSimulationDistance());
                case ENTITY_DISTANCE -> resolved = Math.min(resolved, tuning.maxEntityDistance());
                case FPS_CAP -> resolved = Math.min(resolved, tuning.maxFpsCap());
                default -> {
                }
            }
            if (resolved != intValue.value()) {
                return new CapabilityValue.IntValue(resolved);
            }
        }
        return value;
    }

    private double clampConfidence(double confidence) {
        if (confidence < 0.0) {
            return 0.0;
        }
        if (confidence > 1.0) {
            return 1.0;
        }
        return confidence;
    }

    private boolean isQualityIncrease(CapabilityId id, CapabilityValue current, CapabilityValue baseline) {
        if (current.equals(baseline)) {
            return false;
        }
        return switch (id) {
            case PARTICLES -> compareEnum(current, baseline, PARTICLE_ORDER);
            case CLOUDS -> compareEnum(current, baseline, CLOUD_ORDER);
            case GRAPHICS_MODE -> compareEnum(current, baseline, GRAPHICS_ORDER);
            case ENTITY_SHADOWS, ARMOR_STANDS, ITEM_FRAMES, BLOCK_ENTITIES, ANIMATIONS, VSYNC, DYNAMIC_LIGHTING ->
                    compareBool(current, baseline);
            case RENDER_DISTANCE, SIMULATION_DISTANCE, ENTITY_DISTANCE, BIOME_BLEND, MIPMAP_LEVEL, FOG ->
                    compareInt(current, baseline);
            case RESOLUTION_SCALE, DISTORTION_EFFECT_SCALE -> compareFloat(current, baseline);
            default -> false;
        };
    }

    private boolean compareEnum(CapabilityValue current, CapabilityValue baseline, String[] ordering) {
        if (!(current instanceof CapabilityValue.EnumValue currentEnum)
                || !(baseline instanceof CapabilityValue.EnumValue baselineEnum)) {
            return false;
        }
        int currentIndex = indexOf(ordering, currentEnum.name());
        int baselineIndex = indexOf(ordering, baselineEnum.name());
        if (currentIndex < 0 || baselineIndex < 0) {
            return false;
        }
        return baselineIndex > currentIndex;
    }

    private int indexOf(String[] ordering, String value) {
        for (int i = 0; i < ordering.length; i++) {
            if (ordering[i].equals(value)) {
                return i;
            }
        }
        return -1;
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

    private boolean shouldSkipForScenario(ProviderMetadata metadata, Scenario scenario, OptimizationProfile profile) {
        if (scenario == null) {
            return false;
        }
        if (scenario == Scenario.COMBAT && metadata.sideEffects().affectsInputLag()) {
            return true;
        }
        if (scenario == Scenario.MENU && metadata.gameplayImpact() != ImpactLevel.NONE) {
            return true;
        }
        if (scenario == Scenario.BUILDING && metadata.gameplayImpact() == ImpactLevel.HIGH) {
            return profile == null || !profile.isAggressive();
        }
        return false;
    }
}
