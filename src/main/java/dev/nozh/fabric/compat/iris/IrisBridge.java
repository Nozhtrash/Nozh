package dev.nozh.fabric.compat.iris;

import dev.nozh.NozhConstants;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Advanced Bridge to Iris internals using Reflection.
 * Allows "God Mode" orchestration: dynamic shader toggling and quality
 * adjustment.
 */
public class IrisBridge {

    private static boolean initialized = false;
    private static Object irisConfigInstance = null;
    private static Method setShadersEnabledMethod = null;
    private static Method isShadersEnabledMethod = null;

    public static void init() {
        if (initialized)
            return;

        try {
            // 1. Get Config Instance
            // Checking: net.coderbot.iris.Iris.getIrisConfig()
            Class<?> irisClass = Class.forName("net.coderbot.iris.Iris");
            Method getConfig = irisClass.getMethod("getIrisConfig");
            irisConfigInstance = getConfig.invoke(null);

            if (irisConfigInstance != null) {
                Class<?> configClass = irisConfigInstance.getClass();

                // 2. Find enable/disable methods
                // setShadersEnabled(boolean)
                try {
                    setShadersEnabledMethod = configClass.getMethod("setShadersEnabled", boolean.class);
                    isShadersEnabledMethod = configClass.getMethod("isShadersEnabled");
                } catch (NoSuchMethodException e) {
                    NozhConstants.LOGGER.warn("IrisBridge: Could not find setShadersEnabled");
                }

                // 3. Find Shadow Distance (if available) - usually in a separate config or
                // mixin
                // This is harder to find safely via reflection without source.
                // We'll skip deep field modification for now to avoid crashes.
            }

            initialized = true;
            NozhConstants.LOGGER.info("IrisBridge: Orchestration initialized");
        } catch (Exception e) {
            NozhConstants.LOGGER.warn("IrisBridge: Failed to initialize reflection: " + e.getMessage());
            initialized = true; // Don't retry endlessly
        }
    }

    public static boolean areShadersEnabled() {
        if (!initialized)
            init();
        if (irisConfigInstance == null || isShadersEnabledMethod == null)
            return false;
        try {
            return (boolean) isShadersEnabledMethod.invoke(irisConfigInstance);
        } catch (Exception e) {
            return false;
        }
    }

    public static void setShadersEnabled(boolean enabled) {
        if (!initialized)
            init();
        if (irisConfigInstance == null || setShadersEnabledMethod == null)
            return;

        try {
            boolean current = areShadersEnabled();
            if (current != enabled) {
                setShadersEnabledMethod.invoke(irisConfigInstance, enabled);
                // Iris usually requires a reload/pipeline update after config change
                // We might need to call Iris.reload()
                reloadPipeline();
                NozhConstants.LOGGER.info("IrisBridge: Orchestrator set shaders to " + enabled);
            }
        } catch (Exception e) {
            NozhConstants.LOGGER.error("IrisBridge: Failed to set shaders", e);
        }
    }

    public static void reloadPipeline() {
        try {
            Class<?> irisClass = Class.forName("net.coderbot.iris.Iris");
            Method reload = irisClass.getMethod("reload");
            reload.invoke(null);
        } catch (Exception e) {
            NozhConstants.LOGGER.error("IrisBridge: Failed to reload", e);
        }
    }
}
