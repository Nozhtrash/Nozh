package dev.nozh.core.monitoring;

/**
 * Chunk load monitor to detect temporary FPS drops from world loading.
 * 
 * Intelligence: Prevents governor from acting on temporary performance dips
 * caused by chunk loading, which would be false positives.
 */
public final class ChunkLoadMonitor {

    private static final int HEAVY_LOAD_THRESHOLD = 20; // chunks/second
    private static final long TRACKING_WINDOW_MS = 1000; // 1 second

    private int chunksThisSecond = 0;
    private long lastResetTime = System.currentTimeMillis();

    /**
     * Notify monitor that a chunk was loaded.
     * 
     * Note: In production, this would hook into Minecraft's chunk events.
     * For now, it's a placeholder that can be called manually.
     */
    public void onChunkLoad() {
        // No-op: Data now comes from Mixins via NozhChunkMonitor
    }

    /**
     * Check if currently experiencing heavy chunk loading.
     * 
     * @return true if chunks loading faster than threshold
     */
    public boolean isHeavyChunkLoad() {
        return getChunkLoadRate() > HEAVY_LOAD_THRESHOLD;
    }

    /**
     * Get chunk load rate (chunks/second).
     * Retrieves real data from Mixins.
     */
    public int getChunkLoadRate() {
        // Sync with real monitor
        // Note: calling getAndResetLoadCount consumes the data, so we need to be
        // careful
        // if multiple things needed this. For now, only Governor checks this.
        // Better pattern: poll it once per tick in GovernorRunner, but to keep
        // architecture simple:
        // We will just read the current atomic value directly if we added a getter
        // without reset,
        // or accept that calling this resets the 'since last check' counter.

        // Since we want rate per second, and Governor calls this frequently,
        // we should probably just return the atomic value without resetting, and let
        // NozhChunkMonitor
        // handle the reset logic or use a rolling window.
        // For simplicity in this step: We assume getAndReset is called ~once per second
        // or we
        // just expose the raw AtomicInteger from NozhChunkMonitor without reset.

        // Let's modify NozhChunkMonitor to expose 'current second count' instead of
        // resetting on read.
        // But NozhChunkMonitor currently does atomic increment.
        // Let's rely on NozhChunkMonitor to be the source of truth.

        // If we want a simple non-destructive read:
        // NozhChunkMonitor doesn't expose a non-destructive read yet.
        // I'll make ChunkLoadMonitor a simple wrapper for now that consumes the data
        // effectively integrating the mixin stream.

        chunksThisSecond += dev.nozh.core.monitoring.NozhChunkMonitor.getAndResetLoadCount();

        long now = System.currentTimeMillis();
        if (now - lastResetTime > TRACKING_WINDOW_MS) {
            int rate = chunksThisSecond;
            chunksThisSecond = 0;
            lastResetTime = now;
            return rate; // Return the rate of the *finished* window
        }

        return chunksThisSecond; // Return current running total for immediate reaction
    }
}
