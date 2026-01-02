package dev.nozh.core.config;

import dev.nozh.NozhConstants;

/**
 * Professional config migration system.
 * 
 * Handles version upgrades gracefully, preserving user preferences
 * while applying necessary changes for new versions.
 */
public final class ConfigMigration {

    private static final int CURRENT_VERSION = 2; // v0.2.0-alpha

    /**
     * Migrate config to current version if needed.
     * 
     * @param loaded Config loaded from file
     * @return Migrated config (may be same object if already current)
     */
    public static NozhConfig migrate(NozhConfig loaded) {
        int version = loaded.configVersion;

        if (version >= CURRENT_VERSION) {
            return loaded; // Already current, no migration needed
        }

        NozhConstants.LOGGER.warn("Migrating NOZH config from v{} to v{}", version, CURRENT_VERSION);

        // Apply migrations sequentially
        if (version < 2) {
            loaded = migrateV1ToV2(loaded);
        }

        // Future: if (version < 3) loaded = migrateV2ToV3(loaded);

        loaded.configVersion = CURRENT_VERSION;
        NozhConstants.LOGGER.info("Config migration complete");

        return loaded;
    }

    /**
     * Migrate from v0.1.0 (version 0 or 1) to v0.2.0-alpha (version 2).
     * 
     * Critical changes:
     * - Force enabled=true (mod was often disabled in v0.1.0)
     * - Force allowAutoTuning=true (required for governor to work)
     * - Preserve user preferences where possible
     */
    private static NozhConfig migrateV1ToV2(NozhConfig old) {
        // CRITICAL: Enable mod for v0.2.0 (governor requires this)
        boolean wasDisabled = !old.enabled;
        old.enabled = true;
        old.allowAutoTuning = true;

        // Preserve user preferences
        old.targetFps = old.targetFps > 0 ? old.targetFps : 60;
        old.debugLogs = old.debugLogs; // Keep user choice

        // Set safe defaults for new fields
        if (old.rollbackWindowMillis <= 0) {
            old.rollbackWindowMillis = 45000; // 45s default
        }

        if (old.cooldownActionMillis <= 0) {
            old.cooldownActionMillis = 30000; // 30s default
        }

        // Log migration details
        if (wasDisabled) {
            NozhConstants.LOGGER.warn("Config migration: enabled={} → enabled=true (required for v0.2.0)", false);
        }
        NozhConstants.LOGGER.warn("Config migration: allowAutoTuning=true (required for governor)");

        return old;
    }

    private ConfigMigration() {
        // Utility class
    }
}
