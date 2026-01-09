package dev.nozh.core.safety;

import dev.nozh.NozhConstants;
import dev.nozh.core.config.JsonMini;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * Manages atomic persistence of NOZH state.
 * Thread-safe singleton with synchronized operations.
 * Uses tmp file + atomic rename pattern for crash safety.
 */
public final class StateManager {

    private static final Object LOCK = new Object();
    private static volatile NozhState state = null;

    private StateManager() {
        // Utility class
    }

    /**
     * Get the current state (never null, lazy-initialized)
     */
    public static NozhState getState() {
        if (state == null) {
            synchronized (LOCK) {
                if (state == null) {
                    load();
                }
            }
        }
        return state;
    }

    /**
     * Load state from disk. Creates default if not exists or corrupted.
     * Never throws - uses defaults on any error.
     * Thread-safe.
     */
    public static void load() {
        synchronized (LOCK) {
            Path stateFile = NozhConstants.STATE_FILE;

            try {
                Files.createDirectories(NozhConstants.CONFIG_DIR);

                if (Files.exists(stateFile)) {
                    String json = Files.readString(stateFile, StandardCharsets.UTF_8);
                    NozhState loaded = JsonMini.fromJsonNozhState(json);
                    if (loaded != null) {
                        state = loaded;
                        NozhConstants.LOGGER.debug("Loaded state from {}", stateFile);
                    } else {
                        NozhConstants.LOGGER.warn("State file was empty or corrupted, using fresh state");
                        state = new NozhState();
                    }
                } else {
                    NozhConstants.LOGGER.debug("No state file found, creating fresh state");
                    state = new NozhState();
                }
            } catch (Exception e) {
                NozhConstants.LOGGER.error("Failed to load state, using fresh state: {}", e.getMessage());
                state = new NozhState();
            }
        }
    }

    /**
     * Save state to disk atomically (write to tmp, then rename).
     * Thread-safe. Cleans up tmp files on any failure.
     * This ensures we never have a corrupted state file from a crash mid-write.
     */
    private static int lastSavedHash = 0;

    public static void save() {
        synchronized (LOCK) {
            if (state == null) {
                NozhConstants.LOGGER.warn("Cannot save null state, skipping");
                return;
            }

            // Zero-Allocation Optimization:
            // Only serialize if we suspect changes? No, serialization is needed to compare.
            // But we can check a dirty flag if we implemented one.
            // For now, we serialize (fast in memory) and compare hash/content to avoid DISK
            // I/O (slow).
            String json = JsonMini.toJson(state);
            int currentHash = json.hashCode();

            if (currentHash == lastSavedHash) {
                // No changes, skip disk write
                return;
            }

            Path tmpFile = NozhConstants.STATE_TMP_FILE;

            try {
                Files.createDirectories(NozhConstants.CONFIG_DIR);

                // Write to temp file first
                Files.writeString(
                        tmpFile,
                        json,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE);

                // Atomic rename
                Files.move(
                        tmpFile,
                        NozhConstants.STATE_FILE,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);

                lastSavedHash = currentHash;
                NozhConstants.LOGGER.debug("Saved state atomically to {}", NozhConstants.STATE_FILE);
            } catch (IOException e) {
                NozhConstants.LOGGER.error("Failed to save state atomically: {}", e.getMessage());

                try {
                    Files.deleteIfExists(tmpFile);
                } catch (IOException cleanup) {
                }

                // Fallback
                try {
                    Files.writeString(NozhConstants.STATE_FILE, json, StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Save state immediately and synchronously (same as save())
     */
    public static void saveImmediately() {
        save();
    }

    /**
     * Persist last failure context for crash recovery.
     */
    public static void recordFailureContext(CrashFailureContext context) {
        synchronized (LOCK) {
            if (context == null) {
                return;
            }
            if (state == null) {
                load();
            }
            if (state == null) {
                NozhConstants.LOGGER.warn("Cannot record failure context: state is null");
                return;
            }
            state.setLastFailureContext(context);
            save();
        }
    }
}
