package dev.nozh.core.governor;

import java.util.Set;

/**
 * Mode policy configuration (Contract 6).
 * 
 * Specifies what actions are allowed in a given GovernorMode.
 * Immutable.
 */
public record ModePolicy(
        Set<Integer> allowedTiers,
        double minConfidence,
        boolean allowExperimental,
        boolean requiresUserConfirmation) {
    /**
     * Policy for OFF mode (no actions).
     */
    public static ModePolicy off() {
        return new ModePolicy(Set.of(), 1.0, false, true);
    }

    /**
     * Policy for MANUAL_ASSIST mode (recommend only).
     */
    public static ModePolicy manualAssist() {
        return new ModePolicy(
                Set.of(0, 1, 2, 3), // All tiers
                0.5, // Low threshold for recommendations
                true, // Can show experimental
                true // Always requires confirmation
        );
    }

    /**
     * Policy for AUTO_CONSERVATIVE mode (DEFAULT).
     */
    public static ModePolicy autoConservative() {
        return new ModePolicy(
                Set.of(0, 1), // Only Tier 0-1
                0.8, // High confidence required
                false, // No experimental
                false // No confirmation (autonomous)
        );
    }

    /**
     * Policy for AUTO_AGGRESSIVE mode.
     */
    public static ModePolicy autoAggressive() {
        return new ModePolicy(
                Set.of(0, 1, 2), // Tier 0-2
                0.6, // Moderate confidence
                false, // No experimental
                false // No confirmation
        );
    }

    /**
     * Get policy for a mode.
     */
    public static ModePolicy forMode(GovernorMode mode) {
        return switch (mode) {
            case OFF -> off();
            case MANUAL_ASSIST -> manualAssist();
            case AUTO_CONSERVATIVE -> autoConservative();
            case AUTO_AGGRESSIVE -> autoAggressive();
        };
    }

    /**
     * Ensure manual mode preference is respected when auto-tuning is disabled.
     */
    public static GovernorMode enforceManualPreference(GovernorMode mode, boolean autoTuningEnabled) {
        if (!autoTuningEnabled && mode != GovernorMode.OFF) {
            return GovernorMode.MANUAL_ASSIST;
        }
        return mode;
    }
}
