package dev.nozh.fabric.compat;

import dev.nozh.NozhConstants;

/**
 * EntityCulling mod adapter.
 * 
 * EntityCulling mod culls entities behind blocks.
 * If present, NOZH can be less aggressive with entity culling
 * since the mod handles it efficiently.
 * 
 * TASK 5: Real orchestration - EntityCulling detection
 */
public final class EntityCullingAdapter {

    private static boolean entityCullingDetected = false;
    private static boolean initialized = false;

    /**
     * Detect if EntityCulling mod is loaded.
     */
    public static boolean initialize() {
        if (initialized) {
            return entityCullingDetected;
        }

        try {
            // Try to load EntityCulling's main class
            Class.forName("net.entityculling.EntityCulling");
            entityCullingDetected = true;
            NozhConstants.LOGGER.info("EntityCulling detected - will defer occlusion culling");
            initialized = true;
            return true;

        } catch (ClassNotFoundException e) {
            entityCullingDetected = false;
            initialized = true;
            NozhConstants.LOGGER.info("EntityCulling not detected - NOZH will handle entity culling");
            return false;
        }
    }

    /**
     * Check if EntityCulling is active.
     */
    public static boolean isEntityCullingActive() {
        if (!initialized) {
            initialize();
        }
        return entityCullingDetected;
    }

    /**
     * Check if NOZH should defer entity culling to EntityCulling mod.
     */
    public static boolean shouldDeferCulling() {
        return isEntityCullingActive();
    }

    /**
     * Get estimated FPS impact of EntityCulling.
     * EntityCulling provides ~5-15% FPS boost in entity-dense scenarios.
     */
    public static double getOptimizationImpact() {
        if (!isEntityCullingActive()) {
            return 0.0;
        }
        return 0.10; // 10% average estimate
    }
}
