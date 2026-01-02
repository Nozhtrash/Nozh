package dev.nozh.api;

/**
 * Contract for decision-making engine.
 * 
 * The Governor analyzes performance snapshots and decides
 * what actions (if any) should be taken.
 * 
 * Phase 2: Interface definition
 * Phase 5: Passive implementation (logs only)
 * Phase 6: Active implementation (applies changes)
 */
public interface Governor {

    /**
     * Evaluate current performance and generate action plan.
     * 
     * Phase 5: Returns plan but doesn't execute
     * Phase 6: Plan is actually executed by ActionExecutor
     * 
     * @param snapshot current performance state
     * @return action plan (may be empty if no action needed)
     * @throws IllegalArgumentException if snapshot is invalid
     */
    ActionPlan evaluate(PerfSnapshot snapshot);

    /**
     * Check if governor can currently apply actions.
     * Returns false if in cooldown, safe mode, or disabled.
     */
    boolean canAct();
}
