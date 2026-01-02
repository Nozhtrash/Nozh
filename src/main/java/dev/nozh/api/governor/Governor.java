package dev.nozh.api.governor;

import dev.nozh.api.Bound;
import dev.nozh.api.PerfSnapshot;

/**
 * The Brain.
 * Pure function: Accepts data, returns intent.
 * NEVER modifies game state.
 */
public interface Governor {
    /**
     * Evaluate performance and return a decision.
     * 
     * @param snapshot Current performance data
     * @param bound    Current bottleneck classification
     * @return A decision (intent), never null.
     */
    Decision evaluate(PerfSnapshot snapshot, Bound bound);
}
