package dev.nozh.fabric.compat;

import dev.nozh.NozhConstants;

/**
 * Lithium adapter for optimization detection.
 * 
 * Lithium optimizes:
 * - Entity AI pathfinding
 * - Chunk tick scheduling
 * - Redstone logic
 * - Block entity updates
 * 
 * We can't directly control Lithium (it auto-optimizes),
 * but we can detect if it's present and factor it into decisions.
 * 
 * TASK 5: Real orchestration - Lithium detection
 */
public final class LithiumAdapter {

    private static boolean lithiumDetected = false;
    private static boolean initialized = false;

    /**
     * Detect if Lithium is loaded.
     */
    public static boolean initialize() {
        if (initialized) {
            return lithiumDetected;
        }

        try {
            // Try to load Lithium's main class
            Class.forName("me.jellysquid.mods.lithium.common.LithiumMod");
            lithiumDetected = true;
            NozhConstants.LOGGER.info("Lithium detected - will factor into optimization decisions");
            initialized = true;
            return true;

        } catch (ClassNotFoundException e) {
            lithiumDetected = false;
            initialized = true;
            NozhConstants.LOGGER.info("Lithium not detected");
            return false;
        }
    }

    /**
     * Check if Lithium is active.
     */
    public static boolean isLithiumActive() {
        if (!initialized) {
            initialize();
        }
        return lithiumDetected;
    }

    /**
     * Get optimization impact estimate.
     * Lithium provides ~10-30% performance boost in logic-heavy scenarios.
     */
    public static double getOptimizationImpact() {
        if (!isLithiumActive()) {
            return 0.0;
        }
        // Conservative estimate: 15% average impact
        return 0.15;
    }

    /**
     * Check if NOZH should avoid optimizing entity AI.
     * Lithium already handles this better than we could.
     */
    public static boolean shouldDeferEntityAI() {
        return isLithiumActive();
    }

    /**
     * Check if NOZH should avoid chunk tick optimization.
     */
    public static boolean shouldDeferChunkTicks() {
        return isLithiumActive();
    }
}
