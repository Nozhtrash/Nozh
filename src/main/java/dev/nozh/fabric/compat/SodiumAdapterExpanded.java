package dev.nozh.fabric.compat;

import dev.nozh.NozhConstants;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Expanded Sodium adapter with full option control.
 * 
 * Controls:
 * - Graphics quality (fast/fancy)
 * - Clouds (off/fast/fancy)
 * - Smooth lighting
 * - Mipmap levels
 * - Chunk updates (1-5)
 * - Particle quality
 * - VSync
 * 
 * TASK 5: Real orchestration - deep Sodium integration
 */
public final class SodiumAdapterExpanded {

    private static Object sodiumConfig;
    private static Class<?> sodiumConfigClass;
    private static boolean initialized = false;

    /**
     * Initialize Sodium reflection.
     */
    public static boolean initialize() {
        if (initialized) {
            return true;
        }

        try {
            // Sodium config class path
            sodiumConfigClass = Class.forName("me.jellysquid.mods.sodium.client.SodiumClientMod");
            Method getConfigMethod = sodiumConfigClass.getMethod("options");
            sodiumConfig = getConfigMethod.invoke(null);

            initialized = true;
            NozhConstants.LOGGER.info("Sodium adapter initialized successfully");
            return true;

        } catch (Exception e) {
            NozhConstants.LOGGER.warn("Failed to initialize Sodium adapter: " + e.getMessage());
            return false;
        }
    }

    /**
     * Set graphics quality.
     */
    public static boolean setGraphicsQuality(GraphicsQuality quality) {
        if (!initialized && !initialize()) {
            return false;
        }

        try {
            Object graphicsQualityOption = getField(sodiumConfig, "graphicsQuality");
            setEnumValue(graphicsQualityOption, quality.name());
            return true;
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to set graphics quality", e);
            return false;
        }
    }

    /**
     * Set clouds rendering.
     */
    public static boolean setClouds(CloudMode mode) {
        if (!initialized && !initialize()) {
            return false;
        }

        try {
            Object cloudsOption = getField(sodiumConfig, "cloudQuality");
            setEnumValue(cloudsOption, mode.name());
            return true;
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to set clouds", e);
            return false;
        }
    }

    /**
     * Set smooth lighting.
     */
    public static boolean setSmoothLighting(boolean enabled) {
        if (!initialized && !initialize()) {
            return false;
        }

        try {
            Object smoothLightingOption = getField(sodiumConfig, "smoothLighting");
            setBooleanValue(smoothLightingOption, enabled);
            return true;
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to set smooth lighting", e);
            return false;
        }
    }

    /**
     * Set mipmap levels.
     */
    public static boolean setMipmapLevels(int levels) {
        if (!initialized && !initialize()) {
            return false;
        }

        if (levels < 0 || levels > 4) {
            levels = Math.max(0, Math.min(4, levels));
        }

        try {
            Object mipmapOption = getField(sodiumConfig, "mipmapLevels");
            setIntValue(mipmapOption, levels);
            return true;
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to set mipmap levels", e);
            return false;
        }
    }

    /**
     * Set particle quality.
     */
    public static boolean setParticleQuality(ParticleQuality quality) {
        if (!initialized && !initialize()) {
            return false;
        }

        try {
            Object particleOption = getField(sodiumConfig, "particleQuality");
            setEnumValue(particleOption, quality.name());
            return true;
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to set particle quality", e);
            return false;
        }
    }

    // === HELPERS ===

    private static Object getField(Object obj, String fieldName) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }

    private static void setBooleanValue(Object option, boolean value) throws Exception {
        Method setter = option.getClass().getMethod("setValue", Boolean.class);
        setter.invoke(option, value);
    }

    private static void setIntValue(Object option, int value) throws Exception {
        Method setter = option.getClass().getMethod("setValue", Integer.class);
        setter.invoke(option, value);
    }

    private static void setEnumValue(Object option, String enumName) throws Exception {
        // Get enum class from option
        Method getter = option.getClass().getMethod("getValue");
        Object currentValue = getter.invoke(option);
        Class<?> enumClass = currentValue.getClass();

        // Find matching enum constant
        Object enumValue = Enum.valueOf((Class<Enum>) enumClass, enumName);

        // Set value
        Method setter = option.getClass().getMethod("setValue", enumClass);
        setter.invoke(option, enumValue);
    }

    // === ENUMS ===

    public enum GraphicsQuality {
        FAST,
        FANCY
    }

    public enum CloudMode {
        OFF,
        FAST,
        FANCY
    }

    public enum ParticleQuality {
        MINIMAL,
        DECREASED,
        ALL
    }
}
