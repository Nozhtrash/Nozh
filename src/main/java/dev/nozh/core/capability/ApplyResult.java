package dev.nozh.core.capability;

import dev.nozh.core.capability.CapabilityValue;

/**
 * Result of a CapabilityProvider.apply() operation (Contract 3).
 * 
 * Sealed type hierarchy for structured error handling.
 * Providers MUST return one of these, never throw exceptions upward.
 */
public sealed interface ApplyResult {

    /**
     * Apply succeeded.
     * 
     * @param previousValue Value before apply (for rollback tracking)
     * @param newValue      Value after apply (confirmation)
     */
    record Success(
            CapabilityValue previousValue,
            CapabilityValue newValue) implements ApplyResult {
    }

    /**
     * Apply failed, rollback was attempted.
     * 
     * @param reason            Human-readable failure reason
     * @param rollbackAttempted Whether rollback was attempted
     * @param rollbackSucceeded Whether rollback succeeded (only meaningful if
     *                          attempted)
     */
    record Failed(
            String reason,
            boolean rollbackAttempted,
            boolean rollbackSucceeded) implements ApplyResult {
    }

    /**
     * Apply rejected before attempt.
     * Provider determined request is invalid at runtime.
     * 
     * Example: Value out of range, mod missing, MC API unavailable.
     * 
     * @param reason Human-readable rejection reason
     */
    record Rejected(
            String reason) implements ApplyResult {
    }

    // Helper methods

    default boolean succeeded() {
        return this instanceof Success;
    }

    default boolean failed() {
        return this instanceof Failed;
    }

    default boolean rejected() {
        return this instanceof Rejected;
    }
}
