package dev.nozh.core.update;

import dev.nozh.NozhConstants;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * Checks for mod updates and notifies users.
 * Non-intrusive update checking system.
 * 
 * @since 0.3.0
 * @author NOZH Team
 */
public final class AutoUpdater {

    private static final String CURRENT_VERSION = "0.3.0";
    private static final String MODRINTH_API = "https://api.modrinth.com/v2/project/nozh/version";
    private static final String GITHUB_API = "https://api.github.com/repos/Nozhtrash/Nozh-Testing/releases/latest";

    /**
     * Update check result.
     */
    public record UpdateInfo(
            boolean updateAvailable,
            String currentVersion,
            String latestVersion,
            String downloadUrl,
            String changelog,
            boolean isCritical) {
        public String getVersionDiff() {
            return String.format("%s → %s", currentVersion, latestVersion);
        }
    }

    private final ExecutorService executor;
    private UpdateInfo cachedResult;
    private long lastCheckTime;
    private static final long CHECK_INTERVAL = 3600000; // 1 hour

    private boolean enabled;
    private boolean notifyOnUpdate;

    /**
     * Sanitizes a string for safe logging by removing control characters and line
     * breaks.
     * Prevents log injection attacks.
     *
     * @param value string to sanitize
     * @return sanitized string safe for logging
     */
    private static String sanitizeForLogging(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\r' || c == '\n' || Character.isISOControl(c)) {
                sb.append(' ');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Constructs a new AutoUpdater.
     */
    public AutoUpdater() {
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "NOZH-UpdateChecker");
            t.setDaemon(true);
            return t;
        });
        this.enabled = true;
        this.notifyOnUpdate = true;
    }

    /**
     * Checks for updates asynchronously.
     * 
     * @return future containing update info
     */
    public CompletableFuture<UpdateInfo> checkForUpdates() {
        if (!enabled) {
            return CompletableFuture.completedFuture(
                    new UpdateInfo(false, CURRENT_VERSION, CURRENT_VERSION, null, null, false));
        }

        // Return cached result if recent
        if (cachedResult != null && System.currentTimeMillis() - lastCheckTime < CHECK_INTERVAL) {
            return CompletableFuture.completedFuture(cachedResult);
        }

        return CompletableFuture.supplyAsync(this::doUpdateCheck, executor);
    }

    /**
     * Performs the actual update check.
     */
    private UpdateInfo doUpdateCheck() {
        NozhConstants.LOGGER.info("Checking for updates...");

        try {
            // Try GitHub first
            String latestVersion = fetchLatestVersionFromGitHub();

            if (latestVersion != null && isNewerVersion(latestVersion)) {
                // Sanitize version string before storing/logging to prevent log injection
                String safeLatestVersion = sanitizeForLogging(latestVersion);
                UpdateInfo info = new UpdateInfo(
                        true,
                        CURRENT_VERSION,
                        safeLatestVersion,
                        "https://github.com/Nozhtrash/Nozh-Testing/releases/latest",
                        null,
                        false);
                cachedResult = info;
                lastCheckTime = System.currentTimeMillis();

                NozhConstants.LOGGER.info("Update available: {}", info.getVersionDiff());
                return info;
            }

            // No update available
            UpdateInfo info = new UpdateInfo(
                    false,
                    CURRENT_VERSION,
                    CURRENT_VERSION,
                    null,
                    "You are running the latest version",
                    false);

            cachedResult = info;
            lastCheckTime = System.currentTimeMillis();

            NozhConstants.LOGGER.info("No updates available (current: {})", CURRENT_VERSION);
            return info;

        } catch (Exception e) {
            NozhConstants.LOGGER.warn("Failed to check for updates: {}", e.getMessage());
            return new UpdateInfo(false, CURRENT_VERSION, CURRENT_VERSION, null,
                    "Could not check for updates", false);
        }
    }

    /**
     * Fetches latest version from GitHub.
     */
    private String fetchLatestVersionFromGitHub() {
        try {
            URL url = new URI(GITHUB_API).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    // Simple JSON parsing for tag_name
                    String json = response.toString();
                    int tagIndex = json.indexOf("\"tag_name\"");
                    if (tagIndex != -1) {
                        int colonIndex = json.indexOf(":", tagIndex);
                        int startQuote = json.indexOf("\"", colonIndex + 1);
                        int endQuote = json.indexOf("\"", startQuote + 1);
                        if (startQuote != -1 && endQuote != -1) {
                            String tag = json.substring(startQuote + 1, endQuote);
                            return tag.startsWith("v") ? tag.substring(1) : tag;
                        }
                    }
                }
            }
        } catch (Exception e) {
            NozhConstants.LOGGER.debug("GitHub check failed: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Compares versions to determine if update is available.
     * 
     * @param latestVersion latest version string
     * @return true if latest is newer than current
     */
    private boolean isNewerVersion(String latestVersion) {
        try {
            String[] current = CURRENT_VERSION.split("\\.");
            String[] latest = latestVersion.split("\\.");

            int length = Math.max(current.length, latest.length);

            for (int i = 0; i < length; i++) {
                int currentPart = i < current.length ? Integer.parseInt(current[i].replaceAll("[^0-9]", "")) : 0;
                int latestPart = i < latest.length ? Integer.parseInt(latest[i].replaceAll("[^0-9]", "")) : 0;

                if (latestPart > currentPart)
                    return true;
                if (latestPart < currentPart)
                    return false;
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Enables or disables update checking.
     * 
     * @param enabled true to enable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Checks if updates are enabled.
     * 
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether to notify on update.
     * 
     * @param notify true to notify
     */
    public void setNotifyOnUpdate(boolean notify) {
        this.notifyOnUpdate = notify;
    }

    /**
     * Gets current mod version.
     * 
     * @return current version string
     */
    public static String getCurrentVersion() {
        return CURRENT_VERSION;
    }

    /**
     * Shuts down the update checker.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
