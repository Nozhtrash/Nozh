package dev.nozh.api;

/**
 * Performance gain estimate for actions.
 * 
 * Sealed interface enforces honesty: these are NOT measured metrics,
 * they are heuristic estimates for UX and logging only.
 * 
 * Phase 2 Iteration 1: Replaces double estimatedGainMs (which implied truth)
 * 
 * CRITICAL RULES:
 * - NEVER convert to numbers for comparisons
 * - NEVER sum or do math on estimates
 * - NEVER use for automated decisions
 * - ONLY for logging and user explanation
 */
public sealed interface GainEstimate permits GainEstimate.Rough, GainEstimate.Unknown {

    /**
     * Rough estimate with min/max range.
     * Example: Disabling shadows might save 5-8ms
     */
    record Rough(int minMs, int maxMs) implements GainEstimate {
        public Rough {
            if (minMs < 0 || maxMs < minMs) {
                throw new IllegalArgumentException("Invalid range: [" + minMs + ", " + maxMs + "]");
            }
        }

        @Override
        public String toString() {
            return minMs == maxMs ? minMs + "ms" : minMs + "-" + maxMs + "ms";
        }
    }

    /**
     * Unknown estimate - no data available.
     */
    record Unknown() implements GainEstimate {
        @Override
        public String toString() {
            return "unknown";
        }
    }
}
