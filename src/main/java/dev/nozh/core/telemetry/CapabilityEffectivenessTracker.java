package dev.nozh.core.telemetry;

import dev.nozh.core.capability.CapabilityId;
import dev.nozh.core.governor.ActionOutcome;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the effectiveness of each capability over time.
 * Used to prioritize actions and avoid repeatedly failing changes.
 */
public final class CapabilityEffectivenessTracker {
    private final Map<CapabilityId, EffectivenessMetrics> metrics = new ConcurrentHashMap<>();
    
    public void recordOutcome(CapabilityId capability, ActionOutcome outcome, double impactMs) {
        metrics.computeIfAbsent(capability, k -> new EffectivenessMetrics())
            .record(outcome, impactMs);
    }
    
    public Optional<EffectivenessMetrics> getMetrics(CapabilityId capability) {
        return Optional.ofNullable(metrics.get(capability));
    }
    
    public Map<CapabilityId, EffectivenessMetrics> getAllMetrics() {
        return new HashMap<>(metrics);
    }
    
    public List<CapabilityRanking> getRankings() {
        return metrics.entrySet().stream()
            .map(e -> new CapabilityRanking(
                e.getKey(),
                e.getValue().successRate(),
                e.getValue().avgImpact(),
                e.getValue().total()
            ))
            .sorted(Comparator.comparingDouble(CapabilityRanking::score).reversed())
            .toList();
    }
    
    public void reset() {
        metrics.clear();
    }
    
    public static final class EffectivenessMetrics {
        private int successes = 0;
        private int failures = 0;
        private int neutrals = 0;
        private double cumulativeImpact = 0.0;
        private double bestImpact = Double.MIN_VALUE;
        private double worstImpact = Double.MAX_VALUE;
        private long lastUsed = System.currentTimeMillis();
        
        void record(ActionOutcome outcome, double impactMs) {
            switch (outcome) {
                case POSITIVE -> successes++;
                case NEGATIVE -> failures++;
                case NEUTRAL -> neutrals++;
            }
            cumulativeImpact += impactMs;
            bestImpact = Math.max(bestImpact, impactMs);
            worstImpact = Math.min(worstImpact, impactMs);
            lastUsed = System.currentTimeMillis();
        }
        
        public int successes() { return successes; }
        public int failures() { return failures; }
        public int neutrals() { return neutrals; }
        public int total() { return successes + failures + neutrals; }
        
        public double successRate() {
            int total = total();
            return total > 0 ? (double) successes / total : 0.0;
        }
        
        public double avgImpact() {
            int total = total();
            return total > 0 ? cumulativeImpact / total : 0.0;
        }
        
        public double bestImpact() {
            return bestImpact != Double.MIN_VALUE ? bestImpact : 0.0;
        }
        
        public double worstImpact() {
            return worstImpact != Double.MAX_VALUE ? worstImpact : 0.0;
        }
        
        public long lastUsed() { return lastUsed; }
    }
    
    public record CapabilityRanking(
        CapabilityId capability,
        double successRate,
        double avgImpact,
        int usageCount
    ) {
        public double score() {
            // Score = success rate * avg impact * log(usage + 1)
            // This favors capabilities that are reliable AND impactful
            return successRate * avgImpact * Math.log(usageCount + 1);
        }
    }
}