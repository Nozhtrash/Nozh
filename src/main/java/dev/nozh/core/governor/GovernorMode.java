package dev.nozh.core.governor;

/**
 * Governor operational mode (Contract 6).
 * 
 * Determines how autonomous the governor is.
 */
public enum GovernorMode {
    /**
     * Governor completely disabled.
     * No decisions, no recommendations.
     */
    OFF,

    /**
     * Governor analyzes and recommends, but never acts.
     * User must manually approve each action.
     */
    MANUAL_ASSIST,

    /**
     * Governor acts autonomously within conservative bounds.
     * - Only Tier 0-1 actions
     * - High confidence threshold (>= 0.8)
     * - Safe actions only
     * DEFAULT MODE.
     */
    AUTO_CONSERVATIVE,

    /**
     * Governor acts more aggressively.
     * - Tier 0-2 actions
     * - Moderate confidence threshold (>= 0.6)
     * - Allows SAFE_WITH_VISUAL_CHANGE
     */
    AUTO_AGGRESSIVE
}
