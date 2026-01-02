package dev.nozh.core.profiler;

import dev.nozh.api.PerfSnapshot;
import java.util.Arrays;

/**
 * Ring buffer for frame/tick time samples (Measurement Only).
 * 
 * Design:
 * - Fixed size ring buffer of LONGS (nanoseconds)
 * - Spike filtering: outliers >500ms are counted but don't contaminate avg/p95
 * - No cached stats - compute on demand
 * - Synchronized for thread safety
 */
public class RollingWindowStats {

    private static final long SPIKE_THRESHOLD_NANOS = 500_000_000L; // 500ms

    private final long[] buffer;
    private final int capacity;
    private final int windowSeconds;
    private int writeIndex = 0;
    private int count = 0;
    private int spikeCount = 0; // Tracks extreme outliers separately

    public RollingWindowStats(int capacity, int windowSeconds) {
        this.capacity = capacity;
        this.windowSeconds = windowSeconds;
        this.buffer = new long[capacity];
    }

    /**
     * Add a sample to the buffer.
     * Spikes (>500ms) are tracked separately.
     */
    public synchronized void addSample(long valueNanos) {
        buffer[writeIndex] = valueNanos;
        writeIndex = (writeIndex + 1) % capacity;
        if (count < capacity) {
            count++;
        }

        // Track spikes but don't let them dominate avg/p95
        if (valueNanos > SPIKE_THRESHOLD_NANOS) {
            spikeCount++;
        }
    }

    /**
     * Create a snapshot of current performance.
     * Synchronized to ensure consistent view.
     */
    public synchronized PerfSnapshot snapshot() {
        if (count < capacity / 2) {
            // Insufficient data
            return new PerfSnapshot(Double.NaN, Double.NaN, count, 0, windowSeconds, false, System.currentTimeMillis());
        }

        // Copy samples, FILTERING spikes for avg/p95 calculation
        // (but spikes are still tracked in spikeCount)
        int filteredCount = 0;
        long[] filtered = new long[count];

        for (int i = 0; i < count; i++) {
            long sample = buffer[i];
            if (sample <= SPIKE_THRESHOLD_NANOS) {
                filtered[filteredCount++] = sample;
            }
        }

        // Edge case: ALL samples are spikes (alt-tab, massive lag)
        if (filteredCount == 0) {
            // Return NaN to signal "data too noisy"
            return new PerfSnapshot(Double.NaN, Double.NaN, count, spikeCount, windowSeconds, false,
                    System.currentTimeMillis());
        }

        // Sort filtered samples for percentile
        long[] validSamples = Arrays.copyOf(filtered, filteredCount);
        Arrays.sort(validSamples);

        // Calculate average (filtered)
        double sum = 0;
        for (long s : validSamples) {
            sum += s;
        }
        double avgNanos = sum / filteredCount;

        // Calculate P95 (filtered)
        int p95Index = (int) Math.ceil(filteredCount * 0.95) - 1;
        p95Index = Math.max(0, Math.min(p95Index, filteredCount - 1));
        long p95Nanos = validSamples[p95Index];

        // Convert to ms
        return new PerfSnapshot(
                avgNanos / 1_000_000.0,
                p95Nanos / 1_000_000.0,
                count,
                spikeCount, // Spike count for diagnostics
                windowSeconds,
                true,
                System.currentTimeMillis());
    }

    /**
     * Get spike count for diagnostics.
     */
    public synchronized int getSpikeCount() {
        return spikeCount;
    }

    /**
     * Clear all samples and reset.
     */
    public synchronized void reset() {
        writeIndex = 0;
        count = 0;
        spikeCount = 0;
    }
}
