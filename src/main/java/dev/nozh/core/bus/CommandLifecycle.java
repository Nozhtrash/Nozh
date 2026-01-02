package dev.nozh.core.bus;

/**
 * Command lifecycle states (Contract 2).
 * 
 * State machine:
 * QUEUED → VALIDATED → EXECUTING → (SUCCESS | FAILED | ROLLED_BACK | ABORTED)
 */
public enum CommandLifecycle {
    /**
     * Command enqueued, awaiting validation.
     */
    QUEUED,

    /**
     * Command validated, awaiting execution.
     */
    VALIDATED,

    /**
     * Command currently executing.
     */
    EXECUTING,

    /**
     * Command executed successfully.
     */
    SUCCESS,

    /**
     * Command execution failed.
     */
    FAILED,

    /**
     * Command executed but was rolled back due to no improvement.
     */
    ROLLED_BACK,

    /**
     * Command aborted before execution (e.g., SafeMode activated).
     */
    ABORTED
}
