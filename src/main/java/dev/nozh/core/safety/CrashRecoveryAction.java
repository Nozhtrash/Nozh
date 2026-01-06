package dev.nozh.core.safety;

/**
 * Crash recovery actions taken after detecting a crash loop.
 */
public enum CrashRecoveryAction {
    NONE,
    QUARANTINED_CAPABILITY,
    SAFE_MODE
}
