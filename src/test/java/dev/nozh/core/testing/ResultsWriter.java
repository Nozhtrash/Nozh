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

public final class ResultsWriter {
    private ResultsWriter() {
    }

    public static void writePlan(Path outputDir, QuickTestPlan plan) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Path output = outputDir.resolve("quick-test-plan.json");
        Files.createDirectories(outputDir);
        Files.writeString(output, gson.toJson(plan));
    }

    public static void writeResults(
        Path outputDir,
        String modpack,
        String seed,
        List<String> scenarios,
        EnvironmentMetadata metadata,
        QuickTestPlan plan,
        String sampleSource,
        List<BenchmarkResult> results
    ) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> meta = new HashMap<>();
        meta.put("modpack", modpack);
        meta.put("seed", seed);
        meta.put("scenarios", scenarios);
        meta.put("environment", metadata.getValues());
        meta.put("timing", plan.timingMap());
        meta.put("sample_source", sampleSource);
        meta.put("generated_at", Instant.now().toString());
        payload.put("metadata", meta);
        payload.put("results", results);
        Files.createDirectories(outputDir);
        Files.writeString(outputDir.resolve("quick-test-results.json"), gson.toJson(payload));

        writeCsv(outputDir.resolve("quick-test-results.csv"), results);
    }

    private static void writeCsv(Path output, List<BenchmarkResult> results) throws IOException {
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(output))) {
            writer.println("scenario,avg_ms,p95_ms,p99_ms,spike_count,samples,notes");
            for (BenchmarkResult result : results) {
                writer.printf(
                    "%s,%.3f,%.3f,%s,%d,%d,%s%n",
                    result.getScenarioName(),
                    result.getAverageMs(),
                    result.getP95Ms(),
                    result.getP99Ms() == null ? "" : String.format("%.3f", result.getP99Ms()),
                    result.getSpikeCount(),
                    result.getSampleCount(),
                    escapeCsv(result.getNotes())
                );
            }
        }
    }

    private static String escapeCsv(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return '"' + escaped + '"';
    }
}
