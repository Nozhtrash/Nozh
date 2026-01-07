package dev.nozh.core.testing;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EnvironmentMetadata {
    private final Map<String, String> values;

    private EnvironmentMetadata(Map<String, String> values) {
        this.values = Map.copyOf(values);
    }

    public static EnvironmentMetadata fromSystem(Map<String, String> env) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("cpu", env.getOrDefault("NOZH_CPU", "unknown"));
        values.put("gpu", env.getOrDefault("NOZH_GPU", "unknown"));
        values.put("ram", env.getOrDefault("NOZH_RAM", "unknown"));
        values.put("os", env.getOrDefault("NOZH_OS", System.getProperty("os.name", "unknown")));
        values.put("os_arch", System.getProperty("os.arch", "unknown"));
        values.put("java", env.getOrDefault("NOZH_JVM", System.getProperty("java.version", "unknown")));
        values.put("mc_version", env.getOrDefault("NOZH_MC_VERSION", "unknown"));
        values.put("display", env.getOrDefault("NOZH_DISPLAY", "unknown"));
        values.put("renderer", env.getOrDefault("NOZH_RENDERER", "unknown"));
        values.put("shaders", env.getOrDefault("NOZH_SHADERS", "unknown"));
        values.put("power_mode", env.getOrDefault("NOZH_POWER_MODE", "unknown"));
        values.put("background_load", env.getOrDefault("NOZH_BACKGROUND_LOAD", "unknown"));
        values.put("notes", env.getOrDefault("NOZH_ENV_NOTES", ""));
        return new EnvironmentMetadata(values);
    }

    public Map<String, String> getValues() {
        return values;
    }
}
