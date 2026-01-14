package dev.nozh.core.governor;

/**
 * Represents the current optimization state of the Governor.
 * Used for Schmitt Trigger hysteresis.
 */
public enum GovernorState {
    STABLE,
    OPTIMIZING_MILD,
    OPTIMIZING_SEVERE
}
