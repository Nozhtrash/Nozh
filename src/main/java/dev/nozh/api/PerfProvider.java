package dev.nozh.api;

/**
 * Contract for performance data providers.
 * 
 * Implementations must be:
 * - Thread-safe
 * - Non-blocking (fast sampling)
 * - Immutable return values
 * 
 * Phase 2: Interface definition only
 * Phase 3: First implementation (FrameTimeSampler)
 */
public interface PerfProvider {

    /**
     * Capture current performance snapshot.
     * 
     * @return immutable snapshot, never null
     * @throws IllegalStateException if provider not initialized
     */
    PerfSnapshot snapshot();

    /**
     * Check if provider has enough data for valid snapshots.
     * 
     * @return true if snapshot() will return meaningful data
     */
    boolean hasValidData();
}
