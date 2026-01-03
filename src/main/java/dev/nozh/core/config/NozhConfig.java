package dev.nozh.core.config;

import dev.nozh.NozhConstants;

/**
 * NOZH configuration data (PROFESSIONAL SPEC).
 * Loaded from config/nozh/nozh.json.
 * 
 * Includes strict validation, clamps, and migration support.
 */
public class NozhConfig {

    // Core
    public boolean enabled = true;
    public boolean debugLogs = false;
    public String language = "auto"; // "auto", "en_us", "es_cl"
    public boolean showHud = true;
    public String hudAnchor = "TOP_LEFT"; // TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    public int hudOffsetX = 0;
    public int hudOffsetY = 0;

    // Version tracking for migrations
    public int configVersion = 0; // 0=v0.1.0, 1=legacy, 2=v0.2.0-alpha

    // Feature Toggles
    public boolean allowAutoTuning = false;
    public boolean allowGameplayImpactActions = false; // Phase 8: Level 2/3 actions
    public boolean safeModeForce = false;
    public boolean rollbackEnabled = true;

    // Tuning Parameters (Rollback)
    public int rollbackWindowMillis = 45000;
    public double improvementEpsilonAvgMs = 0.5;
    public double improvementEpsilonP95Ms = 1.0;

    // Performance Targets
    public int targetFps = 60;

    // Limits & Cooldowns
    public int historyMaxEntries = 50;
    public int historyCommandLimit = 10;
    public int cooldownActionMillis = 120000; // 2 min default
    public int cooldownGlobalMinIntervalMillis = 60000; // 1 min default
    public int maxChangesPerSession = 2; // Strict default
    public int evalPeriodTicks = 100;

    // Migration / Legacy compatibility (hidden/mapped)
    // We remove old fields. JSON parsing will ignore unknown keys in the file,
    // effectively "migrating" by discarding old keys and using defaults for new
    // ones
    // unless we write a custom deserializer. For 0.1.0 this is acceptable.

    // Transient state
    private transient boolean wasCorrected = false;

    public boolean wasCorrected() {
        return wasCorrected;
    }

    public void clearCorrectedFlag() {
        wasCorrected = false;
    }

    /**
     * Validate and clamp config values to safe ranges.
     * Returns true if corrections were made.
     */
    public boolean validate() {
        boolean corrected = false;

        // targetFps: 30-240
        if (targetFps < 30 || targetFps > 240) {
            targetFps = clamp(targetFps, 30, 240);
            corrected = true;
        }

        // rollbackWindowMillis: 10000-180000
        if (rollbackWindowMillis < 10000 || rollbackWindowMillis > 180000) {
            rollbackWindowMillis = clamp(rollbackWindowMillis, 10000, 180000);
            corrected = true;
        }

        // improvementEpsilonAvgMs: 0.0-5.0
        if (improvementEpsilonAvgMs < 0.0 || improvementEpsilonAvgMs > 5.0) {
            improvementEpsilonAvgMs = clamp(improvementEpsilonAvgMs, 0.0, 5.0);
            corrected = true;
        }

        // improvementEpsilonP95Ms: 0.0-5.0
        if (improvementEpsilonP95Ms < 0.0 || improvementEpsilonP95Ms > 5.0) {
            improvementEpsilonP95Ms = clamp(improvementEpsilonP95Ms, 0.0, 5.0);
            corrected = true;
        }

        // historyMaxEntries: 10-500
        if (historyMaxEntries < 10 || historyMaxEntries > 500) {
            historyMaxEntries = clamp(historyMaxEntries, 10, 500);
            corrected = true;
        }

        // historyCommandLimit: 1-50
        if (historyCommandLimit < 1 || historyCommandLimit > 50) {
            historyCommandLimit = clamp(historyCommandLimit, 1, 50);
            corrected = true;
        }

        // cooldownActionMillis: 30000-600000
        if (cooldownActionMillis < 30000 || cooldownActionMillis > 600000) {
            cooldownActionMillis = clamp(cooldownActionMillis, 30000, 600000);
            corrected = true;
        }

        // cooldownGlobalMinIntervalMillis: 10000-300000
        if (cooldownGlobalMinIntervalMillis < 10000 || cooldownGlobalMinIntervalMillis > 300000) {
            cooldownGlobalMinIntervalMillis = clamp(cooldownGlobalMinIntervalMillis, 10000, 300000);
            corrected = true;
        }

        // maxChangesPerSession: 0-10
        if (maxChangesPerSession < 0 || maxChangesPerSession > 10) {
            maxChangesPerSession = clamp(maxChangesPerSession, 0, 10);
            corrected = true;
        }

        // evalPeriodTicks: 20-600
        if (evalPeriodTicks < 20 || evalPeriodTicks > 600) {
            evalPeriodTicks = clamp(evalPeriodTicks, 20, 600);
            corrected = true;
        }

        if (!isValidHudAnchor(hudAnchor)) {
            hudAnchor = "TOP_LEFT";
            corrected = true;
        }

        int clampedHudOffsetX = clamp(hudOffsetX, -200, 200);
        if (clampedHudOffsetX != hudOffsetX) {
            hudOffsetX = clampedHudOffsetX;
            corrected = true;
        }

        int clampedHudOffsetY = clamp(hudOffsetY, -200, 200);
        if (clampedHudOffsetY != hudOffsetY) {
            hudOffsetY = clampedHudOffsetY;
            corrected = true;
        }

        if (corrected) {
            wasCorrected = true;
            NozhConstants.LOGGER.warn("Config had invalid values and was auto-corrected/clamped.");
        }

        return corrected;
    }

    private int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    private double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    private boolean isValidHudAnchor(String anchor) {
        if (anchor == null) {
            return false;
        }
        return anchor.equals("TOP_LEFT")
                || anchor.equals("TOP_RIGHT")
                || anchor.equals("BOTTOM_LEFT")
                || anchor.equals("BOTTOM_RIGHT");
    }
}
