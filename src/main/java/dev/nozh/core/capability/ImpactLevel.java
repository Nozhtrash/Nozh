package dev.nozh.core.capability;

/**
 * Impact level classification for gameplay and visual effects.
 * 
 * Contract 3: Provider Guarantees
 * Quantifies the magnitude of change a capability modification creates.
 */
public enum ImpactLevel {
    /**
     * No observable impact.
     * Example: Internal telemetry flag.
     */
    NONE,

    /**
     * Minor impact, barely noticeable.
     * Example: Slight FPS change (1-3 frames).
     */
    LOW,

    /**
     * Moderate impact, clearly noticeable.
     * Example: Visual quality change (particles DECREASED).
     */
    MED,

    /**
     * High impact, dramatic change.
     * Example: Render distance cut in half, major FPS gain/loss.
     */
    HIGH
}
