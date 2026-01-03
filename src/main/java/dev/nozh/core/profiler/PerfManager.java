package dev.nozh.core.profiler;

import dev.nozh.NozhConstants;
import dev.nozh.api.PerfSnapshot;
import dev.nozh.core.config.ConfigManager;
import dev.nozh.core.config.NozhConfig;
import dev.nozh.core.safety.CrashLoopGuard;

/**
 * Orchestrator for performance profiling.
 * 
 * Responsibilities:
 * - Owns Sampler and Stats
 * - Manages lifecycle (reset, update)
 * - Capacity calculation
 * - Exposes Thread-Safe snapshots
 */
public class PerfManager {

    private FrameTimeSampler sampler;
    private RollingWindowStats stats;
    private int windowSeconds;
    private final PerfWindowController windowController;
    private long lastWindowAdjustMillis = 0L;

    public PerfManager() {
        // Calculate capacity based on strict rules
        NozhConfig config = ConfigManager.getConfig();
        this.windowSeconds = 5; // Default window
        this.windowController = new PerfWindowController(3, 10);

        int targetFps = Math.max(30, config.targetFps);
        int capacity = calculateCapacity(targetFps, windowSeconds);
        this.stats = new RollingWindowStats(capacity, windowSeconds);
        this.sampler = new FrameTimeSampler(stats);

        NozhConstants.LOGGER.info("PerfManager initialized. Capacity={} ({}s @ {}fps)",
                capacity, windowSeconds, targetFps);
    }

    /**
     * Called once per frame ONLY if enabled.
     */
    public void onFrame() {
        // Check enabled state efficiently
        if (ConfigManager.getConfig().enabled && !CrashLoopGuard.isInSafeMode()) {
            sampler.onFrame();
        } else if (CrashLoopGuard.isInSafeMode()) {
            // Safe mode shouldn't block measurement according to prompt?
            // Prompt says: "Safe mode NO bloquea medición"
            // Prompt says: "Safe mode BLOCKS ACTIONS, NOT MEASUREMENT."
            // Correcting logic:
            sampler.onFrame();
        }
    }

    public PerfSnapshot getSnapshot() {
        PerfSnapshot snapshot = stats.snapshot();
        adjustWindowIfNeeded(snapshot);
        return snapshot;
    }

    public void reset() {
        sampler.reset();
    }

    private void adjustWindowIfNeeded(PerfSnapshot snapshot) {
        long now = System.currentTimeMillis();
        if (now - lastWindowAdjustMillis < 1000) {
            return;
        }

        int newWindowSeconds = windowController.evaluate(snapshot, windowSeconds, now);
        if (newWindowSeconds != windowSeconds) {
            NozhConfig config = ConfigManager.getConfig();
            int targetFps = Math.max(30, config.targetFps);
            int capacity = calculateCapacity(targetFps, newWindowSeconds);
            windowSeconds = newWindowSeconds;
            stats = new RollingWindowStats(capacity, newWindowSeconds);
            sampler = new FrameTimeSampler(stats);
            NozhConstants.LOGGER.debug("PerfManager window adjusted to {}s (capacity={})", newWindowSeconds, capacity);
        }
        lastWindowAdjustMillis = now;
    }

    private int calculateCapacity(int targetFps, int windowSeconds) {
        int calcCapacity = targetFps * windowSeconds;
        return Math.max(60, Math.min(calcCapacity, 600));
    }
}
