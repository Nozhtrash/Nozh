package dev.nozh.core.telemetry;

import dev.nozh.api.Scenario;
import dev.nozh.api.PerfSnapshot;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aggregates telemetry data by scenario for advanced analytics.
 * Tracks performance patterns across different game contexts.
 */
public final class TelemetryAggregator {
    private final Map<Scenario, ScenarioMetrics> metricsByScenario = new ConcurrentHashMap<>();
    private final List<PerfSnapshot> recentSnapshots = new ArrayList<>();
    private static final int MAX_RECENT_SNAPSHOTS = 1000;
    
    private long totalSamples = 0;
    private long totalSpikes = 0;
    private double cumulativeFrametime = 0.0;
    
    public void recordSnapshot(PerfSnapshot snapshot, Scenario scenario) {
        synchronized (recentSnapshots) {
            recentSnapshots.add(snapshot);
            if (recentSnapshots.size() > MAX_RECENT_SNAPSHOTS) {
                recentSnapshots.remove(0);
            }
        }
        
        totalSamples += snapshot.sampleCount();
        totalSpikes += snapshot.spikeCount();
        cumulativeFrametime += snapshot.avgFrametimeMs() * snapshot.sampleCount();
        
        metricsByScenario.computeIfAbsent(scenario, k -> new ScenarioMetrics())
            .record(snapshot);
    }
    
    public Map<Scenario, ScenarioMetrics> getMetricsByScenario() {
        return new HashMap<>(metricsByScenario);
    }
    
    public List<PerfSnapshot> getRecentSnapshots(int limit) {
        synchronized (recentSnapshots) {
            int size = recentSnapshots.size();
            int start = Math.max(0, size - limit);
            return new ArrayList<>(recentSnapshots.subList(start, size));
        }
    }
    
    public GlobalMetrics getGlobalMetrics() {
        double avgFrametime = totalSamples > 0 ? cumulativeFrametime / totalSamples : 0.0;
        return new GlobalMetrics(
            totalSamples,
            totalSpikes,
            avgFrametime,
            metricsByScenario.size()
        );
    }
    
    public void reset() {
        metricsByScenario.clear();
        synchronized (recentSnapshots) {
            recentSnapshots.clear();
        }
        totalSamples = 0;
        totalSpikes = 0;
        cumulativeFrametime = 0.0;
    }
    
    public static final class ScenarioMetrics {
        private long sampleCount = 0;
        private long spikeCount = 0;
        private double cumulativeP95 = 0.0;
        private double cumulativeP99 = 0.0;
        private double cumulativeAvg = 0.0;
        private double minFrametime = Double.MAX_VALUE;
        private double maxFrametime = Double.MIN_VALUE;
        private long firstSeen = System.currentTimeMillis();
        private long lastSeen = System.currentTimeMillis();
        
        void record(PerfSnapshot snapshot) {
            sampleCount += snapshot.sampleCount();
            spikeCount += snapshot.spikeCount();
            cumulativeP95 += snapshot.p95FrametimeMs();
            cumulativeP99 += snapshot.p99FrametimeMs();
            cumulativeAvg += snapshot.avgFrametimeMs();
            
            minFrametime = Math.min(minFrametime, snapshot.avgFrametimeMs());
            maxFrametime = Math.max(maxFrametime, snapshot.avgFrametimeMs());
            lastSeen = System.currentTimeMillis();
        }
        
        public long sampleCount() { return sampleCount; }
        public long spikeCount() { return spikeCount; }
        public double avgP95() { return sampleCount > 0 ? cumulativeP95 / sampleCount : 0.0; }
        public double avgP99() { return sampleCount > 0 ? cumulativeP99 / sampleCount : 0.0; }
        public double avgFrametime() { return sampleCount > 0 ? cumulativeAvg / sampleCount : 0.0; }
        public double minFrametime() { return minFrametime != Double.MAX_VALUE ? minFrametime : 0.0; }
        public double maxFrametime() { return maxFrametime != Double.MIN_VALUE ? maxFrametime : 0.0; }
        public long durationMs() { return lastSeen - firstSeen; }
        public long firstSeen() { return firstSeen; }
        public long lastSeen() { return lastSeen; }
    }
    
    public record GlobalMetrics(
        long totalSamples,
        long totalSpikes,
        double avgFrametime,
        int scenariosEncountered
    ) {}
}