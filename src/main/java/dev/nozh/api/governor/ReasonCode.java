package dev.nozh.api.governor;

/**
 * Machine-readable reasons for decisions.
 */
public enum ReasonCode {
    SATISFIED,
    INSUFFICIENT_DATA,
    SAFE_MODE_ACTIVE,
    TARGET_EXCEEDED,
    P95_SPIKES,
    UNKNOWN_BOUND_CONSERVATIVE
}
