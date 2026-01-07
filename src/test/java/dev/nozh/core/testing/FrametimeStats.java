package dev.nozh.core.testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FrametimeStats {
    private FrametimeStats() {
    }

    public static StatsResult compute(List<Double> samples) {
        if (samples.isEmpty()) {
            return new StatsResult(Double.NaN, Double.NaN, Double.NaN, 0, 0, false);
        }
        List<Double> sorted = new ArrayList<>(samples);
        Collections.sort(sorted);
        double avg = sorted.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
        double p95 = percentile(sorted, 0.95);
        boolean p99Valid = sorted.size() >= 2000;
        double p99 = p99Valid ? percentile(sorted, 0.99) : Double.NaN;
        long spikes = sorted.stream().filter(value -> value > 500).count();
        return new StatsResult(avg, p95, p99, spikes, sorted.size(), p99Valid);
    }

    private static double percentile(List<Double> sorted, double percentile) {
        if (sorted.isEmpty()) {
            return Double.NaN;
        }
        double rank = percentile * sorted.size();
        int index = (int) Math.ceil(rank) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        return sorted.get(index);
    }

    public record StatsResult(
        double averageMs,
        double p95Ms,
        double p99Ms,
        long spikeCount,
        int sampleCount,
        boolean p99Valid
    ) {
    }
}
