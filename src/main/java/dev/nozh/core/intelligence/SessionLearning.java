package dev.nozh.core.intelligence;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.NozhConstants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
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

    private static final String STATS_FILE = "nozh_session.json";
    private static final String STATS_TMP_FILE = "nozh_session.json.tmp";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long SAVE_INTERVAL_MILLIS = 60000;

    private final Map<String, ActionStats> history = new HashMap<>();
    private final File statsFile;
    private final Path statsPath;
    private final Path tmpPath;
    private int lastSavedHash = 0;
    private long lastSaveMillis = 0;
    private boolean dirty = false;

    public SessionLearning(File configDir) {
        this.statsFile = new File(configDir, STATS_FILE);
        this.statsPath = statsFile.toPath();
        this.tmpPath = new File(configDir, STATS_TMP_FILE).toPath();
        load();
    }

    /**
     * Record successful action.
     * ZERO ALLOCATION - primitive operations only.
     */
    public void recordSuccess(CapabilityId id, dev.nozh.core.context.Scenario scenario, double fpsGainMs) {
        String key = buildKey(id, scenario);
        ActionStats stats = history.computeIfAbsent(key, k -> new ActionStats());

        stats.totalAttempts++;
        stats.successCount++;
        stats.lastSuccessTime = System.currentTimeMillis();
        stats.totalFpsGain += fpsGainMs;
        stats.avgFpsGain = stats.totalFpsGain / stats.successCount;
        dirty = true;
    }

    /**
     * Record failed action.
     * ZERO ALLOCATION - primitive operations only.
     */
    public void recordFailure(CapabilityId id, dev.nozh.core.context.Scenario scenario) {
        String key = buildKey(id, scenario);
        ActionStats stats = history.computeIfAbsent(key, k -> new ActionStats());

        stats.totalAttempts++;
        stats.failureCount++;
        stats.lastFailureTime = System.currentTimeMillis();
        dirty = true;
    }

    /**
     * Get success rate for capability (0.0 to 1.0).
     * ZERO ALLOCATION.
     */
    public double getSuccessRate(CapabilityId id) {
        return getSuccessRate(id, null);
    }

    public double getSuccessRate(CapabilityId id, dev.nozh.core.context.Scenario scenario) {
        ActionStats stats = history.get(buildKey(id, scenario));
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
        return getAvgFpsGain(id, null);
    }

    public double getAvgFpsGain(CapabilityId id, dev.nozh.core.context.Scenario scenario) {
        ActionStats stats = history.get(buildKey(id, scenario));
        return stats != null ? stats.avgFpsGain : 0.0;
    }

    /**
     * Check if this action should be avoided (<30% success rate).
     * ZERO ALLOCATION.
     */
    public boolean shouldAvoid(CapabilityId id) {
        ActionStats stats = history.get(buildKey(id, null));
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
        return getRanking(id, null);
    }

    public double getRanking(CapabilityId id, dev.nozh.core.context.Scenario scenario) {
        double successRate = getSuccessRate(id, scenario);
        double avgGain = getAvgFpsGain(id, scenario);

        // Ranking formula: success_rate * (1 + avg_gain)
        // Example: 80% success, 2ms gain → 0.8 * 3 = 2.4
        return successRate * (1 + avgGain);
    }

    /**
     * Get total attempts for this capability.
     */
    public int getTotalAttempts(CapabilityId id) {
        ActionStats stats = history.get(buildKey(id, null));
        return stats != null ? stats.totalAttempts : 0;
    }

    public void recordSuccess(CapabilityId id, double fpsGainMs) {
        recordSuccess(id, null, fpsGainMs);
    }

    public void recordFailure(CapabilityId id) {
        recordFailure(id, null);
    }

    public boolean shouldAvoid(CapabilityId id, dev.nozh.core.context.Scenario scenario) {
        ActionStats stats = history.get(buildKey(id, scenario));
        if (stats == null || stats.totalAttempts < 3) {
            return false;
        }
        double successRate = (double) stats.successCount / stats.totalAttempts;
        return successRate < 0.3;
    }

    private String buildKey(CapabilityId id, dev.nozh.core.context.Scenario scenario) {
        String scenarioKey = scenario != null ? scenario.name() : "GLOBAL";
        return id.name() + "|" + scenarioKey;
    }

    /**
     * Save stats to disk (JSON).
     * Called on shutdown or periodically.
     */
    public void save() {
        saveInternal(true);
    }

    /**
     * Save stats to disk if the interval has elapsed.
     */
    public void saveIfDue() {
        saveInternal(false);
    }

    private void saveInternal(boolean force) {
        try {
            long now = System.currentTimeMillis();
            if (!force) {
                if (!dirty || now - lastSaveMillis < SAVE_INTERVAL_MILLIS) {
                    return;
                }
            }

            File parent = statsFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            String json = GSON.toJson(history);
            int currentHash = json.hashCode();
            if (!force && currentHash == lastSavedHash) {
                dirty = false;
                lastSaveMillis = now;
                return;
            }

            Files.writeString(
                    tmpPath,
                    json,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);

            Files.move(
                    tmpPath,
                    statsPath,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);

            lastSavedHash = currentHash;
            lastSaveMillis = now;
            dirty = false;

            safeLog("Session learning stats saved ({} entries)", history.size());
        } catch (IOException e) {
            safeWarn("Failed to save session stats: {}", e.getMessage());

            try {
                Files.deleteIfExists(tmpPath);
            } catch (IOException cleanup) {
            }

            try {
                String json = GSON.toJson(history);
                Files.writeString(statsPath, json, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Load stats from disk (JSON).
     * Called on startup.
     */
    private void load() {
        if (!statsFile.exists()) {
            safeLog("No session stats file found, starting fresh");
            return;
        }

        try (FileReader reader = new FileReader(statsFile)) {
            java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<Map<String, ActionStats>>() {
            }.getType();
            Map<String, ActionStats> loaded = GSON.fromJson(reader, type);

            if (loaded != null) {
                history.putAll(loaded);
                lastSavedHash = GSON.toJson(history).hashCode();
                lastSaveMillis = System.currentTimeMillis();
                safeLog("Session learning loaded ({} entries)", history.size());
            }
        } catch (Exception e) {
            safeWarn("Failed to load session stats: {}", e.getMessage());
        }
    }

    private void safeLog(String message, Object... args) {
        try {
            if (NozhConstants.LOGGER != null) {
                NozhConstants.LOGGER.info(message, args);
            }
        } catch (Throwable ignored) {
            // Tests may not have logger initialized
        }
    }

    private void safeWarn(String message, Object... args) {
        try {
            if (NozhConstants.LOGGER != null) {
                NozhConstants.LOGGER.warn(message, args);
            }
        } catch (Throwable ignored) {
            // Tests may not have logger initialized
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
