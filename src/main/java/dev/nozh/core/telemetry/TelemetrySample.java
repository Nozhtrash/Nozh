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
 * SECURITY: Full input validation added to prevent data corruption
 * and NaN propagation through the telemetry pipeline.
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
    
    // ===== VALIDATION CONSTANTS =====
    private static final long MAX_TIMESTAMP_FUTURE_MS = 1000L; // 1 second tolerance
    private static final long MAX_TIMESTAMP_PAST_MS = 3600000L; // 1 hour max age
    private static final double MAX_FRAMETIME_MS = 10000.0; // 10 seconds max
    private static final double MAX_TICK_MS = 1000.0; // 1 second max
    private static final int MAX_FPS = 1000; // Maximum reasonable FPS
    
    /**
     * Compact constructor with comprehensive validation.
     * 
     * Validates all inputs to prevent:
     * - NaN/Infinity propagation
     * - Timestamp anomalies
     * - Negative counts
     * - Out-of-range metrics
     */
    public TelemetrySample {
        // Validate timestamp (must be reasonable - not in future, not too old)
        long now = System.currentTimeMillis();
        if (timestampMillis > now + MAX_TIMESTAMP_FUTURE_MS) {
            throw new IllegalArgumentException(
                String.format("Timestamp is in the future: %d (now: %d, delta: +%dms)",
                    timestampMillis, now, timestampMillis - now)
            );
        }
        if (timestampMillis < now - MAX_TIMESTAMP_PAST_MS) {
            throw new IllegalArgumentException(
                String.format("Timestamp is too old: %d (now: %d, age: %dms)",
                    timestampMillis, now, now - timestampMillis)
            );
        }
        
        // Validate frametime (if not sentinel -1)
        if (frametimeMs != -1) {
            if (!Double.isFinite(frametimeMs)) {
                throw new IllegalArgumentException(
                    "Frametime must be finite, got: " + frametimeMs
                );
            }
            if (frametimeMs < 0) {
                throw new IllegalArgumentException(
                    "Frametime cannot be negative (use -1 for unavailable): " + frametimeMs
                );
            }
            if (frametimeMs > MAX_FRAMETIME_MS) {
                throw new IllegalArgumentException(
                    String.format("Frametime exceeds maximum %.0fms: %.2fms",
                        MAX_FRAMETIME_MS, frametimeMs)
                );
            }
        }
        
        // Validate tick time (if not sentinel -1)
        if (tickMs != -1) {
            if (!Double.isFinite(tickMs)) {
                throw new IllegalArgumentException(
                    "Tick time must be finite, got: " + tickMs
                );
            }
            if (tickMs < 0) {
                throw new IllegalArgumentException(
                    "Tick time cannot be negative (use -1 for unavailable): " + tickMs
                );
            }
            if (tickMs > MAX_TICK_MS) {
                throw new IllegalArgumentException(
                    String.format("Tick time exceeds maximum %.0fms: %.2fms",
                        MAX_TICK_MS, tickMs)
                );
            }
        }
        
        // Validate FPS (if not sentinel -1)
        if (fps != -1) {
            if (fps < 0) {
                throw new IllegalArgumentException(
                    "FPS cannot be negative (use -1 for unavailable): " + fps
                );
            }
            if (fps > MAX_FPS) {
                throw new IllegalArgumentException(
                    String.format("FPS exceeds maximum %d: %d", MAX_FPS, fps)
                );
            }
        }
        
        // Validate entity counts (must be non-negative or -1)
        if (entities != -1 && entities < 0) {
            throw new IllegalArgumentException(
                "Entities count cannot be negative (use -1 for unavailable): " + entities
            );
        }
        
        if (chunks != -1 && chunks < 0) {
            throw new IllegalArgumentException(
                "Chunks count cannot be negative (use -1 for unavailable): " + chunks
            );
        }
        
        if (drawCalls != -1 && drawCalls < 0) {
            throw new IllegalArgumentException(
                "Draw calls count cannot be negative (use -1 for unavailable): " + drawCalls
            );
        }
        
        // Validate dropped samples (must always be non-negative)
        if (droppedSamples < 0) {
            throw new IllegalArgumentException(
                "Dropped samples count cannot be negative: " + droppedSamples
            );
        }
    }
    
    /**
     * Sentinel sample for unavailable data.
     */
    public static TelemetrySample UNAVAILABLE = new TelemetrySample(
            System.currentTimeMillis(),
            -1, -1, -1, -1, -1, -1, 0);

    /**
     * Check if this sample has valid frametime data.
     */
    public boolean hasFrametimeData() {
        return frametimeMs >= 0 && Double.isFinite(frametimeMs);
    }

    /**
     * Check if this sample has valid tick data.
     */
    public boolean hasTickData() {
        return tickMs >= 0 && Double.isFinite(tickMs);
    }
}
