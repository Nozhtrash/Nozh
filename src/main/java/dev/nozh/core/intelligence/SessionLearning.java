package dev.nozh.core.intelligence;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.NozhConstants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Session Learning - Persistent AI that remembers what works on THIS specific
 * PC.
 * 
 * Intelligence: Learns from history and adapts recommendations based on actual
 * results.
 * This is THE killer feature that makes NOZH truly smart.
 * 
 * ZERO ALLOCATION in hot paths - only allocates during save/load.
 */
public final class SessionLearning {

    private static final String STATS_FILE = "session_stats.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Map<String, ActionStats> history = new HashMap<>();
    private final File statsFile;

    public SessionLearning(File configDir) {
        this.statsFile = new File(configDir, STATS_FILE);
        load();
    }

    /**
     * Record successful action.
     * ZERO ALLOCATION - primitive operations only.
     */
    public void recordSuccess(CapabilityId id, double fpsGainMs) {
        String key = id.name();
        ActionStats stats = history.computeIfAbsent(key, k -> new ActionStats());

        stats.totalAttempts++;
        stats.successCount++;
        stats.lastSuccessTime = System.currentTimeMillis();
        stats.totalFpsGain += fpsGainMs;
        stats.avgFpsGain = stats.totalFpsGain / stats.successCount;
    }

    /**
     * Record failed action.
     * ZERO ALLOCATION - primitive operations only.
     */
    public void recordFailure(CapabilityId id) {
        String key = id.name();
        ActionStats stats = history.computeIfAbsent(key, k -> new ActionStats());

        stats.totalAttempts++;
        stats.failureCount++;
        stats.lastFailureTime = System.currentTimeMillis();
    }

    /**
     * Get success rate for capability (0.0 to 1.0).
     * ZERO ALLOCATION.
     */
    public double getSuccessRate(CapabilityId id) {
        ActionStats stats = history.get(id.name());
        if (stats == null || stats.totalAttempts == 0) {
            return 0.5; // Default 50% confidence for unknowns
        }
        return (double) stats.successCount / stats.totalAttempts;
    }

    /**
     * Get average FPS gain from this action (if successful).
     * ZERO ALLOCATION.
     */
    public double getAvgFpsGain(CapabilityId id) {
        ActionStats stats = history.get(id.name());
        return stats != null ? stats.avgFpsGain : 0.0;
    }

    /**
     * Check if this action should be avoided (<30% success rate).
     * ZERO ALLOCATION.
     */
    public boolean shouldAvoid(CapabilityId id) {
        ActionStats stats = history.get(id.name());
        if (stats == null || stats.totalAttempts < 3) {
            return false; // Need at least 3 attempts to judge
        }

        double successRate = (double) stats.successCount / stats.totalAttempts;
        return successRate < 0.3; // Less than 30% success = avoid
    }

    /**
     * Get provider ranking (higher = better).
     * Combines success rate + avg FPS gain.
     * ZERO ALLOCATION.
     */
    public double getRanking(CapabilityId id) {
        double successRate = getSuccessRate(id);
        double avgGain = getAvgFpsGain(id);

        // Ranking formula: success_rate * (1 + avg_gain)
        // Example: 80% success, 2ms gain → 0.8 * 3 = 2.4
        return successRate * (1 + avgGain);
    }

    /**
     * Get total attempts for this capability.
     */
    public int getTotalAttempts(CapabilityId id) {
        ActionStats stats = history.get(id.name());
        return stats != null ? stats.totalAttempts : 0;
    }

    /**
     * Save stats to disk (JSON).
     * Called on shutdown or periodically.
     */
    public void save() {
        try {
            // Ensure parent directory exists
            File parent = statsFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            // Write JSON
            try (FileWriter writer = new FileWriter(statsFile)) {
                GSON.toJson(history, writer);
            }

            NozhConstants.LOGGER.info("Session learning stats saved ({} entries)", history.size());
        } catch (Exception e) {
            NozhConstants.LOGGER.warn("Failed to save session stats: {}", e.getMessage());
        }
    }

    /**
     * Load stats from disk (JSON).
     * Called on startup.
     */
    private void load() {
        if (!statsFile.exists()) {
            NozhConstants.LOGGER.info("No session stats file found, starting fresh");
            return;
        }

        try (FileReader reader = new FileReader(statsFile)) {
            java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<Map<String, ActionStats>>() {
            }.getType();
            Map<String, ActionStats> loaded = GSON.fromJson(reader, type);

            if (loaded != null) {
                history.putAll(loaded);
                NozhConstants.LOGGER.info("Session learning loaded ({} entries)", history.size());
            }
        } catch (Exception e) {
            NozhConstants.LOGGER.warn("Failed to load session stats: {}", e.getMessage());
        }
    }

    /**
     * Stats for a single action capability.
     */
    public static class ActionStats {
        public int totalAttempts = 0;
        public int successCount = 0;
        public int failureCount = 0;
        public long lastSuccessTime = 0;
        public long lastFailureTime = 0;
        public double totalFpsGain = 0.0;
        public double avgFpsGain = 0.0;
    }
}
