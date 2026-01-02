package dev.nozh;

import net.fabricmc.api.ModInitializer;
import dev.nozh.core.config.ConfigManager;
import dev.nozh.core.safety.CrashLoopGuard;

/**
 * NOZH - Frametime Stability Optimizer
 * Main mod initializer (runs on both client and server, but most logic is
 * client-only)
 */
public class NozhMod implements ModInitializer {

    @Override
    public void onInitialize() {
        NozhConstants.LOGGER.info("NOZH {} initializing...", NozhConstants.getVersion());

        // Load configuration
        ConfigManager.load();

        // Initialize crash loop guard (increment boot attempts)
        CrashLoopGuard.onStartup();

        NozhConstants.LOGGER.info("NOZH initialized. Safe mode: {}", CrashLoopGuard.isInSafeMode());
    }
}
