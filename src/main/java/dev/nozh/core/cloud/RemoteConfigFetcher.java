package dev.nozh.core.cloud;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.nozh.NozhConstants;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Remote Config Fetcher - Fetches hot-reloadable compatibility rules.
 * 
 * Purpose:
 * 1. Allow updating mod compatibility rules without releasing a new jar.
 * 2. Fetch "knowledge" about new mods from a central repository.
 * 3. Cache results locally to work offline.
 */
public final class RemoteConfigFetcher {

    private static final RemoteConfigFetcher INSTANCE = new RemoteConfigFetcher();
    // Default organization URL for public rules
    private static final String REMOTE_URL = "https://raw.githubusercontent.com/TrxyyPC/nozh-rules/main/compatibility.json";
    private static final String CACHE_FILENAME = "compatibility_cache.json";
    private static final long CACHE_TTL_MS = 3600_000; // 1 hour

    private final HttpClient httpClient;
    private final Path cachePath;

    private JsonObject cachedConfig = null;

    private RemoteConfigFetcher() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.cachePath = NozhConstants.CONFIG_DIR.resolve(CACHE_FILENAME);
    }

    public static RemoteConfigFetcher getInstance() {
        return INSTANCE;
    }

    /**
     * Fetch the config (from cache or remote).
     * Async operation.
     */
    public CompletableFuture<JsonObject> fetch() {
        // 1. Try memory cache
        if (cachedConfig != null) {
            return CompletableFuture.completedFuture(cachedConfig);
        }

        return CompletableFuture.supplyAsync(() -> {
            // 2. Try disk cache if fresh enough
            if (isCacheValid()) {
                try {
                    String json = Files.readString(cachePath);
                    cachedConfig = JsonParser.parseString(json).getAsJsonObject();
                    NozhConstants.LOGGER.info("[NOZH] Loaded compatibility rules from local cache");
                    return cachedConfig;
                } catch (Exception e) {
                    NozhConstants.LOGGER.warn("[NOZH] Failed to read disk cache", e);
                }
            }
            return null;
        }).thenCompose(local -> {
            if (local != null) {
                return CompletableFuture.completedFuture(local);
            }

            // 3. Fetch from Cloud
            return fetchFromCloud();
        });
    }

    private CompletableFuture<JsonObject> fetchFromCloud() {
        if (!CloudManager.getInstance().isFeatureEnabled("compat_cloud")) {
            NozhConstants.LOGGER.info("[NOZH] Cloud compatibility disabled by user");
            return CompletableFuture.completedFuture(new JsonObject());
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(REMOTE_URL))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "Nozh-Optimizer/" + NozhConstants.getVersion())
                .GET()
                .build();

        NozhConstants.LOGGER.info("[NOZH] Fetching compatibility rules from cloud...");

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                            saveCache(response.body());
                            cachedConfig = json;
                            NozhConstants.LOGGER.info("[NOZH] Successfully updated compatibility rules");
                            return json;
                        } catch (Exception e) {
                            NozhConstants.LOGGER.error("[NOZH] Invalid JSON from cloud", e);
                            return getFallbackConfig();
                        }
                    } else {
                        NozhConstants.LOGGER.warn("[NOZH] Failed to fetch rules: HTTP {}", response.statusCode());
                        return loadDiskCacheForce(); // Fallback to stale cache
                    }
                })
                .exceptionally(ex -> {
                    NozhConstants.LOGGER.warn("[NOZH] Cloud fetch failed: {}", ex.getMessage());
                    return loadDiskCacheForce();
                });
    }

    private void saveCache(String json) {
        try {
            Files.writeString(cachePath, json);
        } catch (Exception e) {
            NozhConstants.LOGGER.warn("[NOZH] Failed to save cache", e);
        }
    }

    private boolean isCacheValid() {
        try {
            if (!Files.exists(cachePath))
                return false;
            long lastModified = Files.getLastModifiedTime(cachePath).toMillis();
            return (System.currentTimeMillis() - lastModified) < CACHE_TTL_MS;
        } catch (Exception e) {
            return false;
        }
    }

    private JsonObject loadDiskCacheForce() {
        try {
            if (Files.exists(cachePath)) {
                String json = Files.readString(cachePath);
                cachedConfig = JsonParser.parseString(json).getAsJsonObject();
                NozhConstants.LOGGER.info("[NOZH] Using stale cache as fallback");
                return cachedConfig;
            }
        } catch (Exception e) {
            // ignore
        }
        return getFallbackConfig();
    }

    private JsonObject getFallbackConfig() {
        // Return minimal default config or empty
        return new JsonObject();
    }
}
