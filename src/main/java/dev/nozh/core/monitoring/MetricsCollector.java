package dev.nozh.core.monitoring;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import dev.nozh.core.telemetry.TelemetrySnapshot;

/**
 * Collects and aggregates performance metrics.
 * Extended to support action execution metrics.
 */
public class MetricsCollector {
    private final ConcurrentHashMap<String, MetricData> metrics = new ConcurrentHashMap<>();
    private final AtomicLong telemetryCount = new AtomicLong(0);
    private final AtomicInteger actionCount = new AtomicInteger(0);
    
    public void recordTelemetry(TelemetrySnapshot snapshot) {
        if (snapshot != null) {
            telemetryCount.incrementAndGet();
        }
    }
    
    public void recordAction(String actionId, boolean success, long durationMs) {
        actionCount.incrementAndGet();
        
        metrics.compute(actionId, (key, data) -> {
            if (data == null) {
                data = new MetricData();
            }
            data.count++;
            data.totalDuration += durationMs;
            if (success) {
                data.successCount++;
            }
            return data;
        });
    }
    
    public java.util.Map<String, Object> getSummary() {
        java.util.Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("telemetry_count", telemetryCount.get());
        summary.put("action_count", actionCount.get());
        summary.put("metrics", new java.util.HashMap<>(metrics));
        return summary;
    }
    
    private static class MetricData {
        int count = 0;
        int successCount = 0;
        long totalDuration = 0;
        
        @Override
        public String toString() {
            return String.format("count=%d, success=%d, avgDuration=%dms",
                    count, successCount, count > 0 ? totalDuration / count : 0);
        }
    }
}
