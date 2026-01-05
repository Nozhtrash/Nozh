package dev.nozh.fabric.compat;

import dev.nozh.NozhConstants;
import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;

public final class SodiumOptionsAdapter implements CompatAdapter {

    private static final String MOD_ID = "sodium";
    private static final String CLIENT_MOD_CLASS = "me.jellysquid.mods.sodium.client.SodiumClientMod";

    private static final EnumSet<CapabilityId> SUPPORTED = EnumSet.of(
            CapabilityId.CLOUDS,
            CapabilityId.SMOOTH_LIGHTING,
            CapabilityId.MIPMAP_LEVEL);

    @Override
    public String modId() {
        return MOD_ID;
    }

    @Override
    public EnumSet<CapabilityId> supportedCapabilities() {
        return SUPPORTED;
    }

    @Override
    public boolean isAvailable() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID) && resolveOptions().isPresent();
    }

    @Override
    public Optional<CapabilityValue> getCurrentValue(CapabilityId capability) {
        Optional<Object> options = resolveOptions();
        if (options.isEmpty()) {
            return Optional.empty();
        }
        return switch (capability) {
            case CLOUDS -> readClouds(options.get());
            case SMOOTH_LIGHTING -> readSmoothLighting(options.get());
            case MIPMAP_LEVEL -> readMipmapLevels(options.get());
            default -> Optional.empty();
        };
    }

    @Override
    public boolean apply(CapabilityId capability, CapabilityValue value) {
        Optional<Object> options = resolveOptions();
        if (options.isEmpty()) {
            return false;
        }
        return switch (capability) {
            case CLOUDS -> writeClouds(options.get(), value);
            case SMOOTH_LIGHTING -> writeSmoothLighting(options.get(), value);
            case MIPMAP_LEVEL -> writeMipmapLevels(options.get(), value);
            default -> false;
        };
    }

    private Optional<Object> resolveOptions() {
        try {
            Class<?> apiClass = Class.forName(CLIENT_MOD_CLASS);
            for (String methodName : new String[] { "options", "getOptions", "getConfig", "config" }) {
                try {
                    Method method = apiClass.getDeclaredMethod(methodName);
                    method.setAccessible(true);
                    Object result = method.invoke(null);
                    if (result != null) {
                        return Optional.of(result);
                    }
                } catch (ReflectiveOperationException ignored) {
                }
            }
        } catch (ClassNotFoundException ignored) {
        } catch (Exception e) {
            NozhConstants.LOGGER.debug("Sodium options lookup failed: {}", e.getMessage());
        }
        return Optional.empty();
    }

    private Optional<CapabilityValue> readClouds(Object options) {
        Optional<Object> quality = resolveSection(options, "quality", "graphics", "qualitySettings");
        Optional<Object> fieldValue = readFieldValue(quality.orElse(options),
                "clouds", "cloudsQuality", "cloudsMode");
        if (fieldValue.isEmpty()) {
            return Optional.empty();
        }
        Object value = fieldValue.get();
        if (value instanceof Enum<?> enumValue) {
            return mapCloudEnum(enumValue.name())
                    .map(CapabilityValue.EnumValue::new);
        }
        if (value instanceof Boolean boolValue) {
            return Optional.of(new CapabilityValue.EnumValue(boolValue ? "FANCY" : "OFF"));
        }
        return Optional.empty();
    }

    private boolean writeClouds(Object options, CapabilityValue value) {
        if (!(value instanceof CapabilityValue.EnumValue enumValue)) {
            return false;
        }
        Optional<Object> quality = resolveSection(options, "quality", "graphics", "qualitySettings");
        return writeEnumField(quality.orElse(options), enumValue.name(),
                "clouds", "cloudsQuality", "cloudsMode")
                || writeBooleanField(quality.orElse(options),
                        !"OFF".equals(enumValue.name()),
                        "cloudsEnabled", "clouds", "cloudsQuality");
    }

    private Optional<CapabilityValue> readSmoothLighting(Object options) {
        Optional<Object> quality = resolveSection(options, "quality", "graphics", "qualitySettings");
        Optional<Object> fieldValue = readFieldValue(quality.orElse(options),
                "smoothLighting", "smoothLightingQuality", "smoothLightingMode");
        if (fieldValue.isEmpty()) {
            return Optional.empty();
        }
        Object value = fieldValue.get();
        if (value instanceof Boolean boolValue) {
            return Optional.of(new CapabilityValue.BoolValue(boolValue));
        }
        if (value instanceof Enum<?> enumValue) {
            boolean enabled = !enumValue.name().toUpperCase(Locale.ROOT).contains("OFF");
            return Optional.of(new CapabilityValue.BoolValue(enabled));
        }
        return Optional.empty();
    }

    private boolean writeSmoothLighting(Object options, CapabilityValue value) {
        if (!(value instanceof CapabilityValue.BoolValue boolValue)) {
            return false;
        }
        Optional<Object> quality = resolveSection(options, "quality", "graphics", "qualitySettings");
        return writeBooleanField(quality.orElse(options), boolValue.value(),
                "smoothLighting", "smoothLightingQuality", "smoothLightingMode")
                || writeEnumField(quality.orElse(options), boolValue.value() ? "ON" : "OFF",
                        "smoothLighting", "smoothLightingQuality", "smoothLightingMode");
    }

    private Optional<CapabilityValue> readMipmapLevels(Object options) {
        Optional<Object> quality = resolveSection(options, "quality", "graphics", "qualitySettings");
        Optional<Object> fieldValue = readFieldValue(quality.orElse(options),
                "mipmapLevels", "mipmapLevel", "mipmap");
        if (fieldValue.isEmpty()) {
            return Optional.empty();
        }
        Object value = fieldValue.get();
        if (value instanceof Number number) {
            return Optional.of(new CapabilityValue.IntValue(number.intValue()));
        }
        return Optional.empty();
    }

    private boolean writeMipmapLevels(Object options, CapabilityValue value) {
        if (!(value instanceof CapabilityValue.IntValue intValue)) {
            return false;
        }
        Optional<Object> quality = resolveSection(options, "quality", "graphics", "qualitySettings");
        return writeIntField(quality.orElse(options), intValue.value(),
                "mipmapLevels", "mipmapLevel", "mipmap");
    }

    private Optional<Object> resolveSection(Object options, String... candidates) {
        for (String candidate : candidates) {
            Optional<Object> fieldValue = readFieldValue(options, candidate);
            if (fieldValue.isPresent()) {
                return fieldValue;
            }
            Optional<Object> methodValue = readMethodValue(options, candidate);
            if (methodValue.isPresent()) {
                return methodValue;
            }
        }
        return Optional.empty();
    }

    private Optional<Object> readFieldValue(Object target, String... candidates) {
        for (String candidate : candidates) {
            Field field = findField(target, candidate);
            if (field == null) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object result = field.get(target);
                if (result != null) {
                    return Optional.of(result);
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        return Optional.empty();
    }

    private Optional<Object> readMethodValue(Object target, String methodName) {
        String getter = "get" + methodName.substring(0, 1).toUpperCase(Locale.ROOT) + methodName.substring(1);
        for (String name : new String[] { methodName, getter }) {
            try {
                Method method = target.getClass().getMethod(name);
                Object result = method.invoke(target);
                if (result != null) {
                    return Optional.of(result);
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return Optional.empty();
    }

    private Field findField(Object target, String name) {
        for (Field field : target.getClass().getDeclaredFields()) {
            if (field.getName().equalsIgnoreCase(name)) {
                return field;
            }
        }
        return null;
    }

    private Optional<String> mapCloudEnum(String enumName) {
        String upper = enumName.toUpperCase(Locale.ROOT);
        if (upper.contains("OFF")) {
            return Optional.of("OFF");
        }
        if (upper.contains("FAST")) {
            return Optional.of("FAST");
        }
        if (upper.contains("FANCY") || upper.contains("FULL")) {
            return Optional.of("FANCY");
        }
        return Optional.empty();
    }

    private boolean writeEnumField(Object target, String desiredName, String... candidates) {
        for (String candidate : candidates) {
            Field field = findField(target, candidate);
            if (field == null || !field.getType().isEnum()) {
                continue;
            }
            Object enumValue = findEnumConstant(field.getType(), desiredName);
            if (enumValue == null) {
                continue;
            }
            try {
                field.setAccessible(true);
                field.set(target, enumValue);
                return true;
            } catch (IllegalAccessException ignored) {
            }
        }
        return false;
    }

    private boolean writeBooleanField(Object target, boolean desiredValue, String... candidates) {
        for (String candidate : candidates) {
            Field field = findField(target, candidate);
            if (field == null) {
                continue;
            }
            if (field.getType() != boolean.class && field.getType() != Boolean.class) {
                continue;
            }
            try {
                field.setAccessible(true);
                field.set(target, desiredValue);
                return true;
            } catch (IllegalAccessException ignored) {
            }
        }
        return false;
    }

    private boolean writeIntField(Object target, int desiredValue, String... candidates) {
        for (String candidate : candidates) {
            Field field = findField(target, candidate);
            if (field == null) {
                continue;
            }
            Class<?> type = field.getType();
            if (type != int.class && type != Integer.class) {
                continue;
            }
            try {
                field.setAccessible(true);
                field.set(target, desiredValue);
                return true;
            } catch (IllegalAccessException ignored) {
            }
        }
        return false;
    }

    private Object findEnumConstant(Class<?> enumClass, String desiredName) {
        Object[] constants = enumClass.getEnumConstants();
        if (constants == null) {
            return null;
        }
        String upper = desiredName.toUpperCase(Locale.ROOT);
        for (Object constant : constants) {
            String name = ((Enum<?>) constant).name().toUpperCase(Locale.ROOT);
            if (name.equals(upper)) {
                return constant;
            }
        }
        for (Object constant : constants) {
            String name = ((Enum<?>) constant).name().toUpperCase(Locale.ROOT);
            if (upper.contains("ON") && !name.contains("OFF")) {
                return constant;
            }
            if (upper.contains("OFF") && name.contains("OFF")) {
                return constant;
            }
        }
        return null;
    }
}
