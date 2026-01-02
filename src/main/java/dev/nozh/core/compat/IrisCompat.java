package dev.nozh.core.compat;

import dev.nozh.NozhConstants;

/**
 * Iris compatibility stub.
 * MVP: Detection only, no shader manipulation.
 */
public final class IrisCompat {
    
    private static boolean detected = false;
    private static boolean checked = false;
    
    private IrisCompat() {
        // Utility class
    }
    
    /**
     * Check if Iris is present
     */
    public static boolean isPresent() {
        if (!checked) {
            detected = ModDetector.isModLoaded("iris");
            checked = true;
            if (detected) {
                NozhConstants.LOGGER.debug("Iris detected - NOZH will account for shader overhead");
            }
        }
        return detected;
    }
    
    /**
     * Check if shaders are likely active (heuristic)
     * MVP: Just returns presence of Iris - cannot detect active shaders without API
     */
    public static boolean areShadersLikelyActive() {
        return isPresent(); // Conservative assumption
    }
    
    // Future: Methods to detect shader pack, check if shaders enabled
    // For MVP, we just detect and assume shaders might be active
}
