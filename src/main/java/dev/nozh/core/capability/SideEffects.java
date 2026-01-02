package dev.nozh.core.capability;

/**
 * Side effects declaration for capability changes.
 * 
 * Contract 3: Provider Guarantees
 * Immutable record documenting all known side effects of applying a capability.
 * 
 * Used by Governor to enforce policies (e.g., no restart-required changes in
 * AUTO mode).
 */
public record SideEffects(
        /**
         * Whether this capability modifies options.txt or similar config files.
         */
        boolean touchesOptions,

        /**
         * Whether applying this capability requires a game restart to take effect.
         */
        boolean requiresRestart,

        /**
         * Whether this capability may increase input lag.
         * Example: V-Sync changes, triple buffering.
         */
        boolean affectsInputLag,

        /**
         * Whether this capability may break determinism/replay systems.
         * Example: Random seed changes, unstable render tweaks.
         */
        boolean breaksDeterminism) {
    /**
     * No side effects (safest).
     */
    public static SideEffects none() {
        return new SideEffects(false, false, false, false);
    }

    /**
     * Only touches options file (common case).
     */
    public static SideEffects optionsOnly() {
        return new SideEffects(true, false, false, false);
    }
}
