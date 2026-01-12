package dev.nozh.core.matrix;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.capability.ProviderStatus;
import dev.nozh.api.Scenario;

/**
 * Enhanced confidence calculator with Bayesian updates (Contract 5).
 * 
 * Features:
 * - Bayesian confidence updates based on prediction accuracy
 * - Scenario-specific confidence modifiers
 * - Success streak bonuses
 * - Adaptive decay based on action frequency
 * - Gradient anti-flapping (not binary)
 * 
 * Formula:
 * confidence = bayesianUpdate(base, evidence) * scenarioModifier * streakBonus
 *            * decayFactor * antiFlappingPenalty * stabilityFactor
 * 
 * PURE - deterministic calculations only.
 * 
 * @author Nozh Team
 * @since 0.3.0
 */
public final class ConfidenceCalculator {

    // Time constants
    private static final double BASE_DECAY_CONSTANT_MS = 300_000; // 5 minutes base
    private static final double MIN_DECAY_CONSTANT_MS = 60_000;   // 1 minute minimum
    private static final double MAX_DECAY_CONSTANT_MS = 600_000;  // 10 minutes maximum
    private static final double ANTI_FLAP_WINDOW_MS = 120_000;    // 2 minutes
    
    // Confidence weights
    private static final double BAYESIAN_PRIOR_WEIGHT = 0.6;
    private static final double BAYESIAN_EVIDENCE_WEIGHT = 0.4;
    private static final double MAX_STREAK_BONUS = 0.15;
    private static final int STREAK_THRESHOLD = 3;
    
    // Scenario modifiers (some scenarios need higher confidence)
    private static final double COMBAT_MODIFIER = 0.85;      // More conservative in combat
    private static final double BUILDING_MODIFIER = 0.90;    // Slightly conservative
    private static final double EXPLORATION_MODIFIER = 1.0;  // Normal confidence
    private static final double IDLE_MODIFIER = 1.1;         // More aggressive when idle
    private static final double LOADING_MODIFIER = 1.15;     // Very aggressive during loading

    /**
     * Calculate confidence for an action with full context.
     * 
     * @param dataConfidence        Confidence in perf data [0..1]
     * @param historicalSuccessRate Historical success rate [0..1]
     * @param providerStatus        Provider operational status
     * @param lastSuccessMillis     Last time this action succeeded (0 = never)
     * @param lastFailureMillis     Last time this action failed (0 = never)
     * @param environmentChanged    Whether environment (mods) changed since last success
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
        return calculate(
            dataConfidence,
            historicalSuccessRate,
            providerStatus,
            lastSuccessMillis,
            lastFailureMillis,
            environmentChanged,
            nowMillis,
            null,  // No scenario
            0,     // No success streak
            0.5    // Default prediction accuracy
        );
    }

    /**
     * Calculate confidence with full context including scenario and learning data.
     * 
     * @param dataConfidence        Confidence in perf data [0..1]
     * @param historicalSuccessRate Historical success rate [0..1]
     * @param providerStatus        Provider operational status
     * @param lastSuccessMillis     Last time this action succeeded (0 = never)
     * @param lastFailureMillis     Last time this action failed (0 = never)
     * @param environmentChanged    Whether environment (mods) changed since last success
     * @param nowMillis             Current timestamp
     * @param scenario              Current gameplay scenario (nullable)
     * @param successStreak         Consecutive successful actions for this capability
     * @param predictionAccuracy    Recent prediction accuracy [0..1]
     * @return Confidence score [0..1]
     */
    public double calculate(
            double dataConfidence,
            double historicalSuccessRate,
            ProviderStatus providerStatus,
            long lastSuccessMillis,
            long lastFailureMillis,
            boolean environmentChanged,
            long nowMillis,
            Scenario scenario,
            int successStreak,
            double predictionAccuracy) {
        
        // === 1. Provider confidence ===
        double providerConfidence = calculateProviderConfidence(providerStatus);
        if (providerConfidence <= 0.0) {
            return 0.0; // Broken provider = no confidence
        }

        // === 2. Historical confidence with environment awareness ===
        double historicConfidence = calculateHistoricalConfidence(
            historicalSuccessRate, environmentChanged);

        // === 3. Bayesian update using prediction accuracy as evidence ===
        double bayesianConfidence = bayesianUpdate(
            Math.min(dataConfidence, historicConfidence),
            predictionAccuracy
        );

        // === 4. Base confidence (combine all factors) ===
        double baseConfidence = Math.min(bayesianConfidence, providerConfidence);

        // === 5. Scenario modifier ===
        double scenarioModifier = getScenarioModifier(scenario);

        // === 6. Success streak bonus ===
        double streakBonus = calculateStreakBonus(successStreak);

        // === 7. Time decay (adaptive based on action frequency) ===
        double decayFactor = calculateDecayFactor(
            lastSuccessMillis, lastFailureMillis, nowMillis);

        // === 8. Gradient anti-flapping penalty ===
        double antiFlappingPenalty = calculateAntiFlappingPenalty(
            lastFailureMillis, nowMillis);

        // === 9. Final confidence ===
        double finalConfidence = baseConfidence 
            * scenarioModifier 
            * (1.0 + streakBonus) 
            * decayFactor 
            * antiFlappingPenalty;

        return clamp01(finalConfidence);
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
            System.currentTimeMillis(),
            null,
            0,
            0.5
        );
    }

    /**
     * Calculates provider confidence based on operational status.
     */
    private double calculateProviderConfidence(ProviderStatus status) {
        return switch (status) {
            case HEALTHY -> 1.0;
            case DEGRADED -> 0.6;
            case BROKEN -> 0.0;
        };
    }

    /**
     * Calculates historical confidence with environment change awareness.
     */
    private double calculateHistoricalConfidence(
            double historicalSuccessRate, boolean environmentChanged) {
        if (environmentChanged) {
            // Decay to 50% if environment changed, but never below base rate
            return Math.max(0.3, historicalSuccessRate * 0.5);
        }
        return historicalSuccessRate;
    }

    /**
     * Bayesian update combining prior belief with new evidence.
     * 
     * Uses simplified Bayesian formula:
     * posterior = (prior * weight_prior + evidence * weight_evidence)
     * 
     * When prediction accuracy is high, we trust historical data more.
     * When prediction accuracy is low, we're more conservative.
     */
    private double bayesianUpdate(double prior, double evidence) {
        // Validate inputs
        double safePrior = clamp01(prior);
        double safeEvidence = clamp01(evidence);
        
        // Evidence weight is modulated by how strong the evidence is
        // High evidence (accuracy > 0.7) increases weight
        // Low evidence (accuracy < 0.3) decreases weight
        double evidenceStrength = (safeEvidence - 0.5) * 2; // Range [-1, 1]
        double adjustedEvidenceWeight = BAYESIAN_EVIDENCE_WEIGHT 
            * (1.0 + evidenceStrength * 0.5);
        adjustedEvidenceWeight = clamp01(adjustedEvidenceWeight);
        
        double adjustedPriorWeight = 1.0 - adjustedEvidenceWeight;
        
        return safePrior * adjustedPriorWeight + safeEvidence * adjustedEvidenceWeight;
    }

    /**
     * Gets scenario-specific confidence modifier.
     * Combat needs higher confidence, idle allows more risk.
     */
    private double getScenarioModifier(Scenario scenario) {
        if (scenario == null) {
            return 1.0;
        }
        return switch (scenario) {
            case COMBAT -> COMBAT_MODIFIER;
            case BUILDING -> BUILDING_MODIFIER;
            case MINING -> BUILDING_MODIFIER;
            case EXPLORATION -> EXPLORATION_MODIFIER;
            case IDLE -> IDLE_MODIFIER;
            case AFK -> IDLE_MODIFIER;
            case MENU -> IDLE_MODIFIER;
            case WORLD_LOADING -> LOADING_MODIFIER;
            case HIGH_ENTITY_DENSITY -> COMBAT_MODIFIER; // Conservative
            case UNKNOWN -> 1.0;
        };
    }

    /**
     * Calculates bonus for consecutive successful actions.
     * Rewards consistent success with up to 15% confidence boost.
     */
    private double calculateStreakBonus(int successStreak) {
        if (successStreak < STREAK_THRESHOLD) {
            return 0.0;
        }
        // Logarithmic bonus to prevent runaway confidence
        double bonus = Math.log1p(successStreak - STREAK_THRESHOLD + 1) * 0.05;
        return Math.min(bonus, MAX_STREAK_BONUS);
    }

    /**
     * Calculates time-based decay factor.
     * Adapts decay rate based on time since last activity.
     */
    private double calculateDecayFactor(
            long lastSuccessMillis, long lastFailureMillis, long nowMillis) {
        if (lastSuccessMillis <= 0) {
            return 0.8; // No history = conservative baseline
        }
        
        long timeSinceSuccess = nowMillis - lastSuccessMillis;
        if (timeSinceSuccess <= 0) {
            return 1.0;
        }
        
        // Adaptive decay constant:
        // If we have recent failure, use faster decay
        // If we have consistent success, use slower decay
        double decayConstant = BASE_DECAY_CONSTANT_MS;
        if (lastFailureMillis > 0 && lastFailureMillis > lastSuccessMillis) {
            // Recent failure - faster decay
            decayConstant = MIN_DECAY_CONSTANT_MS;
        } else if (lastFailureMillis <= 0 || 
                   (lastSuccessMillis - lastFailureMillis) > 300_000) {
            // No recent failure - slower decay
            decayConstant = MAX_DECAY_CONSTANT_MS;
        }
        
        return Math.exp(-timeSinceSuccess / decayConstant);
    }

    /**
     * Calculates gradient anti-flapping penalty.
     * Uses smooth falloff instead of binary penalty.
     */
    private double calculateAntiFlappingPenalty(long lastFailureMillis, long nowMillis) {
        if (lastFailureMillis <= 0) {
            return 1.0; // No failures on record
        }
        
        long timeSinceFailure = nowMillis - lastFailureMillis;
        if (timeSinceFailure <= 0) {
            return 0.3; // Just failed - heavy penalty
        }
        
        if (timeSinceFailure >= ANTI_FLAP_WINDOW_MS) {
            return 1.0; // Outside window - no penalty
        }
        
        // Gradient penalty: linear recovery over the window
        // At t=0: penalty = 0.3
        // At t=window: penalty = 1.0
        double progress = timeSinceFailure / ANTI_FLAP_WINDOW_MS;
        return 0.3 + (0.7 * progress);
    }

    /**
     * Clamps a value to [0, 1] range.
     */
    private static double clamp01(double value) {
        if (value < 0.0) return 0.0;
        if (value > 1.0) return 1.0;
        return value;
    }
}

