package dev.nozh.core.capability;

/**
 * Rollback guarantee level for capability providers.
 * 
 * Contract 3: Provider Guarantees
 * Indicates provider's ability to rollback a failed apply() operation.
 */
public enum RollbackGuarantee {
    /**
     * Provider GUARANTEES rollback will succeed.
     * Provider captures previous value atomically and can restore it.
     * 
     * Example: Simple option toggle (particles ON/OFF).
     */
    STRONG,

    /**
     * Provider will ATTEMPT rollback but may fail.
     * Rollback depends on external state or complex conditions.
     * 
     * Example: Render distance (may fail if memory constraints changed).
     */
    BEST_EFFORT,
    /**
     * Legacy alias for BEST_EFFORT.
     */
    @Deprecated
    WEAK,

    /**
     * Provider CANNOT rollback.
     * Change is irreversible or rollback is not implemented.
     * 
     * Example: One-time initialization, file writes.
     */
    NONE
}
