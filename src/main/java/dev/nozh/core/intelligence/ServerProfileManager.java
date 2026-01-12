package dev.nozh.core.intelligence;

import com.google.gson.*;
import dev.nozh.NozhConstants;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server Profile Manager - Manages per-server optimization profiles.
 * 
 * Features:
 * 1. Automatic server identification (IP/name hash)
 * 2. Per-server learned settings and preferences
 * 3. Automatic profile switching on server change
 * 4. Profile persistence across sessions
 * 
 * This allows NOZH to remember what works best on each specific server
 * rather than using a one-size-fits-all approach.
 */
public final class ServerProfileManager {

    private static final String PROFILES_FILENAME = "server_profiles.json";
    private static final int MAX_PROFILES = 50;
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private final Path profilesPath;
    private final Map<String, ServerProfile> profiles = new ConcurrentHashMap<>();
    private String currentServerId = null;
    private boolean dirty = false;

    public ServerProfileManager(Path configDir) {
        this.profilesPath = configDir.resolve(PROFILES_FILENAME);
        loadProfiles();
    }

    /**
     * Called when joining a server.
     * 
     * @param serverAddress the server address (IP:port or hostname)
     * @param serverName optional server name (from MOTD)
     * @return the profile for this server
     */
    public ServerProfile onServerJoin(String serverAddress, String serverName) {
        String serverId = generateServerId(serverAddress);
        currentServerId = serverId;
        
        ServerProfile profile = profiles.get(serverId);
        if (profile == null) {
            profile = new ServerProfile(serverId, serverAddress, serverName);
            profiles.put(serverId, profile);
            dirty = true;
            NozhConstants.LOGGER.info("[NOZH] Created new server profile for: {}", serverAddress);
        } else {
            profile.updateLastSeen();
            profile.incrementJoinCount();
            NozhConstants.LOGGER.info("[NOZH] Loaded existing profile for: {} (joins: {})", 
                serverAddress, profile.joinCount);
        }
        
        enforceMaxProfiles();
        return profile;
    }

    /**
     * Called when leaving a server.
     */
    public void onServerLeave() {
        if (currentServerId != null && dirty) {
            saveProfiles();
        }
        currentServerId = null;
    }

    /**
     * Get current server profile, or null if not connected.
     */
    public ServerProfile getCurrentProfile() {
        if (currentServerId == null) {
            return null;
        }
        return profiles.get(currentServerId);
    }

    /**
     * Get profile by server ID.
     */
    public ServerProfile getProfile(String serverId) {
        return profiles.get(serverId);
    }

    /**
     * Record a successful optimization action on the current server.
     */
    public void recordSuccess(String actionName, double fpsGain) {
        ServerProfile profile = getCurrentProfile();
        if (profile != null) {
            profile.recordActionResult(actionName, true, fpsGain);
            dirty = true;
        }
    }

    /**
     * Record a failed optimization action on the current server.
     */
    public void recordFailure(String actionName) {
        ServerProfile profile = getCurrentProfile();
        if (profile != null) {
            profile.recordActionResult(actionName, false, 0);
            dirty = true;
        }
    }

    /**
     * Get the success rate for an action on the current server.
     * 
     * @return success rate 0-1, or -1 if no data
     */
    public double getActionSuccessRate(String actionName) {
        ServerProfile profile = getCurrentProfile();
        if (profile != null) {
            return profile.getActionSuccessRate(actionName);
        }
        return -1;
    }

    /**
     * Check if an action is known to work well on this server.
     */
    public boolean isActionRecommended(String actionName) {
        double rate = getActionSuccessRate(actionName);
        return rate < 0 || rate >= 0.6; // Unknown or >60% success
    }

    /**
     * Check if an action is known to fail on this server.
     */
    public boolean isActionBlacklisted(String actionName) {
        double rate = getActionSuccessRate(actionName);
        return rate >= 0 && rate < 0.3; // Less than 30% success
    }

    /**
     * Get all known profiles.
     */
    public Collection<ServerProfile> getAllProfiles() {
        return Collections.unmodifiableCollection(profiles.values());
    }

    /**
     * Get number of stored profiles.
     */
    public int getProfileCount() {
        return profiles.size();
    }

    /**
     * Save profiles to disk.
     */
    public void saveProfiles() {
        try {
            Files.createDirectories(profilesPath.getParent());
            
            JsonArray array = new JsonArray();
            for (ServerProfile profile : profiles.values()) {
                array.add(profile.toJson());
            }
            
            Files.writeString(profilesPath, GSON.toJson(array));
            dirty = false;
            NozhConstants.LOGGER.debug("[NOZH] Saved {} server profiles", profiles.size());
        } catch (IOException e) {
            NozhConstants.LOGGER.error("[NOZH] Failed to save server profiles", e);
        }
    }

    /**
     * Load profiles from disk.
     */
    private void loadProfiles() {
        if (!Files.exists(profilesPath)) {
            return;
        }
        
        try {
            String json = Files.readString(profilesPath);
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            
            for (JsonElement element : array) {
                ServerProfile profile = ServerProfile.fromJson(element.getAsJsonObject());
                if (profile != null) {
                    profiles.put(profile.serverId, profile);
                }
            }
            
            NozhConstants.LOGGER.info("[NOZH] Loaded {} server profiles", profiles.size());
        } catch (Exception e) {
            NozhConstants.LOGGER.error("[NOZH] Failed to load server profiles", e);
        }
    }

    /**
     * Generate a unique server ID from the address.
     */
    private String generateServerId(String serverAddress) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(serverAddress.toLowerCase().getBytes());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            // Fallback to simple hash
            return Integer.toHexString(serverAddress.toLowerCase().hashCode());
        }
    }

    /**
     * Remove oldest profiles if we exceed the limit.
     */
    private void enforceMaxProfiles() {
        if (profiles.size() <= MAX_PROFILES) {
            return;
        }
        
        // Sort by last seen, oldest first
        List<ServerProfile> sorted = new ArrayList<>(profiles.values());
        sorted.sort(Comparator.comparingLong(p -> p.lastSeenTimestamp));
        
        // Remove oldest until we're under limit
        int toRemove = profiles.size() - MAX_PROFILES;
        for (int i = 0; i < toRemove && i < sorted.size(); i++) {
            profiles.remove(sorted.get(i).serverId);
        }
        
        NozhConstants.LOGGER.info("[NOZH] Removed {} old server profiles", toRemove);
    }

    /**
     * Server-specific optimization profile.
     */
    public static class ServerProfile {
        public final String serverId;
        public final String serverAddress;
        public String serverName;
        public long firstSeenTimestamp;
        public long lastSeenTimestamp;
        public int joinCount;
        
        // Per-action statistics
        private final Map<String, ActionStats> actionStats = new ConcurrentHashMap<>();
        
        // Server characteristics (learned over time)
        public double avgTps = 20.0;
        public double avgPing = 50.0;
        public int avgPlayerCount = 0;
        public boolean isLaggy = false;

        public ServerProfile(String serverId, String serverAddress, String serverName) {
            this.serverId = serverId;
            this.serverAddress = serverAddress;
            this.serverName = serverName;
            this.firstSeenTimestamp = System.currentTimeMillis();
            this.lastSeenTimestamp = System.currentTimeMillis();
            this.joinCount = 1;
        }

        public void updateLastSeen() {
            this.lastSeenTimestamp = System.currentTimeMillis();
        }

        public void incrementJoinCount() {
            this.joinCount++;
        }

        public void recordActionResult(String actionName, boolean success, double fpsGain) {
            ActionStats stats = actionStats.computeIfAbsent(actionName, k -> new ActionStats());
            stats.record(success, fpsGain);
        }

        public double getActionSuccessRate(String actionName) {
            ActionStats stats = actionStats.get(actionName);
            if (stats == null || stats.attempts < 3) {
                return -1; // Not enough data
            }
            return stats.getSuccessRate();
        }

        public double getActionAvgGain(String actionName) {
            ActionStats stats = actionStats.get(actionName);
            if (stats == null || stats.successes == 0) {
                return 0;
            }
            return stats.totalGain / stats.successes;
        }

        public void updateServerMetrics(double tps, double ping, int playerCount) {
            // EMA update
            double alpha = 0.1;
            avgTps = (alpha * tps) + ((1 - alpha) * avgTps);
            avgPing = (alpha * ping) + ((1 - alpha) * avgPing);
            avgPlayerCount = (int) ((alpha * playerCount) + ((1 - alpha) * avgPlayerCount));
            isLaggy = avgTps < 18.0;
        }

        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("serverId", serverId);
            obj.addProperty("serverAddress", serverAddress);
            obj.addProperty("serverName", serverName);
            obj.addProperty("firstSeen", firstSeenTimestamp);
            obj.addProperty("lastSeen", lastSeenTimestamp);
            obj.addProperty("joinCount", joinCount);
            obj.addProperty("avgTps", avgTps);
            obj.addProperty("avgPing", avgPing);
            obj.addProperty("avgPlayerCount", avgPlayerCount);
            obj.addProperty("isLaggy", isLaggy);
            
            JsonObject actions = new JsonObject();
            for (Map.Entry<String, ActionStats> entry : actionStats.entrySet()) {
                actions.add(entry.getKey(), entry.getValue().toJson());
            }
            obj.add("actionStats", actions);
            
            return obj;
        }

        public static ServerProfile fromJson(JsonObject obj) {
            try {
                String serverId = obj.get("serverId").getAsString();
                String serverAddress = obj.get("serverAddress").getAsString();
                String serverName = obj.has("serverName") ? obj.get("serverName").getAsString() : null;
                
                ServerProfile profile = new ServerProfile(serverId, serverAddress, serverName);
                profile.firstSeenTimestamp = obj.get("firstSeen").getAsLong();
                profile.lastSeenTimestamp = obj.get("lastSeen").getAsLong();
                profile.joinCount = obj.get("joinCount").getAsInt();
                
                if (obj.has("avgTps")) profile.avgTps = obj.get("avgTps").getAsDouble();
                if (obj.has("avgPing")) profile.avgPing = obj.get("avgPing").getAsDouble();
                if (obj.has("avgPlayerCount")) profile.avgPlayerCount = obj.get("avgPlayerCount").getAsInt();
                if (obj.has("isLaggy")) profile.isLaggy = obj.get("isLaggy").getAsBoolean();
                
                if (obj.has("actionStats")) {
                    JsonObject actions = obj.getAsJsonObject("actionStats");
                    for (String key : actions.keySet()) {
                        profile.actionStats.put(key, ActionStats.fromJson(actions.getAsJsonObject(key)));
                    }
                }
                
                return profile;
            } catch (Exception e) {
                return null;
            }
        }

        public String summary() {
            return String.format(
                "%s | Joins: %d | TPS: %.1f | Ping: %.0fms | Actions: %d",
                serverName != null ? serverName : serverAddress,
                joinCount,
                avgTps,
                avgPing,
                actionStats.size()
            );
        }
    }

    /**
     * Statistics for a single action on a specific server.
     */
    private static class ActionStats {
        int attempts = 0;
        int successes = 0;
        double totalGain = 0;

        void record(boolean success, double fpsGain) {
            attempts++;
            if (success) {
                successes++;
                totalGain += fpsGain;
            }
        }

        double getSuccessRate() {
            if (attempts == 0) return 0;
            return (double) successes / attempts;
        }

        JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("attempts", attempts);
            obj.addProperty("successes", successes);
            obj.addProperty("totalGain", totalGain);
            return obj;
        }

        static ActionStats fromJson(JsonObject obj) {
            ActionStats stats = new ActionStats();
            stats.attempts = obj.get("attempts").getAsInt();
            stats.successes = obj.get("successes").getAsInt();
            stats.totalGain = obj.get("totalGain").getAsDouble();
            return stats;
        }
    }
}
