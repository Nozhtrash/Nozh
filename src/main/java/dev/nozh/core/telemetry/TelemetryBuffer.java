package dev.nozh.core.telemetry;

/**
 * Telemetry buffer interface (Contract 4).
 * 
 * Rule 4.7: NEVER blocks, droppable samples, fixed capacity.
 * 
 * Implementation must use ring buffer with zero allocations per add().
 */
public interface TelemetryBuffer {

    /**
     * Add sample to buffer.
     * 
     * NEVER blocks. If buffer is full, drops sample and increments counter.
     * NEVER throws exception.
     * 
     * @param sample Sample to add (must not be null)
     */
    void add(TelemetrySample sample);

    /**
     * Get cheap snapshot of current telemetry state.
     * 
     * Creates new snapshot object but uses fixed calculation.
     * Thread-safe.
     * 
     * @return Current telemetry snapshot
     */
    TelemetrySnapshot snapshot();

    /**
     * Get total samples dropped due to buffer overflow.
     * 
     * @return Cumulative drop count
     */
    int getDroppedCount();

    /**
     * Clear all samples and reset counters.
     * 
     * Thread-safe.
     */
    void clear();
}
