package dev.nozh.api.governor;

/**
 * Scope of a decision.
 */
public enum ActionScope {
    WORLD, // Persists for the world load
    SESSION // Persists for the game session
}
