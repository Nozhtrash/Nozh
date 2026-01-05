package dev.nozh.core.capability;

/**
 * Cost level classification for applying a capability change.
 *
 * Represents the relative disruption or quality cost imposed on the player.
 */
public enum CostLevel {
    /**
     * Minimal or no noticeable cost.
     */
    LOW,

    /**
     * Noticeable but acceptable cost.
     */
    MED,

    /**
     * High cost with strong quality or gameplay trade-offs.
     */
    HIGH
}
