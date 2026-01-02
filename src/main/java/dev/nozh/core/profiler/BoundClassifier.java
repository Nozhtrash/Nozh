package dev.nozh.core.profiler;

import dev.nozh.NozhConstants;
import dev.nozh.api.Bound;
import dev.nozh.api.PerfSnapshot;

/**
 * Classifies performance bottlenecks.
 * 
 * Phase 4: Read-Only / Conservative
 * - Returns UNKNOWN strictly if insufficient data
 * - Does NOT act or tune
 * - No heuristics yet (waiting for Tick Time in future phases)
 */
public class BoundClassifier {

    /**
     * Classify the current performance bound based on a snapshot.
     */
    public Bound classify(PerfSnapshot snapshot) {
        if (!snapshot.sufficientData()) {
            logDebug("Insufficient data -> UNKNOWN");
            return Bound.UNKNOWN;
        }

        // Phase 4 strict rule: Unknown is better than wrong.
        // Without tick time, we cannot reliably distinguish CPU/GPU/Mix.
        // So we default to UNKNOWN until Phase 4.5/5 adds tick sampling.

        logDebug("Snapshot valid (avg=%.2fms) -> UNKNOWN (Phase 4 limit)", snapshot.avgFrametimeMs());

        return Bound.UNKNOWN;
    }

    private void logDebug(String format, Object... args) {
        // Only log if explicitly debug enabled to prevent spam
        if (dev.nozh.core.config.ConfigManager.getConfig().debugLogs) {
            // Log with throttler or just plain debug?
            // Prompt says: "Debug log solo si debug=true"
            // We'll rely on logger for now, user manually enabled it.
            NozhConstants.LOGGER.debug("[BoundClassifier] " + String.format(format, args));
        }
    }
}
