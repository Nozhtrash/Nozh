package dev.nozh.core.issues;

/**
 * Issue severity (Contract 9).
 */
public enum IssueSeverity {
    /**
     * Informational - no action required, just FYI.
     */
    INFO,

    /**
     * Warning - something suboptimal detected, user should know.
     */
    WARNING,

    /**
     * Critical - system is degraded or in unsafe state.
     */
    CRITICAL
}
