package dev.nozh.core.profiler;

import dev.nozh.NozhConstants;

/**
 * Measures frame time (delta between frames) using System.nanoTime().
 * Should be called once per frame from the render loop.
 * 
 * Phase 3: Measurement Only
 * - No FPS calculation
 * - No statistics logic (delegated to RollingWindowStats)
 * - Safe/Defensive
 */
public class FrameTimeSampler {

    private final RollingWindowStats stats;
    private long lastFrameNanos = 0;
    private boolean initialized = false;

    // Safety thresholds
    private static final long MAX_VALID_FRAME_NANOS = 500_000_000L; // 500ms

    public FrameTimeSampler(RollingWindowStats stats) {
        this.stats = stats;
    }

    /**
     * Called once per frame into render loop.
     * Captures delta and pushes to stats.
     */

    public void onFrame() {
        try {
            long now = System.nanoTime();

            if (!initialized) {
                lastFrameNanos = now;
                initialized = true;
                return;
            }

            long deltaNanos = now - lastFrameNanos;
            lastFrameNanos = now;

            // Filter invalid samples
            if (deltaNanos <= 0 || deltaNanos > MAX_VALID_FRAME_NANOS) {
                // Ignore spikes/pauses/time rewinds
                return;
            }

            stats.addSample(deltaNanos);

        } catch (Exception e) {
            // NEVER crash the game from profiler
            NozhConstants.LOGGER.debug("Error in FrameTimeSampler: {}", e.getMessage());
        }
    }

    /**
     * Reset sampler state
     */
    public synchronized void reset() {
        lastFrameNanos = 0;
        initialized = false;
        stats.reset();
    }
}
