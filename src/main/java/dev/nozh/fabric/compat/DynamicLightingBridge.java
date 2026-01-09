package dev.nozh.fabric.compat;

import dev.nozh.core.capability.CapabilityValue;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Best-effort bridge to dynamic lighting mods (LambDynamicLights).
 */
public final class DynamicLightingBridge {

    private static final String MOD_ID = "lambdynlights";
    private static final String CONFIG_CLASS = "me.lambdaurora.lambdynlights.config.LambDynLightsConfig";

    public boolean isAvailable() {
        return isModLoaded() && resolveConfig().isPresent();
    }

    public Optional<CapabilityValue> getCurrentValue() {
        return resolveConfig()
                .flatMap(config -> readEnabled(config)
                        .map(enabled -> new CapabilityValue.BoolValue(enabled)));
    }

    public boolean apply(CapabilityValue value) {
        if (!(value instanceof CapabilityValue.BoolValue boolValue)) {
            return false;
        }
        Optional<Object> configOpt = resolveConfig();
        if (configOpt.isEmpty()) {
            return false;
        }
        Object config = configOpt.get();
        return writeEnabled(config, boolValue.value());
    }

    private boolean isModLoaded() {
        try {
            return FabricLoader.getInstance().isModLoaded(MOD_ID);
        } catch (Exception e) {
            return false;
        }
    }

    private Optional<Object> resolveConfig() {
        try {
            Class<?> configClass = Class.forName(CONFIG_CLASS);
            for (String methodName : new String[] { "get", "getInstance", "instance" }) {
                try {
                    Method method = configClass.getDeclaredMethod(methodName);
                    method.setAccessible(true);
                    Object result = method.invoke(null);
                    if (result != null) {
                        return Optional.of(result);
                    }
                } catch (ReflectiveOperationException ignored) {
                }
            }
        } catch (ClassNotFoundException ignored) {
        }
        return Optional.empty();
    }

    private Optional<Boolean> readEnabled(Object config) {
        for (String methodName : new String[] { "isEnabled", "isDynamicLightsEnabled", "getEnabled" }) {
            try {
                Method method = config.getClass().getMethod(methodName);
                Object result = method.invoke(config);
                if (result instanceof Boolean boolValue) {
                    return Optional.of(boolValue);
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }

        try {
            Field field = config.getClass().getDeclaredField("enabled");
            field.setAccessible(true);
            Object result = field.get(config);
            if (result instanceof Boolean boolValue) {
                return Optional.of(boolValue);
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return Optional.empty();
    }

    private boolean writeEnabled(Object config, boolean enabled) {
        for (String methodName : new String[] { "setEnabled", "setDynamicLightsEnabled" }) {
            try {
                Method method = config.getClass().getMethod(methodName, boolean.class);
                method.invoke(config, enabled);
                return true;
            } catch (ReflectiveOperationException ignored) {
            }
        }

        try {
            Field field = config.getClass().getDeclaredField("enabled");
            field.setAccessible(true);
            field.set(config, enabled);
            return true;
        } catch (ReflectiveOperationException ignored) {
        }

        return false;
    }
}
