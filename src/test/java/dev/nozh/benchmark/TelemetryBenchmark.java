package dev.nozh.benchmark;

import dev.nozh.core.telemetry.RingTelemetryBuffer;
import dev.nozh.core.telemetry.TelemetrySample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Isolated Benchmark for RingTelemetryBuffer (Phase 2).
 * 
 * Objectives:
 * 1. Measure snapshot() latency under contention (Reader vs Writer).
 * 2. Identify locking bottlenecks.
 * 
 * Run with: java dev.nozh.benchmark.TelemetryBenchmark
 */
public class TelemetryBenchmark {

    private static final int BUFFER_CAPACITY = 512;
    private static final int RUN_TIME_MS = 5000;
    private static final int WARMUP_MS = 2000;

    // Simulate render thread (Writer) - High frequency
    private static final long WRITE_INTERVAL_NS = 16_000_000 / 4; // ~4 samples per frame (excessive load test)

    public static void main(String[] args) throws InterruptedException {
        dev.nozh.NozhConstants.LOGGER.info("{\"event\": \"benchmark_start\", \"component\": \"RingTelemetryBuffer\"}");

        RingTelemetryBuffer buffer = new RingTelemetryBuffer(BUFFER_CAPACITY);
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicLong writerSamples = new AtomicLong(0);

        // 1. Writer Thread (Simulates Render Thread)
        Thread writer = new Thread(() -> {
            long lastAppend = System.nanoTime();
            while (running.get()) {
                long now = System.nanoTime();
                if (now - lastAppend >= WRITE_INTERVAL_NS) {
                    // Random data to prevent JVM optimization
                    double ft = 20.0 + Math.random() * 10.0;
                    buffer.add(new TelemetrySample(System.currentTimeMillis(), ft, 5.0, 100, 10, 0, 1000, 0, 0, 0, 0));
                    writerSamples.incrementAndGet();
                    lastAppend = now;
                }
                Thread.yield(); // Spin-wait-ish
            }
        });

        // 2. Reader Thread (Simulates Governor/HUD) -> The one we measure
        List<Long> snapshotLatencies = new ArrayList<>(10000);

        Thread reader = new Thread(() -> {
            while (running.get()) {
                long start = System.nanoTime();
                buffer.snapshot();
                long duration = System.nanoTime() - start;

                // Only record after warmup
                snapshotLatencies.add(duration);

                try {
                    Thread.sleep(16); // ~60 FPS poll rate
                } catch (InterruptedException e) {
                    break;
                }
            }
        });

        // Execution
        writer.start();

        dev.nozh.NozhConstants.LOGGER.info("{\"event\": \"warmup\", \"duration_ms\": " + WARMUP_MS + "}");
        Thread.sleep(WARMUP_MS);
        snapshotLatencies.clear(); // Clear warmup data
        writerSamples.set(0);

        dev.nozh.NozhConstants.LOGGER.info("{\"event\": \"measurement\", \"duration_ms\": " + RUN_TIME_MS + "}");
        reader.start();
        Thread.sleep(RUN_TIME_MS);

        running.set(false);
        writer.join();
        reader.join();

        // Analysis
        analyzeResults(snapshotLatencies, writerSamples.get());
    }

    private static void analyzeResults(List<Long> latencies, long totalWrites) {
        if (latencies.isEmpty()) {
            dev.nozh.NozhConstants.LOGGER.error("{\"error\": \"No samples collected\"}");
            return;
        }

        Collections.sort(latencies);

        double avgNs = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long p50Ns = latencies.get((int) (latencies.size() * 0.50));
        long p95Ns = latencies.get((int) (latencies.size() * 0.95));
        long p99Ns = latencies.get((int) (latencies.size() * 0.99));
        long maxNs = latencies.get(latencies.size() - 1);

        double avgMs = avgNs / 1_000_000.0;
        double p99Ms = p99Ns / 1_000_000.0;
        double maxMs = maxNs / 1_000_000.0;

        dev.nozh.NozhConstants.LOGGER.info(String.format(
                "{\"metric\": \"latency\", \"avg_ms\": %.4f, \"p95_ms\": %.4f, \"p99_ms\": %.4f, \"max_ms\": %.4f, \"writes_per_sec\": %.2f}",
                avgMs,
                p95Ns / 1_000_000.0,
                p99Ms,
                maxMs,
                totalWrites / (RUN_TIME_MS / 1000.0)));

        // Interpretation against budget
        // Budget: 0.1ms avg, 0.5ms P99
        boolean contentionDetected = avgMs > 0.1 || p99Ms > 0.5;

        dev.nozh.NozhConstants.LOGGER.info("INTERPRETATION:");
        dev.nozh.NozhConstants.LOGGER.info("Avg Latency: " + String.format("%.4f", avgMs) + " ms (Target: <0.1ms)");
        dev.nozh.NozhConstants.LOGGER.info("P99 Latency: " + String.format("%.4f", p99Ms) + " ms (Target: <0.5ms)");
        dev.nozh.NozhConstants.LOGGER.info("Contention Risk: " + (contentionDetected ? "HIGH" : "LOW"));
    }
}
