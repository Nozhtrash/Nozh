package dev.nozh.core.telemetry;

import dev.nozh.api.Scenario;
import dev.nozh.core.capability.CapabilityId;
import dev.nozh.core.capability.CapabilityValue;
import dev.nozh.core.governor.ActionOutcome;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Tracks all governor decisions with full context and outcomes.
 * Essential for understanding decision patterns and debugging.
 */
public final class DecisionTracker {
    private final Deque<DecisionRecord> decisions = new ConcurrentLinkedDeque<>();
    private static final int MAX_DECISIONS = 500;
    
    private final Map<CapabilityId, CapabilityStats> statsByCapability = new HashMap<>();
    private final Map<Scenario, ScenarioDecisionStats> statsByScenario = new HashMap<>();
    
    public void recordDecision(DecisionRecord record) {
        decisions.addLast(record);
        while (decisions.size() > MAX_DECISIONS) {
            decisions.removeFirst();
        }
        
        // Update stats
        statsByCapability.computeIfAbsent(record.capability(), k -> new CapabilityStats())
            .record(record.outcome());
        statsByScenario.computeIfAbsent(record.scenario(), k -> new ScenarioDecisionStats())
            .record(record.outcome());
    }
    
    public List<DecisionRecord> getRecentDecisions(int limit) {
        return decisions.stream()
            .skip(Math.max(0, decisions.size() - limit))
            .toList();
    }
    
    public Map<CapabilityId, CapabilityStats> getStatsByCapability() {
        return new HashMap<>(statsByCapability);
    }
    
    public Map<Scenario, ScenarioDecisionStats> getStatsByScenario() {
        return new HashMap<>(statsByScenario);
    }
    
    public OptionalDouble getAverageImpact() {
        return decisions.stream()
            .filter(d -> d.measuredImpactMs() != null)
            .mapToDouble(DecisionRecord::measuredImpactMs)
            .average();
    }
    
    public int getTotalDecisions() {
        return decisions.size();
    }
    
    public void reset() {
        decisions.clear();
        statsByCapability.clear();
        statsByScenario.clear();
    }
    
    public record DecisionRecord(
        long timestamp,
        Scenario scenario,
        CapabilityId capability,
        CapabilityValue oldValue,
        CapabilityValue newValue,
        double expectedImpactMs,
        Double measuredImpactMs,  // null if not yet measured
        ActionOutcome outcome,
        String reason
    ) {}
    
    public static final class CapabilityStats {
        private int positive = 0;
        private int neutral = 0;
        private int negative = 0;
        
        void record(ActionOutcome outcome) {
            switch (outcome) {
                case POSITIVE -> positive++;
                case NEUTRAL -> neutral++;
                case NEGATIVE -> negative++;
            }
        }
        
        public int positive() { return positive; }
        public int neutral() { return neutral; }
        public int negative() { return negative; }
        public int total() { return positive + neutral + negative; }
        public double successRate() {
            int total = total();
            return total > 0 ? (double) positive / total : 0.0;
        }
    }
    
    public static final class ScenarioDecisionStats {
        private int positive = 0;
        private int neutral = 0;
        private int negative = 0;
        
        void record(ActionOutcome outcome) {
            switch (outcome) {
                case POSITIVE -> positive++;
                case NEUTRAL -> neutral++;
                case NEGATIVE -> negative++;
            }
        }
        
        public int positive() { return positive; }
        public int neutral() { return neutral; }
        public int negative() { return negative; }
        public int total() { return positive + neutral + negative; }
        public double successRate() {
            int total = total();
            return total > 0 ? (double) positive / total : 0.0;
        }
    }
}