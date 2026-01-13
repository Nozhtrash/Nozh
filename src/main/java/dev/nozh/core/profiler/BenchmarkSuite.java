package dev.nozh.core.profiler;

import dev.nozh.NozhConstants;
import dev.nozh.core.cloud.HardwareBenchmarker;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Benchmark Suite - Standardized in-game performance testing.
 * 
 * Capabilities:
 * 1. Hardware Score (Synthetic CPU/GPU test)
 * 2. In-Game FPS Test (10 seconds measurement)
 * 3. Results reporting
 */
public final class BenchmarkSuite {

    private static final BenchmarkSuite INSTANCE = new BenchmarkSuite();
    private final AtomicBoolean isBenchmarking = new AtomicBoolean(false);

    private long benchmarkStartTime = 0;
    private long framesCounted = 0;
    private double minFps = Double.MAX_VALUE;
    private double maxFps = 0.0;

    private BenchmarkSuite() {
    }

    public static BenchmarkSuite getInstance() {
        return INSTANCE;
    }

    public void startBenchmark() {
        if (isBenchmarking.get()) {
            NozhConstants.LOGGER.warn("Benchmark already running!");
            return;
        }

        isBenchmarking.set(true);
        benchmarkStartTime = System.currentTimeMillis();
        framesCounted = 0;
        minFps = Double.MAX_VALUE;
        maxFps = 0.0;

        NozhConstants.LOGGER.info("==========================================");
        NozhConstants.LOGGER.info("   STARTING NOZH PERFORMANCE BENCHMARK    ");
        NozhConstants.LOGGER.info("   Duration: 10 seconds                   ");
        NozhConstants.LOGGER.info("   Please perform typical gameplay actions");
        NozhConstants.LOGGER.info("==========================================");
    }

    public void onFrame(double fps) {
        if (!isBenchmarking.get()) {
            return;
        }

        framesCounted++;
        minFps = Math.min(minFps, fps);
        maxFps = Math.max(maxFps, fps);

        long elapsed = System.currentTimeMillis() - benchmarkStartTime;
        if (elapsed >= 10_000) { // 10 seconds
            finishBenchmark(elapsed);
        }
    }

    private void finishBenchmark(long elapsedMs) {
        isBenchmarking.set(false);
        double avgFps = (double) framesCounted * 1000 / elapsedMs;

        NozhConstants.LOGGER.info("==========================================");
        NozhConstants.LOGGER.info("   BENCHMARK COMPLETE                     ");
        NozhConstants.LOGGER.info("   Avg FPS: {}", String.format("%.2f", avgFps));
        NozhConstants.LOGGER.info("   Min FPS: {}", String.format("%.2f", minFps));
        NozhConstants.LOGGER.info("   Max FPS: {}", String.format("%.2f", maxFps));
        NozhConstants.LOGGER.info("   1% Low:  (Available in Pro version)");
        NozhConstants.LOGGER.info("==========================================");

        // Trigger hardware benchmark too
        HardwareBenchmarker.getInstance().runBenchmarkAsync().thenAccept(score -> {
            NozhConstants.LOGGER.info("   Hardware Score: {} (CPU)", String.format("%.2f", score));
        });
    }

    public boolean isRunning() {
        return isBenchmarking.get();
    }
}
