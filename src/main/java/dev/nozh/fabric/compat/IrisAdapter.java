package dev.nozh.fabric.compat;

import dev.nozh.NozhConstants;

import java.lang.reflect.Method;

/**
 * Iris shader adapter for quality control.
 * 
 * Limited control due to shader complexity:
 * - Detect if shaders are active
 * - Get shader pack name
 * - Reload shaders (apply changes)
 * - Disable shaders (emergency fallback)
 * 
 * Cannot directly modify shader quality settings
 * (those are shader-pack specific).
 * 
 * TASK 5: Real orchestration - Iris integration
 */
public final class IrisAdapter {

    private static Class<?> irisApiClass;
    private static boolean initialized = false;

    /**
     * Initialize Iris reflection.
     */
    public static boolean initialize() {
        if (initialized) {
            return true;
        }

        try {
            irisApiClass = Class.forName("net.coderbot.iris.Iris");
            initialized = true;
            NozhConstants.LOGGER.info("Iris adapter initialized successfully");
            return true;

        } catch (Exception e) {
            NozhConstants.LOGGER.warn("Iris not found or incompatible: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if shaders are currently enabled.
     */
    public static boolean areShadersEnabled() {
        if (!initialized && !initialize()) {
            return false;
        }

        try {
            Method isShaderPackInUse = irisApiClass.getMethod("isShaderPackInUse");
            return (boolean) isShaderPackInUse.invoke(null);
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to check shader status", e);
            return false;
        }
    }

    /**
     * Get current shader pack name.
     */
    public static String getShaderPackName() {
        if (!initialized && !initialize()) {
            return "none";
        }

        try {
            Method getCurrentPackName = irisApiClass.getMethod("getCurrentPackName");
            Object result = getCurrentPackName.invoke(null);
            return result != null ? result.toString() : "none";
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to get shader pack name", e);
            return "unknown";
        }
    }

    /**
     * Reload shaders (apply changes).
     */
    public static boolean reloadShaders() {
        if (!initialized && !initialize()) {
            return false;
        }

        try {
            Method reload = irisApiClass.getMethod("reload");
            reload.invoke(null);
            NozhConstants.LOGGER.info("Iris shaders reloaded");
            return true;
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to reload shaders", e);
            return false;
        }
    }

    /**
     * Disable shaders (emergency fallback).
     */
    public static boolean disableShaders() {
        if (!initialized && !initialize()) {
            return false;
        }

        try {
            // This would require more complex integration
            // For now, just log intent
            NozhConstants.LOGGER.warn("Shader disable requested (not implemented)");
            return false;
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to disable shaders", e);
            return false;
        }
    }

    /**
     * Estimate shader performance impact (0.0-1.0).
     * 0.0 = no impact, 1.0 = heavy impact
     */
    public static double estimateShaderImpact() {
        if (!areShadersEnabled()) {
            return 0.0;
        }

        String packName = getShaderPackName().toLowerCase();

        // Heuristic based on known shader packs
        if (packName.contains("bsl") || packName.contains("complementary")) {
            return 0.6; // Medium-heavy
        }
        if (packName.contains("seus") || packName.contains("continuum")) {
            return 0.9; // Very heavy
        }
        if (packName.contains("sildur") && packName.contains("vibrant")) {
            return 0.7; // Heavy
        }
        if (packName.contains("sildur")) {
            return 0.4; // Medium
        }
        if (packName.contains("vanilla")) {
            return 0.2; // Light
        }

        // Unknown shader - assume medium
        return 0.5;
    }
}
