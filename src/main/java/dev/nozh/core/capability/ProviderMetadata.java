package dev.nozh.core.capability;

import java.util.Set;

/**
 * Provider metadata interface (Contract 3).
 * 
 * Declares all static characteristics of a CapabilityProvider.
 * Used by Governor to make policy decisions and by HUD to display information.
 * 
 * MUST be deterministic and immutable.
 */
public interface ProviderMetadata {

    /**
     * Side effects of applying this capability.
     */
    SideEffects sideEffects();

    /**
     * Safety level classification.
     */
    SafetyLevel safetyLevel();

    /**
     * Rollback guarantee.
     */
    RollbackGuarantee rollbackGuarantee();

    /**
     * Expected gameplay impact level.
     */
    ImpactLevel gameplayImpact();

    /**
     * Estimated cost level (quality or disruption cost).
     */
    CostLevel costLevel();

    /**
     * Expected visual impact level.
     */
    ImpactLevel visualImpact();

    /**
     * Estimated performance gain in milliseconds (frametime reduction).
     * 
     * 0.0 = unknown or negligible
     * Positive = performance improvement expected
     * Negative = performance degradation expected
     * 
     * This is ESTIMATE ONLY, not a guarantee.
     */
    double expectedGainMs();

    /**
     * Required mods for this provider to function.
     * Empty set = no mod dependencies.
     */
    Set<String> requiredMods();

    /**
     * Known conflicting mods that may break this provider.
     * Empty set = no known conflicts.
     */
    Set<String> conflictingMods();
}
