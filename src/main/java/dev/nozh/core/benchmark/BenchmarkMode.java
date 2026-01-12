package dev.nozh.core.benchmark;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Built-in benchmark mode for testing optimizations.
 * Runs standardized tests and reports results.
 * 
 * INTEGRATION: Testing and validation
 * CONTRACT: Thread-safe, accurate timing
 */
public final class BenchmarkMode {

    /**
     * Benchmark type/duration.
     */
    public enum BenchmarkType {
        QUICK,          // 30 seconds, basic metrics
        STANDARD,       // 2 minutes, full metrics
        COMPREHENSIVE,  // 5 minutes, stress test
        CUSTOM          // User-defined duration
    }

    /**
     * Benchmark result with detailed metrics.
     */
    public record BenchmarkResult(
        BenchmarkType type,
        long durationMs,
        double avgFps,
        double p95Frametime,
        double p99Frametime,
        int spikeCount,
        double minFps,
        double maxFps,
        Map<String, Double> scenarioBreakdown
    ) {
        public BenchmarkResult {
            scenarioBreakdown = Map.copyOf(scenarioBreakdown);
        }

        public String summary() {
            return String.format("Benchmark: %s | Duration: %.1fs | Avg FPS: %.1f | P95: %.2fms | Spikes: %d",
                type, durationMs / 1000.0, avgFps, p95Frametime, spikeCount);
        }
    }

    private volatile boolean running = false;
    private volatile BenchmarkType currentType = null;
    private volatile long benchmarkStartTime = 0;
    private volatile long customDurationMs = 0;
    
    private final List<Double> fpsReadings = new CopyOnWriteArrayList<>();
    private final List<Double> frametimeReadings = new CopyOnWriteArrayList<>();
    private final Map<String, List<Double>> scenarioMetrics = new HashMap<>();
    private volatile int spikeCount = 0;

    /**
     * Start a benchmark.
     */
    public void startBenchmark(BenchmarkType type) {
        startBenchmark(type, 0);
    }

    /**
     * Start a benchmark with custom duration (for CUSTOM type).
     */
    public void startBenchmark(BenchmarkType type, long customDurationMs) {
        if (running) {
            throw new IllegalStateException("Benchmark already running");
        }

        this.currentType = type;
        this.customDurationMs = customDurationMs;
        this.benchmarkStartTime = System.currentTimeMillis();
        this.running = true;
        
        // Clear previous data
        fpsReadings.clear();
        frametimeReadings.clear();
        scenarioMetrics.clear();
        spikeCount = 0;
    }

    /**
     * Stop the benchmark.
     */
    public void stopBenchmark() {
        running = false;
    }

    /**
     * Record a frame sample.
     */
    public void recordFrame(double fps, double frametimeMs) {
        if (!running) {
            return;
        }

        // Check if benchmark should auto-stop
        if (shouldStop()) {
            stopBenchmark();
            return;
        }

        fpsReadings.add(fps);
        frametimeReadings.add(frametimeMs);

        // Detect spikes (frametime > 50ms = spike)
        if (frametimeMs > 50.0) {
            spikeCount++;
        }
    }

    /**
     * Record scenario-specific metrics.
     */
    public void recordScenarioMetric(String scenario, double value) {
        if (!running) {
            return;
        }

        scenarioMetrics.computeIfAbsent(scenario, k -> new ArrayList<>()).add(value);
    }

    /**
     * Check if benchmark is running.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Get current benchmark type.
     */
    public BenchmarkType getCurrentType() {
        return currentType;
    }

    /**
     * Get elapsed time in milliseconds.
     */
    public long getElapsedMs() {
        if (!running) {
            return 0;
        }
        return System.currentTimeMillis() - benchmarkStartTime;
    }

    /**
     * Get benchmark result.
     * Can be called while running for interim results, or after stopped for final.
     */
    public BenchmarkResult getResult() {
        if (fpsReadings.isEmpty()) {
            return createEmptyResult();
        }

        long durationMs = running ? getElapsedMs() : 
            (currentType != null ? getDurationForType(currentType) : customDurationMs);

        List<Double> sortedFps = new ArrayList<>(fpsReadings);
        Collections.sort(sortedFps);
        
        List<Double> sortedFrametimes = new ArrayList<>(frametimeReadings);
        Collections.sort(sortedFrametimes);

        double avgFps = sortedFps.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double minFps = sortedFps.get(0);
        double maxFps = sortedFps.get(sortedFps.size() - 1);
        
        double p95Frametime = calculatePercentile(sortedFrametimes, 95);
        double p99Frametime = calculatePercentile(sortedFrametimes, 99);

        // Calculate scenario breakdown
        Map<String, Double> breakdown = new HashMap<>();
        for (Map.Entry<String, List<Double>> entry : scenarioMetrics.entrySet()) {
            double avg = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            breakdown.put(entry.getKey(), avg);
        }

        return new BenchmarkResult(
            currentType != null ? currentType : BenchmarkType.CUSTOM,
            durationMs,
            avgFps,
            p95Frametime,
            p99Frametime,
            spikeCount,
            minFps,
            maxFps,
            breakdown
        );
    }

    /**
     * Compare two benchmark results.
     */
    public String compareBenchmarks(BenchmarkResult before, BenchmarkResult after) {
        StringBuilder comparison = new StringBuilder();
        comparison.append("=== Benchmark Comparison ===\n");
        
        double fpsDiff = after.avgFps() - before.avgFps();
        double fpsPercent = (fpsDiff / before.avgFps()) * 100;
        comparison.append(String.format("Average FPS: %.1f → %.1f (%+.1f, %+.1f%%)\n",
            before.avgFps(), after.avgFps(), fpsDiff, fpsPercent));

        double p95Diff = after.p95Frametime() - before.p95Frametime();
        comparison.append(String.format("P95 Frametime: %.2fms → %.2fms (%+.2fms)\n",
            before.p95Frametime(), after.p95Frametime(), p95Diff));

        int spikeDiff = after.spikeCount() - before.spikeCount();
        comparison.append(String.format("Spike Count: %d → %d (%+d)\n",
            before.spikeCount(), after.spikeCount(), spikeDiff));

        if (fpsDiff > 0) {
            comparison.append("\n✓ Performance IMPROVED\n");
        } else if (fpsDiff < 0) {
            comparison.append("\n✗ Performance DEGRADED\n");
        } else {
            comparison.append("\n= Performance UNCHANGED\n");
        }

        return comparison.toString();
    }

    /**
     * Get progress percentage.
     */
    public double getProgress() {
        if (!running) {
            return 100.0;
        }

        long targetDuration = currentType != null ? getDurationForType(currentType) : customDurationMs;
        if (targetDuration == 0) {
            return 0.0;
        }

        long elapsed = getElapsedMs();
        return Math.min(100.0, (elapsed * 100.0) / targetDuration);
    }

    private boolean shouldStop() {
        if (currentType == null && customDurationMs == 0) {
            return false; // Infinite benchmark
        }

        long targetDuration = currentType != null ? getDurationForType(currentType) : customDurationMs;
        return getElapsedMs() >= targetDuration;
    }

    private long getDurationForType(BenchmarkType type) {
        return switch (type) {
            case QUICK -> 30_000;        // 30 seconds
            case STANDARD -> 120_000;    // 2 minutes
            case COMPREHENSIVE -> 300_000; // 5 minutes
            case CUSTOM -> customDurationMs;
        };
    }

    private double calculatePercentile(List<Double> sorted, int percentile) {
        if (sorted.isEmpty()) {
            return 0.0;
        }
        
        int index = (int) Math.ceil((percentile / 100.0) * sorted.size()) - 1;
        index = Math.max(0, Math.min(sorted.size() - 1, index));
        return sorted.get(index);
    }

    private BenchmarkResult createEmptyResult() {
        return new BenchmarkResult(
            currentType != null ? currentType : BenchmarkType.CUSTOM,
            0,
            0.0,
            0.0,
            0.0,
            0,
            0.0,
            0.0,
            Map.of()
        );
    }
}
