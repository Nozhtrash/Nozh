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

    private final FrameTimeSampler sampler;
    private final RollingWindowStats stats;
    private final int windowSeconds;

    public PerfManager() {
        // Calculate capacity based on strict rules
        NozhConfig config = ConfigManager.getConfig();
        this.windowSeconds = 5; // Default window

        int targetFps = Math.max(30, config.targetFps);
        int calcCapacity = targetFps * windowSeconds;
        int capacity = Math.max(60, Math.min(calcCapacity, 600));

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
        return stats.snapshot();
    }

    public void reset() {
        sampler.reset();
    }
}
