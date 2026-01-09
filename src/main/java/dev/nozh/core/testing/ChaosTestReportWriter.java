package dev.nozh.core.testing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ChaosTestReportWriter {
    private ChaosTestReportWriter() {
    }

    public static void writeJson(Path outputDir, ChaosTestReport report) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Files.createDirectories(outputDir);
        Map<String, Object> payload = new HashMap<>();
        payload.put("generated_at", Instant.now().toString());
        payload.put("summary", buildSummary(report));
        payload.put("results", report.results());
        payload.put("metadata", report.metadata());
        Files.writeString(outputDir.resolve("chaos-test-report.json"), gson.toJson(payload));
    }

    public static void writeCsv(Path outputDir, ChaosTestReport report) throws IOException {
        Files.createDirectories(outputDir);
        Path output = outputDir.resolve("chaos-test-report.csv");
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(output))) {
            writer.println("scenario,passed,failure_reason,duration_ms");
            for (ChaosScenarioResult result : report.results()) {
                writer.printf(
                    "%s,%s,%s,%d%n",
                    result.scenario().name(),
                    result.passed(),
                    escapeCsv(result.failureReason()),
                    result.durationMs()
                );
            }
        }
    }

    private static Map<String, Object> buildSummary(ChaosTestReport report) {
        List<ChaosScenarioResult> results = report.results();
        List<Long> durations = results.stream().map(ChaosScenarioResult::durationMs).toList();
        Map<String, Object> summary = new HashMap<>();
        summary.put("total_scenarios", report.totalScenarios());
        summary.put("passed", report.passed());
        summary.put("failed", report.failed());
        summary.put("total_duration_ms", report.totalDurationMs());
        summary.put("p95_duration_ms", percentile(durations, 0.95));
        summary.put("p99_duration_ms", percentile(durations, 0.99));
        return summary;
    }

    private static long percentile(List<Long> values, double percentile) {
        if (values.isEmpty()) {
            return 0L;
        }
        List<Long> sorted = values.stream().sorted().toList();
        int index = (int) Math.ceil(percentile * sorted.size()) - 1;
        int safeIndex = Math.min(Math.max(index, 0), sorted.size() - 1);
        return sorted.get(safeIndex);
    }

    private static String escapeCsv(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return '"' + escaped + '"';
    }
}
