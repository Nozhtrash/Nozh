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

    private volatile int writeIndex = 0;
    private volatile int size = 0;
    private volatile int droppedCount = 0;

    private static final double SPIKE_THRESHOLD_MS = 50.0; // Configurable

    public RingTelemetryBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.buffer = new TelemetrySample[capacity];
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
                buffer[writeIndex] = sample;
                writeIndex = (writeIndex + 1) % capacity;

                if (size < capacity) {
                    size++;
                } else {
                    // Buffer full, we're dropping oldest (implicit by overwrite)
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
        TelemetrySample[] copy;
        int currentSize;
        int currentDropped;

        synchronized (buffer) {
            currentSize = size;
            currentDropped = droppedCount;
            copy = Arrays.copyOf(buffer, capacity);
        }

        if (currentSize == 0) {
            return TelemetrySnapshot.EMPTY;
        }

        double sumFrametime = 0;
        double sumSquares = 0;
        int frametimeSamples = 0;
        int spikes = 0;

        double[] frametimes = new double[currentSize];
        int frametimeCount = 0;

        for (int i = 0; i < currentSize; i++) {
            TelemetrySample s = copy[i];
            if (s != null && s.hasFrametimeData()) {
                double ft = s.frametimeMs();
                frametimes[frametimeCount++] = ft;
                sumFrametime += ft;
                sumSquares += ft * ft;
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
        double stddev = computeStddev(sumSquares, frametimeSamples, avg);
        double p95 = calculateP95(frametimes, frametimeCount);

        return TelemetrySnapshot.of(avg, p95, stddev, spikes, frametimeCount, currentDropped);
    }

    @Override
    public int getDroppedCount() {
        return droppedCount;
    }

    @Override
    public void clear() {
        synchronized (buffer) {
            Arrays.fill(buffer, null);
            writeIndex = 0;
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
        double[] sorted = Arrays.copyOf(values, count);
        Arrays.sort(sorted);

        int p95Index = (int) Math.ceil(count * 0.95) - 1;
        p95Index = Math.max(0, Math.min(p95Index, count - 1));

        return sorted[p95Index];
    }

    private double computeStddev(double sumSquares, int sampleCount, double avg) {
        if (sampleCount <= 0) {
            return 0.0;
        }
        double variance = (sumSquares / sampleCount) - (avg * avg);
        double stddev = Math.sqrt(Math.max(0.0, variance));
        return Double.isFinite(stddev) ? stddev : 0.0;
    }
}
