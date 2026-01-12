package dev.nozh.core.intelligence;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.NozhConstants;
import dev.nozh.core.governor.ActionOutcome;
import dev.nozh.core.profiler.SpikeCauseType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.EnumMap;
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
    
    // Memory management constants
    private static final int MAX_HISTORY_ENTRIES = 500; // Maximum action-scenario pairs
    private static final long STALE_ENTRY_AGE_MS = 24 * 60 * 60 * 1000L; // 24 hours
    private static final int MIN_ATTEMPTS_TO_KEEP = 3; // Minimum attempts to survive compaction
    private static final double DECAY_FACTOR = 0.5; // Weight halves every decay cycle
    private static final long DECAY_CYCLE_MS = 7 * 24 * 60 * 60 * 1000L; // 7 days

    private static final String DEFAULT_SESSION_KEY = "DEFAULT";
    private final Map<String, ActionStats> history = new HashMap<>();
    private final PredictionStats predictionStats = new PredictionStats();
    private final Map<SpikeCauseType, CausalityStats> causalityHistory = new EnumMap<>(SpikeCauseType.class);
    private final File statsFile;
    private final Path statsPath;
    private final Path tmpPath;
    private int lastSavedHash = 0;
    private long lastSaveMillis = 0;
    private long lastCompactionMillis = 0;
    private boolean dirty = false;
    private String sessionKey = DEFAULT_SESSION_KEY;

    public SessionLearning(File configDir) {
        this.statsFile = new File(configDir, STATS_FILE);
        this.statsPath = statsFile.toPath();
        this.tmpPath = new File(configDir, STATS_TMP_FILE).toPath();
        load();
    }

    public void resetForSession(String newSessionKey) {
        String normalizedKey = normalizeSessionKey(newSessionKey);
        if (normalizedKey.equals(sessionKey)) {
            return;
        }
        sessionKey = normalizedKey;
        history.clear();
        predictionStats.reset();
        causalityHistory.clear();
        lastSavedHash = 0;
        lastSaveMillis = 0;
        dirty = true;
        safeLog("Session learning reset for new session ({})", sessionKey);
    }

    private String normalizeSessionKey(String newSessionKey) {
        if (newSessionKey == null || newSessionKey.isBlank()) {
            return DEFAULT_SESSION_KEY;
        }
        return newSessionKey.trim();
    }

    /**
     * Record successful action.
     * ZERO ALLOCATION - primitive operations only.
     */
    public void recordSuccess(CapabilityId id, dev.nozh.core.context.Scenario scenario, double fpsGainMs) {
        recordOutcome(id, scenario, ActionOutcome.POSITIVE, fpsGainMs);
    }

    /**
     * Record failed action.
     * ZERO ALLOCATION - primitive operations only.
     */
    public void recordFailure(CapabilityId id, dev.nozh.core.context.Scenario scenario) {
        recordOutcome(id, scenario, ActionOutcome.NEGATIVE, 0.0);
    }

    public void recordOutcome(CapabilityId id, dev.nozh.core.context.Scenario scenario, ActionOutcome outcome,
            double fpsGainMs) {
        recordOutcome(id, scenario, outcome, fpsGainMs, 0.0, 0);
    }

    public void recordOutcome(CapabilityId id, dev.nozh.core.context.Scenario scenario, ActionOutcome outcome,
            double fpsGainMs, double p95DeltaMs, int spikeDelta) {
        String key = buildKey(id, scenario);
        ActionStats stats = history.computeIfAbsent(key, k -> new ActionStats());

        stats.totalAttempts++;
        if (outcome == ActionOutcome.POSITIVE) {
            stats.successCount++;
            stats.lastSuccessTime = System.currentTimeMillis();
            stats.totalFpsGain += Math.max(0.0, fpsGainMs);
            stats.avgFpsGain = stats.totalFpsGain / stats.successCount;
        } else if (outcome == ActionOutcome.NEGATIVE) {
            stats.failureCount++;
            stats.lastFailureTime = System.currentTimeMillis();
        } else {
            stats.neutralCount++;
        }
        stats.totalP95DeltaMs += p95DeltaMs;
        stats.avgP95DeltaMs = stats.totalP95DeltaMs / stats.totalAttempts;
        stats.totalSpikeDelta += spikeDelta;
        stats.avgSpikeDelta = (double) stats.totalSpikeDelta / stats.totalAttempts;
        dirty = true;
        
        // Automatic memory management: compact if over limit
        enforceMemoryLimits();
    }
    
    /**
     * Enforces memory limits by compacting history when needed.
     * Removes stale and low-value entries to stay under MAX_HISTORY_ENTRIES.
     */
    private void enforceMemoryLimits() {
        if (history.size() <= MAX_HISTORY_ENTRIES) {
            return;
        }
        
        long now = System.currentTimeMillis();
        
        // Don't compact too frequently (at most once per minute)
        if (now - lastCompactionMillis < 60_000) {
            return;
        }
        lastCompactionMillis = now;
        
        int initialSize = history.size();
        
        // Phase 1: Remove stale entries with few attempts
        java.util.Iterator<Map.Entry<String, ActionStats>> it = history.entrySet().iterator();
        while (it.hasNext() && history.size() > MAX_HISTORY_ENTRIES * 0.8) {
            Map.Entry<String, ActionStats> entry = it.next();
            ActionStats stats = entry.getValue();
            long lastActivity = Math.max(stats.lastSuccessTime, stats.lastFailureTime);
            
            // Remove if old and low attempts
            if (stats.totalAttempts < MIN_ATTEMPTS_TO_KEEP 
                && now - lastActivity > STALE_ENTRY_AGE_MS) {
                it.remove();
            }
        }
        
        // Phase 2: If still over limit, remove lowest-value entries
        if (history.size() > MAX_HISTORY_ENTRIES) {
            // Sort by value (success rate * attempts) and remove bottom 20%
            java.util.List<Map.Entry<String, ActionStats>> sorted = 
                new java.util.ArrayList<>(history.entrySet());
            sorted.sort((a, b) -> {
                double valueA = calculateEntryValue(a.getValue());
                double valueB = calculateEntryValue(b.getValue());
                return Double.compare(valueA, valueB);
            });
            
            int toRemove = history.size() - (int)(MAX_HISTORY_ENTRIES * 0.8);
            for (int i = 0; i < toRemove && i < sorted.size(); i++) {
                history.remove(sorted.get(i).getKey());
            }
        }
        
        int removed = initialSize - history.size();
        if (removed > 0) {
            safeLog("Memory compaction: removed {} entries ({} -> {})", 
                removed, initialSize, history.size());
        }
    }
    
    /**
     * Calculates the value of a history entry for compaction decisions.
     * Higher value = more worth keeping.
     */
    private double calculateEntryValue(ActionStats stats) {
        if (stats.totalAttempts == 0) {
            return 0.0;
        }
        double successRate = (double) stats.successCount / stats.totalAttempts;
        double recency = 1.0;
        long lastActivity = Math.max(stats.lastSuccessTime, stats.lastFailureTime);
        long age = System.currentTimeMillis() - lastActivity;
        if (age > DECAY_CYCLE_MS) {
            recency = DECAY_FACTOR;
        }
        // Value = attempts * success_rate * recency
        return stats.totalAttempts * successRate * recency;
    }

    public void recordPredictionOutcome(boolean predictedSpike, boolean actualSpike, double confidence) {
        predictionStats.totalPredictions++;
        if (predictedSpike == actualSpike) {
            predictionStats.correctPredictions++;
            predictionStats.lastCorrectMillis = System.currentTimeMillis();
        } else {
            predictionStats.incorrectPredictions++;
        }
        predictionStats.lastPredictionMillis = System.currentTimeMillis();
        predictionStats.totalConfidence += Math.max(0.0, confidence);
        predictionStats.avgConfidence = predictionStats.totalPredictions > 0
                ? predictionStats.totalConfidence / predictionStats.totalPredictions
                : 0.0;
        dirty = true;
    }

    public double getPredictionAccuracy() {
        if (predictionStats.totalPredictions <= 0) {
            return 0.0;
        }
        return (double) predictionStats.correctPredictions / predictionStats.totalPredictions;
    }

    public double getPredictionAvgConfidence() {
        return predictionStats.avgConfidence;
    }

    public int getPredictionCount() {
        return predictionStats.totalPredictions;
    }

    public void recordCausality(SpikeCauseType cause, double confidence) {
        if (cause == null || cause == SpikeCauseType.UNKNOWN) {
            return;
        }
        CausalityStats stats = causalityHistory.computeIfAbsent(cause, key -> new CausalityStats());
        stats.totalCount++;
        stats.totalConfidence += Math.max(0.0, confidence);
        stats.avgConfidence = stats.totalCount > 0 ? stats.totalConfidence / stats.totalCount : 0.0;
        stats.lastObservedMillis = System.currentTimeMillis();
        dirty = true;
    }

    public double getCausalityConfidence(SpikeCauseType cause) {
        CausalityStats stats = causalityHistory.get(cause);
        return stats != null ? stats.avgConfidence : 0.0;
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

    public double getAvgP95Gain(CapabilityId id, dev.nozh.core.context.Scenario scenario) {
        ActionStats stats = history.get(buildKey(id, scenario));
        if (stats == null) {
            return 0.0;
        }
        return Math.max(0.0, -stats.avgP95DeltaMs);
    }

    public double getAvgSpikeDelta(CapabilityId id, dev.nozh.core.context.Scenario scenario) {
        ActionStats stats = history.get(buildKey(id, scenario));
        return stats != null ? stats.avgSpikeDelta : 0.0;
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
     * Apply exponential decay to old history data.
     * Uses Iterator to avoid ConcurrentModificationException.
     * 
     * @param maxAgeMillis Maximum age before decay is applied
     */
    public void applyDecay(long maxAgeMillis) {
        java.util.Iterator<Map.Entry<String, ActionStats>> it = history.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ActionStats> entry = it.next();
            ActionStats stats = entry.getValue();
            long lastActivity = Math.max(stats.lastSuccessTime, stats.lastFailureTime);
            
            if (lastActivity < System.currentTimeMillis() - maxAgeMillis) {
                // Apply decay
                stats.totalAttempts = (int)(stats.totalAttempts * 0.5);
                stats.successCount = (int)(stats.successCount * 0.5);
                stats.failureCount = (int)(stats.failureCount * 0.5);
                stats.neutralCount = (int)(stats.neutralCount * 0.5);
                stats.totalFpsGain *= 0.5;
                stats.totalP95DeltaMs *= 0.5;
                stats.totalSpikeDelta = (int)(stats.totalSpikeDelta * 0.5);
                
                // Recalculate averages
                if (stats.successCount > 0) {
                    stats.avgFpsGain = stats.totalFpsGain / stats.successCount;
                }
                if (stats.totalAttempts > 0) {
                    stats.avgP95DeltaMs = stats.totalP95DeltaMs / stats.totalAttempts;
                    stats.avgSpikeDelta = (double) stats.totalSpikeDelta / stats.totalAttempts;
                }
                
                // Remove if decayed too much
                if (stats.totalAttempts < 2) {
                    it.remove();
                }
                
                dirty = true;
            }
        }
    }

    /**
     * Apply decay with default 7-day threshold.
     */
    public void applyDecay() {
        applyDecay(7 * 24 * 60 * 60 * 1000L); // 7 days
    }

    /**
     * Remove low-confidence entries from history.
     * Uses Iterator to avoid ConcurrentModificationException.
     * 
     * @param minAttempts Minimum attempts required to keep entry
     * @return Number of entries removed
     */
    public int compactHistory(int minAttempts) {
        int removed = 0;
        java.util.Iterator<Map.Entry<String, ActionStats>> it = history.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().totalAttempts < minAttempts) {
                it.remove();
                removed++;
            }
        }
        if (removed > 0) {
            dirty = true;
        }
        return removed;
    }

    /**
     * Compact history with default 3-attempt threshold.
     */
    public int compactHistory() {
        return compactHistory(3);
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
            if (scenario != null) {
                return shouldAvoid(id);
            }
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

            String json = GSON.toJson(new SessionData(sessionKey, history, predictionStats, causalityHistory));
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
                String json = GSON.toJson(new SessionData(sessionKey, history, predictionStats, causalityHistory));
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
            JsonElement element = JsonParser.parseReader(reader);
            if (element != null && element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                if (object.has("history")) {
                    SessionData data = GSON.fromJson(object, SessionData.class);
                    if (data != null) {
                        sessionKey = normalizeSessionKey(data.sessionKey);
                        if (data.history != null) {
                            history.putAll(data.history);
                        }
                        if (data.predictionStats != null) {
                            predictionStats.copyFrom(data.predictionStats);
                        }
                        if (data.causalityHistory != null) {
                            causalityHistory.putAll(data.causalityHistory);
                        }
                    }
                } else {
                    java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<Map<String, ActionStats>>() {
                    }.getType();
                    Map<String, ActionStats> loaded = GSON.fromJson(object, type);
                    if (loaded != null) {
                        history.putAll(loaded);
                    }
                }
            } else {
                java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<Map<String, ActionStats>>() {
                }.getType();
                Map<String, ActionStats> loaded = GSON.fromJson(element, type);
                if (loaded != null) {
                    history.putAll(loaded);
                }
            }

            lastSavedHash = GSON.toJson(new SessionData(sessionKey, history, predictionStats, causalityHistory))
                    .hashCode();
            lastSaveMillis = System.currentTimeMillis();
            safeLog("Session learning loaded ({} entries)", history.size());
        } catch (Exception e) {
            safeWarn("Failed to load session stats: {}", e.getMessage());
        }
    }

    private void safeLog(String message, Object... args) {
        try {
            if (NozhConstants.LOGGER != null) {
                NozhConstants.LOGGER.info(message, args);
            }
        } catch (Exception e) {
            // Tests may not have logger initialized
        }
    }

    private void safeWarn(String message, Object... args) {
        try {
            if (NozhConstants.LOGGER != null) {
                NozhConstants.LOGGER.warn(message, args);
            }
        } catch (Exception e) {
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
        public int neutralCount = 0;
        public long lastSuccessTime = 0;
        public long lastFailureTime = 0;
        public double totalFpsGain = 0.0;
        public double avgFpsGain = 0.0;
        public double totalP95DeltaMs = 0.0;
        public double avgP95DeltaMs = 0.0;
        public int totalSpikeDelta = 0;
        public double avgSpikeDelta = 0.0;
    }

    public static class PredictionStats {
        public int totalPredictions = 0;
        public int correctPredictions = 0;
        public int incorrectPredictions = 0;
        public long lastPredictionMillis = 0;
        public long lastCorrectMillis = 0;
        public double totalConfidence = 0.0;
        public double avgConfidence = 0.0;

        private void copyFrom(PredictionStats other) {
            if (other == null) {
                return;
            }
            this.totalPredictions = other.totalPredictions;
            this.correctPredictions = other.correctPredictions;
            this.incorrectPredictions = other.incorrectPredictions;
            this.lastPredictionMillis = other.lastPredictionMillis;
            this.lastCorrectMillis = other.lastCorrectMillis;
            this.totalConfidence = other.totalConfidence;
            this.avgConfidence = other.avgConfidence;
        }

        private void reset() {
            totalPredictions = 0;
            correctPredictions = 0;
            incorrectPredictions = 0;
            lastPredictionMillis = 0;
            lastCorrectMillis = 0;
            totalConfidence = 0.0;
            avgConfidence = 0.0;
        }
    }

    public static class CausalityStats {
        public int totalCount = 0;
        public double totalConfidence = 0.0;
        public double avgConfidence = 0.0;
        public long lastObservedMillis = 0;
    }

    private static final class SessionData {
        public String sessionKey;
        public Map<String, ActionStats> history;
        public PredictionStats predictionStats;
        public Map<SpikeCauseType, CausalityStats> causalityHistory;

        private SessionData(String sessionKey, Map<String, ActionStats> history, PredictionStats predictionStats,
                Map<SpikeCauseType, CausalityStats> causalityHistory) {
            this.sessionKey = sessionKey;
            this.history = history;
            this.predictionStats = predictionStats;
            this.causalityHistory = causalityHistory;
        }
    }
}
