package dev.nozh.benchmark;

import dev.nozh.core.math.ExponentialMovingAverage;
import org.junit.jupiter.api.Test;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.stream.Collectors;
import java.util.DoubleSummaryStatistics;

/**
 * Functional Microbenchmark for ExponentialMovingAverage.
 *
 * PHASE 1 HYGIENE CHECK:
 * - Reproducible
 * - Zero Dependencies (JMH-style harness)
 * - JSON Output
 *
 * Usage: Run via gradle test task or main method if configured.
 */
public class NozhTestingBenchmark {

    private static final int WARMUP_ITERATIONS = 5;
    private static final int MEASUREMENT_ITERATIONS = 10;
    private static final int OPS_PER_ITERATION = 10_000_000;

    @Test
    public void runBenchmark() {
        main(new String[0]);
    }

    public static void main(String[] args) {
        dev.nozh.NozhConstants.LOGGER.info("Starting Benchmark: ExponentialMovingAverage...");

        List<Double> throughputs = new ArrayList<>();

        // Warmup
        dev.nozh.NozhConstants.LOGGER.info("Warmup...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            runIteration();
        }

        // Measurement
        dev.nozh.NozhConstants.LOGGER.info("Measurement...");
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            long start = System.nanoTime();
            runIteration();
            long durationNs = System.nanoTime() - start;
            double opsPerSec = (double) OPS_PER_ITERATION / (durationNs / 1_000_000_000.0);
            throughputs.add(opsPerSec);
            dev.nozh.NozhConstants.LOGGER.info("Iter {}: {} ops/s", i + 1, String.format("%.2f", opsPerSec));
        }

        // Stats
        DoubleSummaryStatistics stats = throughputs.stream()
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();

        dev.nozh.NozhConstants.LOGGER.info("Result: {} +/- {} ops/s", String.format("%.2f", stats.getAverage()), String.format("%.2f", calculateStdDev(throughputs, stats.getAverage())));

        // JSON Output
        writeResults(stats.getAverage(), calculateStdDev(throughputs, stats.getAverage()));
    }

    private static void runIteration() {
        ExponentialMovingAverage ema = new ExponentialMovingAverage(0.1);
        for (int j = 0; j < OPS_PER_ITERATION; j++) {
            ema.addSample(j * 1.5);
        }
    }

    private static double calculateStdDev(List<Double> data, double mean) {
        double sumDiffs = data.stream().mapToDouble(d -> Math.pow(d - mean, 2)).sum();
        return Math.sqrt(sumDiffs / (data.size() - 1));
    }

    private static void writeResults(double score, double error) {
        String json = String.format("{\"benchmark\": \"EMA_AddSample\", \"score\": %.2f, \"error\": %.2f, \"unit\": \"ops/s\", \"timestamp\": \"%s\"}",
                score, error, DateTimeFormatter.ISO_INSTANT.format(Instant.now()));

        try {
            Path path = Path.of("benchmark_results.json");
            Files.writeString(path, json);
            dev.nozh.NozhConstants.LOGGER.info("Results written to " + path.toAbsolutePath());
        } catch (IOException e) {
            dev.nozh.NozhConstants.LOGGER.error("Failed to write benchmark results", e);
        }
    }

    // Helper class for stats if needed, using standard stats above
}
