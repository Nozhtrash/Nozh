package dev.nozh.core.telemetry;

/**
 * Telemetry sample (Contract 4).
 * 
 * PURE record - primitives only, no Optional, no nested objects.
 * Sentinel values: -1 for unavailable/unknown metrics, NaN for invalid
 * calculations.
 * 
 * Rule 4.6: Absolute simplicity for zero-allocation sampling.
 * 
 * AUDIT FIX #19: Added robust input validation in compact constructor.
 */
public record TelemetrySample(
        long timestampMillis,
        double frametimeMs, // -1 if unavailable
        double tickMs, // -1 if unavailable
        int fps, // -1 if unavailable
        int entities, // -1 if unavailable
        int chunks, // -1 if unavailable
        int drawCalls, // -1 if unavailable
        int droppedSamples // Cumulative count of dropped samples
) {
    // Maximum age for timestamp validation (24 hours for test flexibility)
    private static final long MAX_TIMESTAMP_AGE_MS = 24L * 60L * 60L * 1000L;
    private static final long MAX_TIMESTAMP_FUTURE_MS = 1000L; // 1 second future tolerance
    
    // Compact constructor with validation (AUDIT FIX #19)
    public TelemetrySample {
        // Validate timestamp (must be reasonable - not in future, not too old)
        long now = System.currentTimeMillis();
        if (timestampMillis > now + MAX_TIMESTAMP_FUTURE_MS || 
            timestampMillis < now - MAX_TIMESTAMP_AGE_MS) {
            throw new IllegalArgumentException(
                String.format("Invalid timestamp: %d (now: %d, age: %dms)",
                    timestampMillis, now, now - timestampMillis)
            );
        }
        
        // Validate frametimeMs (allow -1 sentinel, or valid positive value)
        if (frametimeMs != -1 && (!Double.isFinite(frametimeMs) || frametimeMs < 0 || frametimeMs > 10000)) {
            throw new IllegalArgumentException(
                "Invalid frametime: " + frametimeMs + "ms (must be -1 or 0-10000)"
            );
        }
        
        // Validate tickMs (allow -1 sentinel, or valid positive value)
        if (tickMs != -1 && (!Double.isFinite(tickMs) || tickMs < 0 || tickMs > 1000)) {
            throw new IllegalArgumentException(
                "Invalid tick time: " + tickMs + "ms (must be -1 or 0-1000)"
            );
        }
        
        // Validate fps (-1 sentinel or 0-1000 range)
        if (fps != -1 && (fps < 0 || fps > 1000)) {
            throw new IllegalArgumentException(
                "Invalid FPS: " + fps + " (must be -1 or 0-1000)"
            );
        }
        
        // Validate counts (allow -1 sentinel, or non-negative values)
        if (entities != -1 && entities < 0) {
            throw new IllegalArgumentException("Entities cannot be negative: " + entities);
        }
        if (chunks != -1 && chunks < 0) {
            throw new IllegalArgumentException("Chunks cannot be negative: " + chunks);
        }
        if (drawCalls != -1 && drawCalls < 0) {
            throw new IllegalArgumentException("Draw calls cannot be negative: " + drawCalls);
        }
        if (droppedSamples < 0) {
            throw new IllegalArgumentException("Dropped samples cannot be negative: " + droppedSamples);
        }
    }

    /**
     * Sentinel sample for unavailable data.
     */
    public static final TelemetrySample UNAVAILABLE = new TelemetrySample(
            System.currentTimeMillis(),
            -1, -1, -1, -1, -1, -1, 0);

    /**
     * Factory method for creating test samples with current timestamp.
     * Use this in tests instead of manual timestamp values.
     * 
     * @param frametimeMs frametime in milliseconds
     * @param tickMs tick time in milliseconds
     * @param fps frames per second
     * @param entities entity count
     * @param chunks chunk count
     * @param drawCalls draw call count
     * @param droppedSamples dropped sample count
     * @return valid TelemetrySample for testing
     */
    public static TelemetrySample forTesting(
            double frametimeMs,
            double tickMs,
            int fps,
            int entities,
            int chunks,
            int drawCalls,
            int droppedSamples
    ) {
        return new TelemetrySample(
            System.currentTimeMillis(),
            frametimeMs,
            tickMs,
            fps,
            entities,
            chunks,
            drawCalls,
            droppedSamples
        );
    }

    /**
     * Simplified factory for common test cases.
     */
    public static TelemetrySample forTesting(double frametimeMs) {
        return forTesting(frametimeMs, 16.0, 60, 100, 50, 1000, 0);
    }

    /**
     * Check if this sample has valid frametime data.
     */
    public boolean hasFrametimeData() {
        return frametimeMs >= 0;
    }

    /**
     * Check if this sample has valid tick data.
     */
    public boolean hasTickData() {
        return tickMs >= 0;
    }
}
