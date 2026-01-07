package dev.nozh.core.benchmark;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Metadata for a benchmark artifact bundle.
 */
public record BenchmarkArtifactMetadata(
        BenchmarkEnvironment environment,
        String scenario,
        long scenarioStartMillis,
        long scenarioEndMillis,
        Path telemetryCsv,
        Path telemetryJson,
        Path snapshotsJson,
        Path initialBenchmarkSnapshotJson,
        Path decisionLatencyJson) {

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"environment\": ").append(environment != null ? environment.toJson() : "null").append(",\n");
        sb.append("  \"scenario\": \"").append(escapeJson(scenario)).append("\",\n");
        sb.append("  \"scenarioStartMillis\": ").append(scenarioStartMillis).append(",\n");
        sb.append("  \"scenarioEndMillis\": ").append(scenarioEndMillis).append(",\n");
        sb.append("  \"telemetryCsv\": ").append(formatPath(telemetryCsv)).append(",\n");
        sb.append("  \"telemetryJson\": ").append(formatPath(telemetryJson)).append(",\n");
        sb.append("  \"snapshotsJson\": ").append(formatPath(snapshotsJson)).append(",\n");
        sb.append("  \"initialBenchmarkSnapshotJson\": ").append(formatPath(initialBenchmarkSnapshotJson)).append(",\n");
        sb.append("  \"decisionLatencyJson\": ").append(formatPath(decisionLatencyJson)).append("\n");
        sb.append("}\n");
        return sb.toString();
    }

    private String formatPath(Path path) {
        if (path == null) {
            return "null";
        }
        return "\"" + escapeJson(path.toString()) + "\"";
    }

    private static String escapeJson(String value) {
        return Objects.toString(value, "")
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
