package dev.nozh.core.cloud;

import com.google.gson.JsonObject;
import dev.nozh.NozhConstants;
import dev.nozh.core.math.ExponentialMovingAverage;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Hardware Benchmarker - Safely collects anonymous hardware stats and runs
 * benchmarks.
 * 
 * Purpose:
 * 1. Identify hardware capabilities (Potato vs High-End)
 * 2. Run synthetic benchmarks to score CPU performance
 * 3. Generate anonymous Hardware Fingerprint
 * 
 * PRIVACY NOTE: No personally identifiable information (PII) is collected.
 * Only hardware specs (CPU model, RAM amount, GPU model) are read.
 */
public final class HardwareBenchmarker {

    private static final HardwareBenchmarker INSTANCE = new HardwareBenchmarker();

    // Cached profile
    private JsonObject hardwareProfile = null;
    private double benchmarkScore = -1;

    private HardwareBenchmarker() {
    }

    public static HardwareBenchmarker getInstance() {
        return INSTANCE;
    }

    /**
     * Run a quick CPU benchmark to estimate single-core performance.
     * Non-blocking (runs async).
     */
    public CompletableFuture<Double> runBenchmarkAsync() {
        return CloudManager.getInstance().submitTask(() -> {
            NozhConstants.LOGGER.info("[NOZH] Starting hardware benchmark...");
            long start = System.nanoTime();

            // Synthetic load: floating point heavy (simulating game physics/rendering math)
            // Synthetic load: floating point heavy (simulating game physics/rendering math)
            @SuppressWarnings("unused")
            double result = 0;
            for (int i = 0; i < 5_000_000; i++) {
                result += Math.sqrt(i) * Math.sin(i);
            }

            long durationNs = System.nanoTime() - start;
            double durationMs = durationNs / 1_000_000.0;

            // Score = operations per ms (higher is better)
            // Normalized: 1000 = fast, 100 = slow
            double score = (5_000_000.0 / durationMs) / 100.0;

            benchmarkScore = score;
            NozhConstants.LOGGER.info("[NOZH] Benchmark complete. Score: {}", String.format("%.2f", score));
        }).thenApply(v -> benchmarkScore);
    }

    /**
     * Collect anonymous hardware profile.
     * Uses OSHI if available, or JVM fallbacks.
     */
    public JsonObject getHardwareProfile() {
        if (hardwareProfile != null) {
            return hardwareProfile;
        }

        JsonObject profile = new JsonObject();

        try {
            // Basic JVM info (always available)
            profile.addProperty("os", System.getProperty("os.name"));
            profile.addProperty("arch", System.getProperty("os.arch"));
            profile.addProperty("cores", Runtime.getRuntime().availableProcessors());
            profile.addProperty("max_memory_mb", Runtime.getRuntime().maxMemory() / (1024 * 1024));

            // Try OSHI for detailed info (needs library, might fail if not present)
            try {
                SystemInfo si = new SystemInfo();
                HardwareAbstractionLayer hal = si.getHardware();
                CentralProcessor cpu = hal.getProcessor();
                GlobalMemory mem = hal.getMemory();

                profile.addProperty("cpu_model", cpu.getProcessorIdentifier().getName());
                profile.addProperty("total_ram_gb", mem.getTotal() / (1024 * 1024 * 1024));

                List<GraphicsCard> gpus = hal.getGraphicsCards();
                if (!gpus.isEmpty()) {
                    profile.addProperty("gpu_model", gpus.get(0).getName());
                    profile.addProperty("gpu_vram_mb", gpus.get(0).getVRam() / (1024 * 1024));
                }
            } catch (NoClassDefFoundError | Exception e) {
                // OSHI not available or failed, stick to JVM info
                profile.addProperty("detailed_info", "unavailable");
            }

            // Add benchmark score if available
            if (benchmarkScore > 0) {
                profile.addProperty("benchmark_score", benchmarkScore);
            }

        } catch (Exception e) {
            NozhConstants.LOGGER.error("[NOZH] Failed to profile hardware", e);
            profile.addProperty("error", "profiling_failed");
        }

        hardwareProfile = profile;
        return profile;
    }

    /**
     * Categorize hardware based on specs/benchmark.
     */
    public String getHardwareTier() {
        if (benchmarkScore > 0) {
            if (benchmarkScore > 8.0)
                return "HIGH_END";
            if (benchmarkScore > 4.0)
                return "MID_RANGE";
            return "LOW_END";
        }

        // Fallback to core count
        int cores = Runtime.getRuntime().availableProcessors();
        if (cores >= 12)
            return "HIGH_END";
        if (cores >= 6)
            return "MID_RANGE";
        return "LOW_END";
    }
}
