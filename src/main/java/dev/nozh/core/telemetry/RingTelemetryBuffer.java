package dev.nozh.core.telemetry;

import java.util.Arrays;

/**
 * Ring buffer telemetry implementation (Contract 4).
 * 
 * Rule 4.7 compliant:
 * - Fixed capacity (no dynamic allocation after construction)
 * - NEVER blocks
 * - Drops samples when full
 * - Thread-safe via volatile + careful ordering
 * 
 * Performance characteristics:
 * - add(): O(1), zero allocations
 * - snapshot(): O(n) where n = capacity, but cheap
 */
public final class RingTelemetryBuffer implements TelemetryBuffer {

    private final TelemetrySample[] buffer;
    private final int capacity;
    private final double[] frametimeScratch;

    private volatile int startIndex = 0;
    private volatile int size = 0;
    private volatile int droppedCount = 0;

    private static final double SPIKE_THRESHOLD_MS = 50.0; // Configurable

    public RingTelemetryBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.buffer = new TelemetrySample[capacity];
        this.frametimeScratch = new double[capacity];
    }

    /**
     * Default constructor with 512 sample capacity.
     */
    public RingTelemetryBuffer() {
        this(512);
    }

    @Override
    public void add(TelemetrySample sample) {
        if (sample == null) {
            return; // Fail silently per Contract 4
        }

        try {
            synchronized (buffer) {
                if (size < capacity) {
                    int index = (startIndex + size) % capacity;
                    buffer[index] = sample;
                    size++;
                } else {
                    buffer[startIndex] = sample;
                    startIndex = (startIndex + 1) % capacity;
                    droppedCount++;
                }
            }
        } catch (Exception e) {
            // NEVER throw - Contract 4.7
            droppedCount++;
        }
    }

    @Override
    public TelemetrySnapshot snapshot() {
        int currentSize;
        int currentDropped;
        double sumFrametime = 0;
        double sumSquares = 0;
        int frametimeSamples = 0;
        int spikes = 0;
        int frametimeCount = 0;

        for (int i = 0; i < currentSize; i++) {
            TelemetrySample s = copy[i];
            if (s != null && s.hasFrametimeData()) {
                double ft = s.frametimeMs();
                frametimes[frametimeCount++] = ft;
                sumFrametime += ft;
                sumSquares += ft * ft;
                frametimeSamples++;

            for (int i = 0; i < currentSize; i++) {
                int index = (startIndex + i) % capacity;
                TelemetrySample s = buffer[index];
                if (s != null && s.hasFrametimeData()) {
                    double ft = s.frametimeMs();
                    frametimeScratch[frametimeCount++] = ft;
                    sumFrametime += ft;
                    frametimeSamples++;

                    if (ft > SPIKE_THRESHOLD_MS) {
                        spikes++;
                    }
                }
            }

        if (frametimeSamples == 0) {
            return TelemetrySnapshot.EMPTY;
        }

        double avg = sumFrametime / frametimeSamples;
        double variance = (sumSquares / frametimeSamples) - (avg * avg);
        double stddev = Math.sqrt(Math.max(0.0, variance));

        // Calculate P95
        double p95 = calculateP95(frametimes, frametimeCount);

        return TelemetrySnapshot.of(avg, p95, stddev, spikes, currentSize, currentDropped);
    }

    @Override
    public int getDroppedCount() {
        return droppedCount;
    }

    @Override
    public void clear() {
        synchronized (buffer) {
            Arrays.fill(buffer, null);
            startIndex = 0;
            size = 0;
            droppedCount = 0;
        }
    }

    /**
     * Calculate P95 from sorted array segment.
     */
    private double calculateP95(double[] values, int count) {
        if (count == 0) {
            return 0;
        }

        // Sort only the valid portion
        Arrays.sort(values, 0, count);

        int p95Index = (int) Math.ceil(count * 0.95) - 1;
        p95Index = Math.max(0, Math.min(p95Index, count - 1));

        return values[p95Index];
    }
}
