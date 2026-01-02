package dev.nozh.core.compat;

import dev.nozh.NozhConstants;

import java.lang.reflect.Method;
import java.util.Optional;

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
        return getShaderStatus().orElseGet(IrisCompat::isPresent);
    }

    /**
     * Try to read Iris shader status via reflection.
     */
    public static Optional<Boolean> getShaderStatus() {
        if (!isPresent()) {
            return Optional.empty();
        }

        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Method instanceMethod = apiClass.getMethod("getInstance");
            Object apiInstance = instanceMethod.invoke(null);
            Method statusMethod = findStatusMethod(apiClass);
            if (statusMethod != null) {
                Object result = statusMethod.invoke(apiInstance);
                if (result instanceof Boolean) {
                    return Optional.of((Boolean) result);
                }
            }
        } catch (ClassNotFoundException ignored) {
            // API not available
        } catch (Exception e) {
            NozhConstants.LOGGER.debug("Iris API reflection failed: {}", e.getMessage());
        }

        return Optional.empty();
    }

    private static Method findStatusMethod(Class<?> apiClass) {
        String[] candidates = {
                "isShaderPackInUse",
                "isShaderPackEnabled",
                "isShaderPackActive",
                "areShadersEnabled"
        };
        for (String name : candidates) {
            try {
                return apiClass.getMethod(name);
            } catch (NoSuchMethodException ignored) {
                // Try next
            }
        }
        return null;
    }

    // Future: Methods to detect shader pack, check if shaders enabled
    // For MVP, we just detect and assume shaders might be active
}
