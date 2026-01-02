package dev.nozh.core.issues;

/**
 * Paranoia level (Contract 9).
 * 
 * Controls how conservative the system behaves.
 */
public enum ParanoiaLevel {
    /**
     * Issue detection disabled.
     */
    OFF,

    /**
     * Normal operation - balanced detection.
     */
    NORMAL,

    /**
     * Strict mode - more conservative governor, stricter benchmarks.
     * Activated after crashes or repeated failures.
     */
    STRICT
}
