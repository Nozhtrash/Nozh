package dev.nozh;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Shared constants for NOZH mod
 */
public final class NozhConstants {

    public static final String MOD_ID = "nozh";
    public static final String MOD_NAME = "NOZH";

    // Dynamic version from mod metadata (not hardcoded)
    private static String cachedVersion = null;

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    /**
     * Get mod version dynamically from Fabric metadata.
     * Cached for performance.
     */
    public static String getVersion() {
        if (cachedVersion == null) {
            cachedVersion = FabricLoader.getInstance()
                    .getModContainer(MOD_ID)
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse("UNKNOWN");
        }
        return cachedVersion;
    }

    // Config paths
    public static final Path CONFIG_DIR;
    public static final Path CONFIG_FILE;
    public static final Path STATE_FILE;
    public static final Path STATE_TMP_FILE;
    public static final Path MODEL_DIR;
    public static final Path MODEL_FILE;

    static {
        Path configDir;
        try {
            configDir = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
        } catch (Throwable ignored) {
            configDir = Path.of("config").resolve(MOD_ID);
        }
        CONFIG_DIR = configDir;
        CONFIG_FILE = CONFIG_DIR.resolve("nozh.json");
        STATE_FILE = CONFIG_DIR.resolve("state.json");
        STATE_TMP_FILE = CONFIG_DIR.resolve("state.tmp");
        MODEL_DIR = CONFIG_DIR.resolve("models");
        MODEL_FILE = MODEL_DIR.resolve("hybrid_model_v1.json");
    }

    // Crash loop guard thresholds
    public static final int MAX_BOOT_ATTEMPTS_BEFORE_SAFE_MODE = 3;
    public static final int TICKS_BEFORE_STABLE = 200; // ~10 seconds

    // Profiler defaults
    public static final int DEFAULT_RING_BUFFER_SIZE = 300; // ~5 seconds at 60fps
    public static final int STATS_UPDATE_INTERVAL_FRAMES = 30; // Update stats every 30 frames

    private NozhConstants() {
        // Utility class
    }
}
