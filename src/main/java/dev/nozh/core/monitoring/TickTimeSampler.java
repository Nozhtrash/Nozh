package dev.nozh.core.monitoring;

import dev.nozh.NozhConstants;
import dev.nozh.api.PerfSnapshot;
import dev.nozh.core.profiler.RollingWindowStats;

/**
 * Measures tick time (delta between ticks) using System.nanoTime().
 * Should be called once per tick from the main tick loop.
 */
public final class TickTimeSampler {

    private static final long MAX_VALID_TICK_NANOS = 1_000_000_000L; // 1s

    private final RollingWindowStats stats;
    private long lastTickNanos = 0;
    private boolean initialized = false;

    public TickTimeSampler() {
        this(5);
    }

    public TickTimeSampler(int windowSeconds) {
        int capacity = Math.max(40, Math.min(20 * windowSeconds, 600));
        this.stats = new RollingWindowStats(capacity, windowSeconds);
    }

    /**
     * Called once per tick from tick loop.
     * Captures delta and pushes to stats.
     */
    public void onTick() {
        try {
            long now = System.nanoTime();

            if (!initialized) {
                lastTickNanos = now;
                initialized = true;
                return;
            }

            long deltaNanos = now - lastTickNanos;
            lastTickNanos = now;

            if (deltaNanos <= 0 || deltaNanos > MAX_VALID_TICK_NANOS) {
                return;
            }

            stats.addSample(deltaNanos);
        } catch (Exception e) {
            NozhConstants.LOGGER.debug("Error in TickTimeSampler: {}", e.getMessage());
        }
    }

    public PerfSnapshot getSnapshot() {
        return stats.snapshot();
    }

    public void reset() {
        lastTickNanos = 0;
        initialized = false;
        stats.reset();
    }
}
