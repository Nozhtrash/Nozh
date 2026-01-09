package dev.nozh.core.benchmark;

import java.util.Objects;

/**
 * Benchmark scenario definition for workload coverage.
 */
public record BenchmarkScenario(
        String name,
        String description,
        int targetDurationSeconds) {

    public BenchmarkScenario {
        name = normalize(name, "UNKNOWN");
        description = normalize(description, "");
        if (targetDurationSeconds <= 0) {
            targetDurationSeconds = 60;
        }
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"name\":\"").append(escapeJson(name)).append("\",");
        sb.append("\"description\":\"").append(escapeJson(description)).append("\",");
        sb.append("\"targetDurationSeconds\":").append(targetDurationSeconds);
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String value) {
        return Objects.toString(value, "")
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
