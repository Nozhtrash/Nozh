package dev.nozh.core.capability;

/**
 * Safety level classification for capability changes.
 * 
 * Contract 3: Provider Guarantees
 * This enum categorizes the risk profile of applying a capability change.
 * 
 * Used by ProviderMetadata to communicate safety to Governor and HUD.
 */
public enum SafetyLevel {
    /**
     * Completely safe, no side effects, fully reversible.
     * Example: toggling a cosmetic HUD element.
     */
    SAFE,

    /**
     * Safe but causes visual changes that may be noticeable.
     * Example: changing particles from ALL to MINIMAL.
     */
    SAFE_WITH_VISUAL_CHANGE,

    /**
     * May have gameplay implications or moderate side effects.
     * Example: disabling entity shadows (affects mob visibility).
     */
    RISKY,

    /**
     * Experimental or unstable. May crash, corrupt save, or break mods.
     * Example: changing render distance below safe thresholds.
     */
    EXPERIMENTAL
}
