package dev.nozh.core.cloud;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.nozh.NozhConstants;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Leaderboard Collector - Tracks local performance statistics and personal bests.
 * 
 * Purpose:
 * 1. Track performance improvements session-over-session
 * 2. Store "Personal Best" FPS gains
 * 3. Prepare data for future leaderboard submission (opt-in)
 * 
 * Note: Currently operates in "Local Mode" (data stored on disk only).
 */
public final class LeaderboardCollector {

    private static final LeaderboardCollector INSTANCE = new LeaderboardCollector();
    private static final Gson GSON = new Gson();
    private static final int MAX_HISTORY_ENTRIES = 50;
    
    // Stats storage
    private final List<SessionStat> history = new ArrayList<>();
    private SessionStat personalBest = null;
    
    private LeaderboardCollector() {
        loadStats();
    }

    public static LeaderboardCollector getInstance() {
        return INSTANCE;
    }

    /**
     * Record statistics for a completed session.
     * 
     * @param durationSeconds session length
     * @param avgFpsBase estimated base FPS (without optimizations)
     * @param avgFpsActual actual average FPS
     * @param scenario dominant scenario (e.g., "COMBAT", "FARM")
     */
    public void recordSession(long durationSeconds, double avgFpsBase, double avgFpsActual, String scenario) {
        if (!CloudManager.getInstance().isFeatureEnabled("leaderboards")) {
            return;
        }
        
        if (durationSeconds < 60) {
            return; // Ignore very short sessions
        }
        
        double gainPercent = 0;
        if (avgFpsBase > 0) {
            gainPercent = ((avgFpsActual - avgFpsBase) / avgFpsBase) * 100.0;
        }
        
        SessionStat stat = new SessionStat(
            System.currentTimeMillis(),
            durationSeconds,
            avgFpsActual,
            gainPercent,
            scenario
        );
        
        synchronized (history) {
            history.add(stat);
            
            // Check for PB
            if (personalBest == null || stat.gainPercent > personalBest.gainPercent) {
                personalBest = stat;
                NozhConstants.LOGGER.info("[NOZH] New Personal Best! +{:.1f}% FPS gain in {}", 
                    stat.gainPercent, stat.scenario);
            }
            
            // Trim history
            if (history.size() > MAX_HISTORY_ENTRIES) {
                history.sort(Comparator.comparingLong(s -> s.timestamp));
                while (history.size() > MAX_HISTORY_ENTRIES) {
                    history.remove(0);
                }
            }
        }
        
        saveStats();
    }
    
    public SessionStat getPersonalBest() {
        return personalBest;
    }
    
    public List<SessionStat> getHistory() {
        synchronized (history) {
            return Collections.unmodifiableList(new ArrayList<>(history));
        }
    }
    
    private void saveStats() {
        try {
            JsonObject root = new JsonObject();
            JsonArray arr = new JsonArray();
            
            synchronized (history) {
                for (SessionStat s : history) {
                    arr.add(s.toJson());
                }
            }
            
            root.add("history", arr);
            if (personalBest != null) {
                root.add("personalBest", personalBest.toJson());
            }
            
            Path path = NozhConstants.CONFIG_DIR.resolve("leaderboard_stats.json");
            Files.writeString(path, GSON.toJson(root));
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("[NOZH] Failed to save leaderboard stats", e);
        }
    }
    
    private void loadStats() {
        try {
            Path path = NozhConstants.CONFIG_DIR.resolve("leaderboard_stats.json");
            if (!Files.exists(path)) return;
            
            String json = Files.readString(path);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            
            if (root.has("history")) {
                JsonArray arr = root.getAsJsonArray("history");
                synchronized (history) {
                    history.clear();
                    for (var r : arr) {
                        history.add(SessionStat.fromJson(r.getAsJsonObject()));
                    }
                }
            }
            
            if (root.has("personalBest")) {
                personalBest = SessionStat.fromJson(root.getAsJsonObject("personalBest"));
            }
            
        } catch (Exception e) {
            NozhConstants.LOGGER.warn("[NOZH] Failed to load leaderboard stats");
        }
    }
    
    public record SessionStat(
        long timestamp,
        long durationSeconds,
        double avgFps,
        double gainPercent,
        String scenario
    ) {
        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("timestamp", timestamp);
            obj.addProperty("duration", durationSeconds);
            obj.addProperty("fps", avgFps);
            obj.addProperty("gain", gainPercent);
            obj.addProperty("scenario", scenario);
            return obj;
        }
        
        public static SessionStat fromJson(JsonObject obj) {
            return new SessionStat(
                obj.get("timestamp").getAsLong(),
                obj.get("duration").getAsLong(),
                obj.get("fps").getAsDouble(),
                obj.get("gain").getAsDouble(),
                obj.get("scenario").getAsString()
            );
        }
    }
}
