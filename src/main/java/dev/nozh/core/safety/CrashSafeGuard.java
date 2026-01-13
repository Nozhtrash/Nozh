package dev.nozh.core.safety;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Intelligent guard against boot loops.
 * <p>
 * If the game crashes during startup 3 times in a row, this system
 * will automatically trigger "Safe Mode" on the next launch.
 */
public class CrashSafeGuard {
    private static final String MARKER_FILE_NAME = ".nozh_booting";
    private static final String CRASH_COUNT_FILE = ".nozh_crash_count";
    private static final int MAX_CRASHES_BEFORE_SAFE_MODE = 3;

    private final Path configDir;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public CrashSafeGuard(Path configDir) {
        this.configDir = configDir;
    }

    /**
     * Call this as early as possible during mod initialization.
     * @return true if Safe Mode should be enforced.
     */
    public boolean onStartup() {
        Path markerFile = configDir.resolve(MARKER_FILE_NAME);
        Path crashCountFile = configDir.resolve(CRASH_COUNT_FILE);

        int crashCount = 0;

        try {
            // 1. Check if previous boot finished successfully
            if (Files.exists(markerFile)) {
                // Previous boot didn't delete the marker -> It crashed!
                crashCount = readCrashCount(crashCountFile) + 1;
                writeCrashCount(crashCountFile, crashCount);
                
                dev.nozh.NozhConstants.LOGGER.warn("[NOZH] Detected incomplete previous boot. Crash count: " + crashCount);
            } else {
                // Clean boot last time (or first run), reset counter
                Files.deleteIfExists(crashCountFile);
                Files.createFile(markerFile);
            }
        } catch (IOException e) {
            dev.nozh.NozhConstants.LOGGER.error("Failed to check crash marker", e);
        }

        // 2. Schedule success marker (30 seconds uptime = success)
        scheduler.schedule(() -> markBootSuccessful(markerFile, crashCountFile), 30, TimeUnit.SECONDS);

        // 3. Decision time
        if (crashCount >= MAX_CRASHES_BEFORE_SAFE_MODE) {
            dev.nozh.NozhConstants.LOGGER.error("[NOZH] ⚠️ MAX CRASH LIMIT REACHED. ENFORCING SAFE MODE.");
            return true;
        }
        
        return false;
    }

    private void markBootSuccessful(Path markerFile, Path crashCountFile) {
        try {
            Files.deleteIfExists(markerFile);
            Files.deleteIfExists(crashCountFile);
            dev.nozh.NozhConstants.LOGGER.info("[NOZH] Boot considered successful. Crash counters reset.");
        } catch (IOException e) {
            dev.nozh.NozhConstants.LOGGER.error("Failed to cleanup crash guard file", e);
        } finally {
            scheduler.shutdown();
        }
    }

    private int readCrashCount(Path file) {
        if (!Files.exists(file)) return 0;
        try {
            String content = Files.readString(file).trim();
            return Integer.parseInt(content);
        } catch (Exception e) {
            return 0;
        }
    }

    private void writeCrashCount(Path file, int count) {
        try {
            Files.writeString(file, String.valueOf(count), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            dev.nozh.NozhConstants.LOGGER.error("Failed to write crash count", e);
        }
    }
}
