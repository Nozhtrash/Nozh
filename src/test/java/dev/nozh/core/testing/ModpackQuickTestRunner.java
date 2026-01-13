package dev.nozh.core.testing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ModpackQuickTestRunner {
    private static final int DEFAULT_WARMUP_SECONDS = 60;
    private static final int DEFAULT_MEASUREMENT_SECONDS = 180;
    private static final int DEFAULT_COOLDOWN_SECONDS = 30;

    private ModpackQuickTestRunner() {
    }

    public static void main(String[] args) throws IOException {
        Map<String, String> arguments = parseArgs(args);
        String modpack = arguments.getOrDefault("modpack", "unknown");
        String seed = arguments.getOrDefault("seed", "unknown");
        String outputDir = arguments.getOrDefault("output", "build/test-results/quick-test");
        String sampleDir = arguments.get("samples");
        int warmupSeconds = parseInt(arguments.get("warmupSeconds"), DEFAULT_WARMUP_SECONDS);
        int measurementSeconds = parseInt(arguments.get("measurementSeconds"), DEFAULT_MEASUREMENT_SECONDS);
        int cooldownSeconds = parseInt(arguments.get("cooldownSeconds"), DEFAULT_COOLDOWN_SECONDS);

        Map<String, ScenarioDefinition> catalog = QuickTestScenarioCatalog.defaultScenarios();
        List<ScenarioDefinition> selected = selectScenarios(arguments.get("scenarios"), catalog);

        QuickTestPlan plan = new QuickTestPlan(warmupSeconds, measurementSeconds, cooldownSeconds, selected);
        Path outputPath = Path.of(outputDir);
        ResultsWriter.writePlan(outputPath, plan);

        EnvironmentMetadata metadata = EnvironmentMetadata.fromSystem(System.getenv());

        List<BenchmarkResult> results = new ArrayList<>();
        for (ScenarioDefinition scenario : selected) {
            results.add(runScenario(scenario, sampleDir));
        }

        ResultsWriter.writeResults(
            outputPath,
            modpack,
            seed,
            selected.stream().map(ScenarioDefinition::getId).toList(),
            metadata,
            plan,
            sampleDir == null ? "none" : sampleDir,
            results
        );

        dev.nozh.NozhConstants.LOGGER.info("Quick test plan + results written to {}", outputPath.toAbsolutePath());
    }

    private static BenchmarkResult runScenario(ScenarioDefinition scenario, String sampleDir) throws IOException {
        if (sampleDir == null) {
            return new BenchmarkResult(
                scenario.getId(),
                scenario.getTitle(),
                Double.NaN,
                Double.NaN,
                null,
                0,
                0,
                "No sample data provided; execute scenario and supply samples to compute P95/P99."
            );
        }

        Path samplePath = resolveSamplePath(sampleDir, scenario.getId());
        if (samplePath == null || !Files.exists(samplePath)) {
            return new BenchmarkResult(
                scenario.getId(),
                scenario.getTitle(),
                Double.NaN,
                Double.NaN,
                null,
                0,
                0,
                "Sample file not found for scenario."
            );
        }

        List<Double> samples = readSamples(samplePath);
        FrametimeStats.StatsResult stats = FrametimeStats.compute(samples);
        String notes = stats.p99Valid() ? "" : "P99 requires >= 2000 samples; insufficient sample count.";
        Double p99 = stats.p99Valid() ? stats.p99Ms() : null;
        return new BenchmarkResult(
            scenario.getId(),
            scenario.getTitle(),
            stats.averageMs(),
            stats.p95Ms(),
            p99,
            stats.spikeCount(),
            stats.sampleCount(),
            notes
        );
    }

    private static Path resolveSamplePath(String sampleDir, String scenarioId) {
        Path base = Path.of(sampleDir);
        Path csv = base.resolve(scenarioId + ".csv");
        if (Files.exists(csv)) {
            return csv;
        }
        Path txt = base.resolve(scenarioId + ".txt");
        if (Files.exists(txt)) {
            return txt;
        }
        return null;
    }

    private static List<Double> readSamples(Path samplePath) throws IOException {
        List<String> lines = Files.readAllLines(samplePath);
        List<Double> values = new ArrayList<>();
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            List<String> tokens = Arrays.stream(line.split("[,;\\s]+"))
                .filter(token -> !token.isBlank())
                .collect(Collectors.toList());
            for (String token : tokens) {
                values.add(Double.parseDouble(token));
            }
        }
        return values;
    }

    private static List<ScenarioDefinition> selectScenarios(String scenarioArg, Map<String, ScenarioDefinition> catalog) {
        if (scenarioArg == null || scenarioArg.isBlank()) {
            return new ArrayList<>(catalog.values());
        }
        List<String> ids = Arrays.stream(scenarioArg.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .collect(Collectors.toList());
        return ids.stream()
            .map(id -> Optional.ofNullable(catalog.get(id))
                .orElseThrow(() -> new IllegalArgumentException("Unknown scenario: " + id)))
            .collect(Collectors.toList());
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> values = new LinkedHashMap<>();
        List<String> tokens = new ArrayList<>(Arrays.asList(args));
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (!token.startsWith("--")) {
                continue;
            }
            String key = token.substring(2);
            String value = (i + 1) < tokens.size() ? tokens.get(i + 1) : null;
            if (value != null && !value.startsWith("--")) {
                values.put(key, value);
                i++;
            } else {
                values.put(key, "");
            }
        }
        normalizeArgs(values);
        return values;
    }

    private static void normalizeArgs(Map<String, String> values) {
        rename(values, "scenarios", "scenarios");
        rename(values, "modpack", "modpack");
        rename(values, "seed", "seed");
        rename(values, "output", "output");
        rename(values, "samples", "samples");
        rename(values, "warmup", "warmupSeconds");
        rename(values, "measurement", "measurementSeconds");
        rename(values, "cooldown", "cooldownSeconds");
    }

    private static void rename(Map<String, String> values, String from, String to) {
        if (!Objects.equals(from, to) && values.containsKey(from)) {
            values.putIfAbsent(to, values.remove(from));
        }
    }

    private static int parseInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
