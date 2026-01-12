package dev.nozh.core.intelligence;

import dev.nozh.api.PerfSnapshot;
import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.math.RollingVariance;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Validates action outcomes to prevent false positives.
 * 
 * Features:
 * - Statistical significance testing for performance deltas
 * - Sustained improvement validation (multiple samples)
 * - Related action cooldown tracking
 * - Minimum improvement thresholds
 * 
 * @author Nozh Team
 * @since 0.3.0
 */
public final class ActionValidator {

    private static final int MIN_SAMPLES_FOR_VALIDATION = 3;
    private static final double MIN_P95_IMPROVEMENT_MS = 0.5; // At least 0.5ms improvement
    private static final double SIGNIFICANCE_THRESHOLD = 0.7; // 70% confidence required
    private static final long DEFAULT_COOLDOWN_MS = 10_000; // 10 seconds between related actions
    
    // Related action groups (actions that affect similar areas)
    private static final Map<CapabilityId, CapabilityId[]> RELATED_ACTIONS = new EnumMap<>(CapabilityId.class);
    
    static {
        // Visual effects group
        RELATED_ACTIONS.put(CapabilityId.PARTICLES, new CapabilityId[]{
            CapabilityId.CLOUDS, CapabilityId.ENTITY_SHADOWS
        });
        RELATED_ACTIONS.put(CapabilityId.CLOUDS, new CapabilityId[]{
            CapabilityId.PARTICLES, CapabilityId.FOG
        });
        
        // Distance group
        RELATED_ACTIONS.put(CapabilityId.RENDER_DISTANCE, new CapabilityId[]{
            CapabilityId.SIMULATION_DISTANCE, CapabilityId.ENTITY_DISTANCE
        });
        RELATED_ACTIONS.put(CapabilityId.ENTITY_DISTANCE, new CapabilityId[]{
            CapabilityId.RENDER_DISTANCE, CapabilityId.SIMULATION_DISTANCE
        });
        
        // Quality group
        RELATED_ACTIONS.put(CapabilityId.GRAPHICS_MODE, new CapabilityId[]{
            CapabilityId.SMOOTH_LIGHTING, CapabilityId.ENTITY_SHADOWS
        });
    }

    private final Map<CapabilityId, Long> lastActionTimestamps = new EnumMap<>(CapabilityId.class);
    private final Map<CapabilityId, RollingVariance> recentImprovements = new EnumMap<>(CapabilityId.class);

    /**
     * Validation result with detailed metrics.
     */
    public record ValidationResult(
            boolean isValid,
            double significanceScore,
            double sustainedImprovement,
            String reason
    ) {
        public static ValidationResult valid(double significance, double improvement) {
            return new ValidationResult(true, significance, improvement, "Improvement validated");
        }
        
        public static ValidationResult invalid(String reason) {
            return new ValidationResult(false, 0.0, 0.0, reason);
        }
    }

    /**
     * Validates that an action produced a real, sustained improvement.
     * 
     * @param capability The capability that was modified
     * @param before Performance snapshot before action
     * @param after Performance snapshot after observation window
     * @param samples Number of samples in observation window
     * @return Validation result with significance and improvement metrics
     */
    public ValidationResult validateImprovement(
            CapabilityId capability,
            PerfSnapshot before,
            PerfSnapshot after,
            int samples
    ) {
        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(before, "before snapshot");
        Objects.requireNonNull(after, "after snapshot");
        
        // Check minimum samples
        if (samples < MIN_SAMPLES_FOR_VALIDATION) {
            return ValidationResult.invalid(
                String.format("Insufficient samples: %d < %d", samples, MIN_SAMPLES_FOR_VALIDATION));
        }
        
        // Check data availability
        if (!before.sufficientData() || !after.sufficientData()) {
            return ValidationResult.invalid("Insufficient performance data for comparison");
        }
        
        // Calculate improvements
        double p95Before = before.p95FrametimeMs();
        double p95After = after.p95FrametimeMs();
        double p95Improvement = p95Before - p95After;
        
        double avgBefore = before.avgFrametimeMs();
        double avgAfter = after.avgFrametimeMs();
        double avgImprovement = avgBefore - avgAfter;
        
        int spikesBefore = before.spikeCount();
        int spikesAfter = after.spikeCount();
        int spikeReduction = spikesBefore - spikesAfter;
        
        // Check for minimum improvement
        if (p95Improvement < MIN_P95_IMPROVEMENT_MS && avgImprovement < MIN_P95_IMPROVEMENT_MS) {
            return ValidationResult.invalid(
                String.format("Improvement too small: p95=%.2fms avg=%.2fms (min=%.2fms)",
                    p95Improvement, avgImprovement, MIN_P95_IMPROVEMENT_MS));
        }
        
        // Calculate significance score
        double significance = calculateSignificance(
            p95Before, p95After,
            avgBefore, avgAfter,
            before.frametimeStddevMs(), after.frametimeStddevMs(),
            spikeReduction
        );
        
        // Check significance threshold
        if (significance < SIGNIFICANCE_THRESHOLD) {
            return ValidationResult.invalid(
                String.format("Significance too low: %.2f < %.2f", significance, SIGNIFICANCE_THRESHOLD));
        }
        
        // Record improvement for trend analysis
        recordImprovement(capability, p95Improvement);
        
        return ValidationResult.valid(significance, p95Improvement);
    }

    /**
     * Calculates statistical significance of improvement.
     * Uses a simplified effect size calculation.
     */
    private double calculateSignificance(
            double p95Before, double p95After,
            double avgBefore, double avgAfter,
            double stddevBefore, double stddevAfter,
            int spikeReduction
    ) {
        // Pooled standard deviation (simplified)
        double pooledStddev = Math.sqrt((stddevBefore * stddevBefore + stddevAfter * stddevAfter) / 2.0);
        
        // Guard against division by zero
        if (pooledStddev < 0.001) {
            pooledStddev = 1.0; // Default if variance is too low
        }
        
        // Effect size (Cohen's d approximation)
        double effectSize = (avgBefore - avgAfter) / pooledStddev;
        
        // P95 improvement weight
        double p95Weight = p95Before > 0 ? (p95Before - p95After) / p95Before : 0.0;
        
        // Spike reduction bonus
        double spikeBonus = spikeReduction > 0 ? Math.min(0.2, spikeReduction * 0.05) : 0.0;
        
        // Combine metrics into significance score (0-1 range)
        double rawSignificance = 
            clamp01(effectSize / 2.0) * 0.4 +  // Effect size component
            clamp01(p95Weight) * 0.4 +          // P95 improvement component
            spikeBonus;                         // Spike reduction bonus
        
        return clamp01(rawSignificance);
    }

    /**
     * Checks if an action is on cooldown due to itself or related actions.
     * 
     * @param capability The capability to check
     * @param nowMs Current timestamp
     * @return true if action should be delayed
     */
    public boolean isOnCooldown(CapabilityId capability, long nowMs) {
        Objects.requireNonNull(capability, "capability");
        
        // Check own cooldown
        Long lastAction = lastActionTimestamps.get(capability);
        if (lastAction != null && (nowMs - lastAction) < DEFAULT_COOLDOWN_MS) {
            return true;
        }
        
        // Check related action cooldowns
        CapabilityId[] related = RELATED_ACTIONS.get(capability);
        if (related != null) {
            for (CapabilityId relatedId : related) {
                Long relatedLast = lastActionTimestamps.get(relatedId);
                if (relatedLast != null && (nowMs - relatedLast) < DEFAULT_COOLDOWN_MS / 2) {
                    return true; // Half cooldown for related actions
                }
            }
        }
        
        return false;
    }

    /**
     * Records that an action was executed.
     * 
     * @param capability The capability that was modified
     * @param nowMs Current timestamp
     */
    public void recordActionExecuted(CapabilityId capability, long nowMs) {
        Objects.requireNonNull(capability, "capability");
        lastActionTimestamps.put(capability, nowMs);
    }

    /**
     * Gets the average improvement for a capability.
     * 
     * @param capability The capability to check
     * @return Average improvement in ms, or 0.0 if no data
     */
    public double getAverageImprovement(CapabilityId capability) {
        RollingVariance variance = recentImprovements.get(capability);
        return variance != null ? variance.getMean() : 0.0;
    }

    /**
     * Gets the consistency (inverse of variance) of improvements for a capability.
     * Higher values = more consistent improvements.
     * 
     * @param capability The capability to check
     * @return Consistency score (0-1), or 0.0 if no data
     */
    public double getImprovementConsistency(CapabilityId capability) {
        RollingVariance variance = recentImprovements.get(capability);
        if (variance == null || !variance.isFull()) {
            return 0.0;
        }
        double cv = variance.getCoefficientOfVariation();
        // Low CV = high consistency
        return clamp01(1.0 - cv);
    }

    /**
     * Records an improvement for trend analysis.
     */
    private void recordImprovement(CapabilityId capability, double improvement) {
        RollingVariance variance = recentImprovements.computeIfAbsent(
            capability, k -> new RollingVariance(10));
        variance.addSample(improvement);
    }

    /**
     * Resets all tracking data.
     */
    public void reset() {
        lastActionTimestamps.clear();
        recentImprovements.clear();
    }

    /**
     * Clamps a value to the [0, 1] range.
     */
    private static double clamp01(double value) {
        if (value < 0.0) return 0.0;
        if (value > 1.0) return 1.0;
        return value;
    }
}
