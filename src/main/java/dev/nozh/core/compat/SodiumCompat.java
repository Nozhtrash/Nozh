package dev.nozh.core.compat;

import dev.nozh.NozhConstants;

/**
 * Sodium compatibility stub.
 * MVP: Detection only, no settings manipulation.
 */
public final class SodiumCompat {
    
    private static boolean detected = false;
    private static boolean checked = false;
    
    private SodiumCompat() {
        // Utility class
    }
    
    /**
     * Check if Sodium is present
     */
    public static boolean isPresent() {
        if (!checked) {
            detected = ModDetector.isModLoaded("sodium");
            checked = true;
            if (detected) {
                NozhConstants.LOGGER.debug("Sodium detected - NOZH will not interfere with Sodium settings");
            }
        }
        return detected;
    }
    
    /**
     * Check if NOZH should avoid touching render settings
     * (Sodium handles these better)
     */
    public static boolean shouldDeferRenderSettings() {
        return isPresent();
    }
    
    // Future: Methods to read Sodium settings for better heuristics
    // For MVP, we just detect and defer
}
