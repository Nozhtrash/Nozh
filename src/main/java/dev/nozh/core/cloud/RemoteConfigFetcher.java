package dev.nozh.core.cloud;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.nozh.NozhConstants;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Remote Config Fetcher - Fetches dynamic configuration from the cloud.
 * 
 * Primary use case: Updating mod compatibility rules without requiring a mod
 * update.
 * Uses GitHub Raw as a simple CDN.
 * 
 * Features:
 * 1. Async fetching
 * 2. Local caching (fallback if offline)
 * 3. Hot-reloading of compatibility rules
 */
public final class RemoteConfigFetcher {

    private static final RemoteConfigFetcher INSTANCE = new RemoteConfigFetcher();

    // Default URL (placeholder - would point to main repo in production)
    private static final String DEFAULT_CONFIG_URL = "https://raw.githubusercontent.com/Nozhtrash/Nozh-Testing/main/resources/compatibility.json";

    private static final Gson GSON = new Gson();

    private JsonObject cachedConfig = null;
    private long lastFetchTime = 0;
    private static final long CACHE_TTL_MS = 3600_000; // 1 hour

    private RemoteConfigFetcher() {
    }

    public static RemoteConfigFetcher getInstance() {
        return INSTANCE;
    }

    /**
     * Fetch configuration from the cloud.
     * If cached and fresh, returns cache.
     * Otherwise fetches async and updates cache.
     */
    public CompletableFuture<JsonObject> fetchConfig() {
        if (!CloudManager.getInstance().isFeatureEnabled("compat_cloud")) {
            return CompletableFuture.completedFuture(getLocalFallback());
        }

        // Return cache if fresh
        if (cachedConfig != null && (System.currentTimeMillis() - lastFetchTime < CACHE_TTL_MS)) {
            return CompletableFuture.completedFuture(cachedConfig);
        }

        return CloudManager.getInstance().submitTask(() -> {
            try {
                NozhConstants.LOGGER.info("[NOZH] Fetching remote config from: {}", DEFAULT_CONFIG_URL);

                URL url = new URL(DEFAULT_CONFIG_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000); // 5s timeout
                conn.setReadTimeout(5000);
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == 200) {
                    try (InputStreamReader reader = new InputStreamReader(conn.getInputStream())) {
                        JsonObject config = JsonParser.parseReader(reader).getAsJsonObject();

                        // Validate basic structure
                        if (config.has("version") && config.has("compatibility")) {
                            cachedConfig = config;
                            lastFetchTime = System.currentTimeMillis();
                            saveToDisk(config); // Update local cache file
                            String version = config.get("version").getAsString().replace('\n', '_').replace('\r', '_');
                            NozhConstants.LOGGER.info("[NOZH] Remote config updated (v{})", version);
                        } else {
                            NozhConstants.LOGGER.warn("[NOZH] Invalid remote config format");
                        }
                    }
                } else {
                    NozhConstants.LOGGER.warn("[NOZH] Failed to fetch remote config: HTTP {}", conn.getResponseCode());
                }

            } catch (Exception e) {
                NozhConstants.LOGGER.warn("[NOZH] Remote config fetch failed: {}", e.getMessage());
            }
        }).thenApply(v -> {
            // Return cached (new or old) or fallback if everything failed
            return cachedConfig != null ? cachedConfig : getLocalFallback();
        });
    }

    /**
     * Get the cached config immediately (non-blocking).
     * Returns fallback if no cache available.
     */
    public JsonObject getConfigNow() {
        return cachedConfig != null ? cachedConfig : getLocalFallback();
    }

    private JsonObject getLocalFallback() {
        // Try to load from disk cache first
        try {
            Path cachePath = NozhConstants.CONFIG_DIR.resolve("compatibility_cache.json");
            if (Files.exists(cachePath)) {
                String json = Files.readString(cachePath);
                return JsonParser.parseString(json).getAsJsonObject();
            }
        } catch (Exception e) {
            // Ignore
        }

        // Return minimal default
        JsonObject defaultConf = new JsonObject();
        defaultConf.addProperty("version", "0.0.0");
        defaultConf.add("compatibility", new JsonObject());
        return defaultConf;
    }

    private void saveToDisk(JsonObject config) {
        try {
            Path cachePath = NozhConstants.CONFIG_DIR.resolve("compatibility_cache.json");
            Files.createDirectories(cachePath.getParent());
            Files.writeString(cachePath, GSON.toJson(config));
        } catch (Exception e) {
            NozhConstants.LOGGER.error("[NOZH] Failed to save config cache", e);
        }
    }
}
