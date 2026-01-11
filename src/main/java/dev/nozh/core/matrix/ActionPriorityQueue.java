package dev.nozh.core.matrix;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.capability.ImpactLevel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Priority queue for actions based on multiple factors:
 * - Expected gain (from CapabilityMetrics)
 * - Historical success rate
 * - Current scenario relevance
 * - Cooldown status
 * 
 * INTEGRATION: Action matrix and governor
 * CONTRACT: Thread-safe, efficient sorting
 */
public final class ActionPriorityQueue {

    /**
     * Scored action with priority and reasoning.
     */
    public record ScoredAction(
        ActionCandidate action,
        double priorityScore,
        String reasoning
    ) implements Comparable<ScoredAction> {
        @Override
        public int compareTo(ScoredAction other) {
            // Higher priority first (reverse order)
            return Double.compare(other.priorityScore, this.priorityScore);
        }
    }

    private final PriorityBlockingQueue<ScoredAction> queue = new PriorityBlockingQueue<>();
    private final Map<CapabilityId, Double> successRates = new ConcurrentHashMap<>();
    private final Map<CapabilityId, Long> cooldowns = new ConcurrentHashMap<>();
    
    /**
     * Enqueue an action candidate with scoring.
     */
    public void enqueue(ActionCandidate action) {
        enqueue(action, 1.0, 0.0);
    }

    /**
     * Enqueue an action with scenario relevance and success rate.
     */
    public void enqueue(ActionCandidate action, double scenarioRelevance, double successRate) {
        double score = calculatePriority(action, scenarioRelevance, successRate);
        String reasoning = buildReasoning(action, scenarioRelevance, successRate, score);
        
        ScoredAction scored = new ScoredAction(action, score, reasoning);
        queue.offer(scored);
    }

    /**
     * Dequeue highest priority action.
     */
    public Optional<ScoredAction> dequeue() {
        ScoredAction action = queue.poll();
        return Optional.ofNullable(action);
    }

    /**
     * Peek at top N actions without removing.
     */
    public List<ScoredAction> peek(int count) {
        List<ScoredAction> result = new ArrayList<>();
        Iterator<ScoredAction> iterator = queue.iterator();
        
        int added = 0;
        while (iterator.hasNext() && added < count) {
            result.add(iterator.next());
            added++;
        }
        
        // Sort by priority
        result.sort(Comparator.reverseOrder());
        return result;
    }

    /**
     * Recalculate priorities for all queued actions.
     * Useful when success rates or scenario changes.
     */
    public void recalculatePriorities() {
        List<ScoredAction> current = new ArrayList<>();
        queue.drainTo(current);
        
        for (ScoredAction scored : current) {
            ActionCandidate action = scored.action();
            double successRate = successRates.getOrDefault(action.capabilityId(), 0.5);
            double score = calculatePriority(action, 1.0, successRate);
            String reasoning = buildReasoning(action, 1.0, successRate, score);
            
            queue.offer(new ScoredAction(action, score, reasoning));
        }
    }

    /**
     * Update success rate for a capability.
     */
    public void updateSuccessRate(CapabilityId capability, double rate) {
        successRates.put(capability, Math.max(0.0, Math.min(1.0, rate)));
    }

    /**
     * Set cooldown for a capability.
     */
    public void setCooldown(CapabilityId capability, long cooldownMs) {
        if (cooldownMs > 0) {
            cooldowns.put(capability, System.currentTimeMillis() + cooldownMs);
        } else {
            cooldowns.remove(capability);
        }
    }

    /**
     * Check if capability is on cooldown.
     */
    public boolean isOnCooldown(CapabilityId capability) {
        Long cooldownEnd = cooldowns.get(capability);
        if (cooldownEnd == null) {
            return false;
        }
        
        if (System.currentTimeMillis() >= cooldownEnd) {
            cooldowns.remove(capability);
            return false;
        }
        
        return true;
    }

    /**
     * Get queue size.
     */
    public int size() {
        return queue.size();
    }

    /**
     * Clear the queue.
     */
    public void clear() {
        queue.clear();
    }

    /**
     * Remove actions on cooldown.
     */
    public int removeActionsOnCooldown() {
        int removed = 0;
        Iterator<ScoredAction> iterator = queue.iterator();
        
        while (iterator.hasNext()) {
            ScoredAction scored = iterator.next();
            if (isOnCooldown(scored.action().capabilityId())) {
                iterator.remove();
                removed++;
            }
        }
        
        return removed;
    }

    /**
     * Calculate priority score for an action.
     * 
     * Factors:
     * - Expected gain (most important)
     * - Confidence score
     * - Success rate (historical)
     * - Scenario relevance
     * - Safety level (safer actions preferred)
     * - Impact level (lower impact preferred)
     */
    private double calculatePriority(ActionCandidate action, double scenarioRelevance, double successRate) {
        // Base score from expected gain and confidence
        double baseScore = action.expectedGainMs() * action.confidenceScore();
        
        // Historical success multiplier (0.5 to 1.5)
        double successMultiplier = 0.5 + successRate;
        
        // Scenario relevance multiplier
        double scenarioMultiplier = scenarioRelevance;
        
        // Safety bonus (safer actions get slight boost)
        double safetyBonus = switch (action.safetyLevel()) {
            case SAFE -> 1.1;
            case MOSTLY_SAFE -> 1.05;
            case RISKY -> 0.95;
            case DANGEROUS -> 0.9;
        };
        
        // Impact penalty (higher impact = lower priority)
        double impactPenalty = switch (action.gameplayImpact()) {
            case NONE -> 1.0;
            case MINIMAL -> 0.98;
            case MODERATE -> 0.95;
            case SIGNIFICANT -> 0.9;
            case MAJOR -> 0.85;
        };
        
        return baseScore * successMultiplier * scenarioMultiplier * safetyBonus * impactPenalty;
    }

    private String buildReasoning(ActionCandidate action, double scenarioRelevance, 
                                  double successRate, double finalScore) {
        return String.format(
            "Priority: %.2f | Gain: %.1fms | Confidence: %.0f%% | Success: %.0f%% | Scenario: %.0f%%",
            finalScore,
            action.expectedGainMs(),
            action.confidenceScore() * 100,
            successRate * 100,
            scenarioRelevance * 100
        );
    }

    /**
     * Get statistics summary.
     */
    public String getStats() {
        if (queue.isEmpty()) {
            return "Queue: Empty";
        }

        Optional<ScoredAction> top = peek(1).stream().findFirst();
        return String.format("Queue: %d actions | Top priority: %.2f (%s)",
            queue.size(),
            top.map(ScoredAction::priorityScore).orElse(0.0),
            top.map(a -> a.action().capabilityId().name()).orElse("None")
        );
    }
}
