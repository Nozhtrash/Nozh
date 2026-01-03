package dev.nozh.core.monitoring;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Real-time monitoring of chunk operations.
 * 
 * Fed directly by Mixins for 100% accuracy.
 */
public class NozhChunkMonitor {

    // Tracks active loading operations in this tick/second
    private static final AtomicInteger chunksLoaded = new AtomicInteger(0);
    private static final AtomicInteger chunksUnloaded = new AtomicInteger(0);

    /**
     * Called by Mixin when a chunk is loaded.
     */
    public static void onChunkLoad() {
        chunksLoaded.incrementAndGet();
    }

    /**
     * Called by Mixin when a chunk is unloaded.
     */
    public static void onChunkUnload() {
        chunksUnloaded.incrementAndGet();
    }

    /**
     * Get loads in the last sampling window.
     * Resets counters after reading.
     */
    public static int getAndResetLoadCount() {
        return chunksLoaded.getAndSet(0);
    }

    /**
     * Get current load count without resetting.
     */
    public static int getLoadCount() {
        return chunksLoaded.get();
    }
}
