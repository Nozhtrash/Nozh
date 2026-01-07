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
    public boolean showHudSuggestions = true;
    public String hudMode = "ANALYST"; // COMPACT, ANALYST, EXPERT
    public String hudAnchor = "TOP_LEFT"; // TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    public int hudOffsetX = 0;
    public int hudOffsetY = 0;
    public double hudScale = 1.0;
    public int tutorialStep = 0; // 0=welcome, 1=apply, 2=export, 3=complete

    // Version tracking for migrations
    public int configVersion = 0; // 0=v0.1.0, 1=legacy, 2=v0.2.0-alpha

    // Feature Toggles
    public boolean allowAutoTuning = false;
    public boolean allowGameplayImpactActions = false; // Phase 8: Level 2/3 actions
    public boolean safeModeForce = false;
    public boolean rollbackEnabled = true;
    public boolean hybridModelEnabled = true;

    // Tuning Parameters (Rollback)
    public int rollbackWindowMillis = 45000;
    public double improvementEpsilonAvgMs = 0.5;
    public double improvementEpsilonP95Ms = 1.0;
    public int rollbackEvaluationTicks = 100;
    public int rollbackCooldownMillis = 60000;
    public int observationWindowSeconds = 5;
    public double hybridModelBlockConfidence = 0.65;
    public int governorDecisionBudgetMs = 8;
    public double banditExplorationRate = 0.2;

    // Performance Targets
    public int targetFps = 60;
    public String optimizationProfile = "BALANCED";
    public double reverseEpsilonMs = 1.5;

    // Benchmark & Calibration
    public boolean benchmarkModeEnabled = false;
    public int benchmarkMicroIntervalMillis = 5000;
    public String hardwareProfile = "";

    // Adaptive Visual Quality
    public boolean adaptiveVisualQualityEnabled = true;
    public double adaptiveVisualQualitySensitivityMs = 1.5;
    public int adaptiveVisualQualityMinStep = 0;
    public int adaptiveVisualQualityMaxStep = 17;

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

        if (optimizationProfile == null
                || (!optimizationProfile.equalsIgnoreCase("BALANCED")
                        && !optimizationProfile.equalsIgnoreCase("AGGRESSIVE"))) {
            optimizationProfile = "BALANCED";
            corrected = true;
        }

        if (reverseEpsilonMs < 0.0 || reverseEpsilonMs > 8.0) {
            reverseEpsilonMs = clamp(reverseEpsilonMs, 0.0, 8.0);
            corrected = true;
        }

        if (benchmarkMicroIntervalMillis < 1000 || benchmarkMicroIntervalMillis > 30000) {
            benchmarkMicroIntervalMillis = clamp(benchmarkMicroIntervalMillis, 1000, 30000);
            corrected = true;
        }

        if (adaptiveVisualQualitySensitivityMs < 0.25 || adaptiveVisualQualitySensitivityMs > 8.0) {
            adaptiveVisualQualitySensitivityMs = clamp(adaptiveVisualQualitySensitivityMs, 0.25, 8.0);
            corrected = true;
        }

        if (adaptiveVisualQualityMinStep < 0 || adaptiveVisualQualityMinStep > 50) {
            adaptiveVisualQualityMinStep = clamp(adaptiveVisualQualityMinStep, 0, 50);
            corrected = true;
        }

        if (adaptiveVisualQualityMaxStep < 0 || adaptiveVisualQualityMaxStep > 50) {
            adaptiveVisualQualityMaxStep = clamp(adaptiveVisualQualityMaxStep, 0, 50);
            corrected = true;
        }

        if (adaptiveVisualQualityMinStep > adaptiveVisualQualityMaxStep) {
            adaptiveVisualQualityMaxStep = adaptiveVisualQualityMinStep;
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

        // rollbackEvaluationTicks: 20-600
        if (rollbackEvaluationTicks < 20 || rollbackEvaluationTicks > 600) {
            rollbackEvaluationTicks = clamp(rollbackEvaluationTicks, 20, 600);
            corrected = true;
        }

        // rollbackCooldownMillis: 10000-600000
        if (rollbackCooldownMillis < 10000 || rollbackCooldownMillis > 600000) {
            rollbackCooldownMillis = clamp(rollbackCooldownMillis, 10000, 600000);
            corrected = true;
        }

        // observationWindowSeconds: 3-10
        if (observationWindowSeconds < 3 || observationWindowSeconds > 10) {
            observationWindowSeconds = clamp(observationWindowSeconds, 3, 10);
            corrected = true;
        }

        if (hybridModelBlockConfidence < 0.0 || hybridModelBlockConfidence > 1.0) {
            hybridModelBlockConfidence = clamp(hybridModelBlockConfidence, 0.0, 1.0);
            corrected = true;
        }

        if (governorDecisionBudgetMs < 1 || governorDecisionBudgetMs > 50) {
            governorDecisionBudgetMs = clamp(governorDecisionBudgetMs, 1, 50);
            corrected = true;
        }

        if (banditExplorationRate < 0.0 || banditExplorationRate > 0.6) {
            banditExplorationRate = clamp(banditExplorationRate, 0.0, 0.6);
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

        if (!isValidHudMode(hudMode)) {
            hudMode = "ANALYST";
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

        double clampedHudScale = clamp(hudScale, 0.5, 2.0);
        if (Double.compare(clampedHudScale, hudScale) != 0) {
            hudScale = clampedHudScale;
            corrected = true;
        }

        int clampedTutorialStep = clamp(tutorialStep, 0, 3);
        if (clampedTutorialStep != tutorialStep) {
            tutorialStep = clampedTutorialStep;
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

    private boolean isValidHudMode(String mode) {
        if (mode == null) {
            return false;
        }
        return mode.equals("COMPACT")
                || mode.equals("ANALYST")
                || mode.equals("EXPERT");
    }
}
