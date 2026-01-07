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
    private static final int SPIKE_BURST_SAMPLES = 120;
    private static final int STABLE_SAMPLE_WINDOW = 180;
    private static final int BASE_SAMPLE_STRIDE = 4;

    private int sampleStride = 1;
    private int sampleCounter = 0;
    private int stableSampleCount = 0;
    private int spikeBurstRemaining = 0;
    private int overheadSampleCounter = 0;
    private volatile long averageAddNanos = 0;

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
        long startNanos = 0;
        boolean trackOverhead = (overheadSampleCounter++ & 0xF) == 0;
        if (trackOverhead) {
            startNanos = System.nanoTime();
        }

        if (sample == null) {
            recordOverheadIfNeeded(trackOverhead, startNanos);
            return; // Fail silently per Contract 4
        }

        boolean spike = sample.hasFrametimeData() && sample.frametimeMs() > SPIKE_THRESHOLD_MS;
        if (spike) {
            spikeBurstRemaining = SPIKE_BURST_SAMPLES;
        }

        if (spikeBurstRemaining > 0) {
            if (!spike) {
                spikeBurstRemaining--;
            }
            sampleStride = 1;
            stableSampleCount = 0;
        } else {
            stableSampleCount++;
            sampleStride = stableSampleCount >= STABLE_SAMPLE_WINDOW ? BASE_SAMPLE_STRIDE : 1;
        }

        if (!spike && spikeBurstRemaining == 0 && sampleStride > 1) {
            if ((sampleCounter++ % sampleStride) != 0) {
                recordOverheadIfNeeded(trackOverhead, startNanos);
                return;
            }
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
        } finally {
            recordOverheadIfNeeded(trackOverhead, startNanos);
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

        // Calculate aggregates
        double sumFrametime = 0;
        int frametimeSamples = 0;
        int spikes = 0;

        // Collect valid samples
        double[] frametimes = new double[currentSize];
        int frametimeCount = 0;

        for (int i = 0; i < currentSize; i++) {
            TelemetrySample s = copy[i];
            if (s != null && s.hasFrametimeData()) {
                double ft = s.frametimeMs();
                frametimes[frametimeCount++] = ft;
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

        // Calculate P95
        double p95 = calculateP95(frametimes, frametimeCount);

        return TelemetrySnapshot.of(avg, p95, spikes, currentSize, currentDropped);
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
     * Average sampled add() overhead in microseconds.
     */
    public double getAverageAddOverheadMicros() {
        return averageAddNanos / 1000.0;
    }

    /**
     * Check if average sampled add() overhead is within budget in milliseconds.
     */
    public boolean isOverheadWithinBudget(double maxMs) {
        return averageAddNanos <= (long) (maxMs * 1_000_000.0);
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

    private void recordOverheadIfNeeded(boolean trackOverhead, long startNanos) {
        if (!trackOverhead) {
            return;
        }
        long duration = System.nanoTime() - startNanos;
        if (duration <= 0) {
            return;
        }
        if (averageAddNanos == 0) {
            averageAddNanos = duration;
        } else {
            averageAddNanos = (averageAddNanos * 15 + duration) / 16;
        }
    }
}
