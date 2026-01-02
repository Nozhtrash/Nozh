package dev.nozh.core.compat;

import dev.nozh.NozhConstants;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Method;
import java.util.Optional;

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

    /**
     * Try to read Sodium version via loader API.
     */
    public static Optional<String> getVersion() {
        return FabricLoader.getInstance()
                .getModContainer("sodium")
                .map(container -> container.getMetadata().getVersion().getFriendlyString());
    }

    /**
     * Try to detect advanced Sodium API hints via reflection.
     */
    public static Optional<String> getRendererInfo() {
        if (!isPresent()) {
            return Optional.empty();
        }

        try {
            Class<?> apiClass = Class.forName("me.jellysquid.mods.sodium.client.SodiumClientMod");
            Method versionMethod = findMethod(apiClass, "getVersion");
            if (versionMethod != null) {
                Object result = versionMethod.invoke(null);
                if (result != null) {
                    return Optional.of(result.toString());
                }
            }
        } catch (ClassNotFoundException ignored) {
            // API not available
        } catch (Exception e) {
            NozhConstants.LOGGER.debug("Sodium API reflection failed: {}", e.getMessage());
        }

        return Optional.empty();
    }

    private static Method findMethod(Class<?> apiClass, String name) {
        for (Method method : apiClass.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 0) {
                return method;
            }
        }
        return null;
    }

    // Future: Methods to read Sodium settings for better heuristics
    // For MVP, we just detect and defer
}
