package dev.nozh.core.capability;

/**
 * Interface for all optimization capability providers.
 * <p>
 * Each provider represents a specific optimization action (e.g., reduce render distance,
 * disable particles, etc.) that can be executed to improve FPS.
 * <p>
 * Providers must be:
 * - Idempotent: Multiple execute() calls should be safe
 * - Fast: Execute in < 100ms to avoid blocking
 * - Safe: Handle all exceptions internally
 */
public interface OptimizationProvider {

    /**
     * Get the unique identifier for this provider.
     * <p>
     * This ID is used by the ProviderRegistry and ActionProviderMapping
     * to link actions to their providers.
     *
     * @return Provider ID (e.g., "render_distance", "particles")
     */
    String getId();

    /**
     * Get human-readable name of this provider.
     *
     * @return Display name (e.g., "Render Distance", "Particle Effects")
     */
    String getName();

    /**
     * Get detailed description of what this provider does.
     *
     * @return Description
     */
    String getDescription();

    /**
     * Check if this provider can be executed in the current state.
     * <p>
     * Example reasons to return false:
     * - Minecraft client is null
     * - Required settings are unavailable
     * - Provider is disabled by config
     *
     * @return true if execute() can be called, false otherwise
     */
    boolean canExecute();

    /**
     * Execute the optimization action.
     * <p>
     * This method must:
     * - Modify Minecraft settings/state to improve FPS
     * - Return true if successful, false if failed
     * - Handle all exceptions internally (don't throw)
     * - Complete quickly (< 100ms)
     * - Be idempotent (safe to call multiple times)
     *
     * @return true if optimization was applied successfully, false otherwise
     */
    boolean execute();

    /**
     * Get expected FPS impact of this optimization.
     * <p>
     * This is a rough estimate used for decision-making.
     *
     * @return Expected FPS improvement (e.g., 5.0 for +5 FPS)
     */
    default double getExpectedFpsImpact() {
        return 2.0; // Default: +2 FPS
    }

    /**
     * Get the category of this provider.
     * <p>
     * Used for grouping and filtering providers.
     *
     * @return Category (e.g., "rendering", "particles", "entities")
     */
    default String getCategory() {
        return "general";
    }

    /**
     * Check if this provider is reversible.
     * <p>
     * Reversible providers can be undone (e.g., increase render distance back).
     *
     * @return true if optimization can be reversed
     */
    default boolean isReversible() {
        return true;
    }
}