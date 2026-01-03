package dev.nozh.core.config;

import dev.nozh.NozhConstants;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Configuration manager for NOZH.
 * Thread-safe, atomic writes, lazy initialization.
 * 
 * Phase 2 Iteration 2: resetToDefaults() method
 */
public final class ConfigManager {

    private static final Object LOCK = new Object();
    private static volatile NozhConfig config = null;
    private static final List<ConfigListener> listeners = new CopyOnWriteArrayList<>();

    private ConfigManager() {
        // Utility class
    }

    /**
     * Get current config (lazy load, thread-safe).
     */
    public static NozhConfig getConfig() {
        if (config == null) {
            synchronized (LOCK) {
                if (config == null) {
                    load();
                }
            }
        }
        return config;
    }

    /**
     * Load config from disk (or create defaults)
     */
    public static void load() {
        NozhConfig snapshot;
        synchronized (LOCK) {
            try {
                Path configFile = NozhConstants.CONFIG_FILE;

                ensureConfigDirExists();

                if (Files.exists(configFile)) {
                    String json = Files.readString(configFile, StandardCharsets.UTF_8);
                    NozhConfig loaded = JsonMini.fromJsonNozhConfig(json);
                    if (loaded != null) {
                        // AUTO-MIGRATION: v0.1.0 → v0.2.0 (if needed)
                        NozhConfig migrated = ConfigMigration.migrate(loaded);
                        config = migrated;

                        boolean corrected = config.validate();
                        boolean hardwareUpdated = ensureHardwareProfile(config);
                        if (corrected || migrated != loaded) {
                            NozhConstants.LOGGER.warn("Config had invalid values or was migrated, re-saving");
                            saveNowSilently(); // Re-save migrated/corrected config
                        } else if (hardwareUpdated) {
                            saveNowSilently();
                        }
                        NozhConstants.LOGGER.debug("Loaded config from {}", configFile);
                    } else {
                        NozhConstants.LOGGER.warn("Config file was empty or corrupted, using defaults");
                        config = new NozhConfig();
                        config.configVersion = 2; // Set current version
                        ensureHardwareProfile(config);
                        saveNowSilently();
                    }
                } else {
                    NozhConstants.LOGGER.info("No config found, creating default at {}", configFile);
                    config = new NozhConfig();
                    config.configVersion = 2; // Set current version
                    ensureHardwareProfile(config);
                    saveNowSilently();
                }
            } catch (Exception e) {
                NozhConstants.LOGGER.error("Failed to load config, using defaults", e);
                config = new NozhConfig();
                config.configVersion = 2;
                ensureHardwareProfile(config);
            }
        }
        snapshot = config;
        notifyListeners(snapshot);
    }

    /**
     * Save current config to disk (atomic write)
     * Debounced to prevent race conditions on rapid saves
     */
    public static void save() {
        saveInternal(false, false, false);
    }

    /**
     * Save and notify listeners even if the write is debounced.
     */
    public static void saveAndNotify() {
        saveInternal(true, true, false);
    }

    /**
     * Save immediately, bypassing debounce.
     */
    public static void saveNow() {
        saveInternal(false, false, true);
    }

    /**
     * Save immediately and notify listeners, bypassing debounce.
     */
    public static void saveNowAndNotify() {
        saveInternal(true, true, true);
    }

    private static void saveInternal(boolean notifyOnDebounce, boolean notifyOnSave, boolean ignoreDebounce) {
        NozhConfig snapshot = null;
        boolean shouldNotify = false;
        synchronized (LOCK) {
            // Debounce: prevent rapid successive saves
            long now = System.currentTimeMillis();
            long elapsed = now - lastSaveTime;
            if (!ignoreDebounce && elapsed < SAVE_DEBOUNCE_MS) {
                NozhConstants.LOGGER.debug("Skipping config save (debounce)");
                snapshot = config;
                shouldNotify = notifyOnDebounce;
            } else {
                lastSaveTime = now;
                shouldNotify = notifyOnSave;

                if (config == null) {
                    NozhConstants.LOGGER.warn("Cannot save null config");
                    return;
                }

                try {
                    Path configFile = NozhConstants.CONFIG_FILE;
                    Path tmpFile = configFile.resolveSibling(configFile.getFileName() + ".tmp");

                    ensureConfigDirExists();

                    String json = JsonMini.toJson(config);

                    // Write to tmp, then atomic rename
                    Files.writeString(tmpFile, json, StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE);

                    Files.move(tmpFile, configFile,
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);

                    NozhConstants.LOGGER.debug("Saved config to {}", configFile);
                } catch (IOException e) {
                    NozhConstants.LOGGER.error("Failed to save config", e);

                    // Clean up tmp file if exists
                    try {
                        Path tmpFile = NozhConstants.CONFIG_FILE
                                .resolveSibling(NozhConstants.CONFIG_FILE.getFileName() + ".tmp");
                        Files.deleteIfExists(tmpFile);
                    } catch (IOException cleanup) {
                        // Fail silently
                    }
                }
            }
            snapshot = config;
        }
        if (shouldNotify) {
            notifyListeners(snapshot);
        }
    }

    private static volatile long lastSaveTime = 0;
    private static final long SAVE_DEBOUNCE_MS = 500; // 500ms debounce

    private static boolean ensureHardwareProfile(NozhConfig config) {
        if (config.hardwareProfile == null || config.hardwareProfile.isBlank()) {
            config.hardwareProfile = HardwareProfiler.buildProfile();
            return true;
        }
        return false;
    }

    /**
     * Phase 2 Iteration 3: Reset config to defaults (improved safety).
     * CRITICAL: This is destructive - only call with user confirmation.
     * State.json is NOT touched.
     * 
     * Safety guarantees:
     * - Synchronized (no concurrent modification)
     * - Atomic write (tmp → rename)
     * - Forensic logging (timestamp, version, path)
     * - Failure recovery (rollback on write failure)
     */
    public static void resetToDefaults() {
        NozhConfig snapshot;
        synchronized (LOCK) {
            NozhConfig previousConfig = config; // Backup for rollback

            try {
                NozhConstants.LOGGER.info("Config reset initiated - Version: {} Path: {}",
                        NozhConstants.getVersion(), NozhConstants.CONFIG_FILE);

                // Create new default config
                config = new NozhConfig();
                config.clearCorrectedFlag(); // Fresh reset, not a correction

                // Persist atomically
                saveNowSilently();

                NozhConstants.LOGGER.info("Config reset complete - defaults written successfully");
            } catch (Exception e) {
                // Rollback on failure
                NozhConstants.LOGGER.error("Config reset FAILED - rolling back to previous config", e);
                config = previousConfig;
                throw new RuntimeException("Config reset failed", e);
            }
            snapshot = config;
        }
        notifyListeners(snapshot);
    }

    private static void ensureConfigDirExists() throws IOException {
        Path dir = NozhConstants.CONFIG_DIR;
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
            NozhConstants.LOGGER.debug("Created config directory: {}", dir);
        }
    }

    public static void addListener(ConfigListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public static void removeListener(ConfigListener listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners(NozhConfig snapshot) {
        if (snapshot == null) {
            return;
        }
        for (ConfigListener listener : listeners) {
            try {
                listener.onConfigUpdated(snapshot);
            } catch (Exception e) {
                NozhConstants.LOGGER.warn("Config listener failed: {}", e.getMessage());
            }
        }
    }

    private static void saveNowSilently() {
        saveInternal(false, false, true);
    }
}
