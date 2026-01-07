package dev.nozh.core.monitoring;

import dev.nozh.core.telemetry.TelemetrySnapshot;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Collects and aggregates performance metrics.
 * 
 * Tracks:
 * - FPS statistics (min, max, avg, p95)
 * - Frame time distribution
 * - Action success rates
 * - System health over time
 * 
 * Provides analytics and insights.
 * 
 * TASK 12: Performance metrics - aggregation
 */
public final class MetricsCollector {

    private final Map<String, MetricAccumulator> metrics = new ConcurrentHashMap<>();
    private final AtomicLong totalSamples = new AtomicLong(0);
    private final AtomicLong totalActions = new AtomicLong(0);
    private final AtomicLong successfulActions = new AtomicLong(0);

    private volatile double minFps = Double.MAX_VALUE;
    private volatile double maxFps = 0.0;
    private volatile double sumFps = 0.0;

    /**
     * Record telemetry sample.
     */
    public void recordTelemetry(TelemetrySnapshot snapshot) {
        totalSamples.incrementAndGet();

        double fps = 1000.0 / snapshot.avgFrametimeMs();
        sumFps += fps;

        if (fps < minFps) minFps = fps;
        if (fps > maxFps) maxFps = fps;

        recordMetric("avg_frametime", snapshot.avgFrametimeMs());
        recordMetric("p95_frametime", snapshot.p95FrametimeMs());
        recordMetric("spikes", snapshot.spikeCount());
    }

    /**
     * Record action execution.
     */
    public void recordAction(String actionId, boolean success, double duration) {
        totalActions.incrementAndGet();
        if (success) {
            successfulActions.incrementAndGet();
        }

        recordMetric("action_duration_" + actionId, duration);
    }

    /**
     * Record custom metric.
     */
    public void recordMetric(String name, double value) {
        MetricAccumulator accumulator = metrics.computeIfAbsent(name, k -> new MetricAccumulator());
        accumulator.add(value);
    }

    /**
     * Get action success rate.
     */
    public double getActionSuccessRate() {
        long total = totalActions.get();
        return total == 0 ? 0.0 : (double) successfulActions.get() / total;
    }

    /**
     * Get average FPS.
     */
    public double getAverageFps() {
        long samples = totalSamples.get();
        return samples == 0 ? 0.0 : sumFps / samples;
    }

    /**
     * Get FPS range.
     */
    public FpsRange getFpsRange() {
        return new FpsRange(minFps, maxFps, getAverageFps());
    }

    /**
     * Get metric statistics.
     */
    public MetricStats getMetricStats(String name) {
        MetricAccumulator accumulator = metrics.get(name);
        if (accumulator == null) {
            return null;
        }
        return accumulator.getStats();
    }

    /**
     * Get all metrics summary.
     */
    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("total_samples", totalSamples.get());
        summary.put("total_actions", totalActions.get());
        summary.put("action_success_rate", getActionSuccessRate());
        summary.put("avg_fps", getAverageFps());
        summary.put("min_fps", minFps == Double.MAX_VALUE ? 0.0 : minFps);
        summary.put("max_fps", maxFps);
        return summary;
    }

    /**
     * Reset all metrics.
     */
    public void reset() {
        metrics.clear();
        totalSamples.set(0);
        totalActions.set(0);
        successfulActions.set(0);
        minFps = Double.MAX_VALUE;
        maxFps = 0.0;
        sumFps = 0.0;
    }

    /**
     * Metric accumulator for statistics.
     */
    private static class MetricAccumulator {
        private double sum = 0.0;
        private double min = Double.MAX_VALUE;
        private double max = Double.MIN_VALUE;
        private long count = 0;

        synchronized void add(double value) {
            sum += value;
            count++;
            if (value < min) min = value;
            if (value > max) max = value;
        }

        synchronized MetricStats getStats() {
            if (count == 0) {
                return new MetricStats(0, 0, 0, 0);
            }
            return new MetricStats(sum / count, min, max, count);
        }
    }

    public record FpsRange(double min, double max, double avg) {}

    public record MetricStats(double avg, double min, double max, long count) {}
}
