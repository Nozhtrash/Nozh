package dev.nozh.core.benchmark;

import java.util.Objects;

/**
 * Benchmark environment descriptor (hardware, resolution, mods, workload).
 */
public record BenchmarkEnvironment(
        String hardwareProfile,
        String resolution,
        String mods,
        String workloadProfile) {

    public BenchmarkEnvironment {
        hardwareProfile = normalize(hardwareProfile, "unknown");
        resolution = normalize(resolution, "unknown");
        mods = normalize(mods, "unknown");
        workloadProfile = normalize(workloadProfile, "default");
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"hardwareProfile\": \"").append(escapeJson(hardwareProfile)).append("\",\n");
        sb.append("  \"resolution\": \"").append(escapeJson(resolution)).append("\",\n");
        sb.append("  \"mods\": \"").append(escapeJson(mods)).append("\",\n");
        sb.append("  \"workloadProfile\": \"").append(escapeJson(workloadProfile)).append("\"\n");
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String value) {
        return Objects.toString(value, "")
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
