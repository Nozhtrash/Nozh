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
    public boolean enabled = true; // Master switch: enables/disables the entire governor runtime.
    public boolean debugLogs = false; // Verbose logs for diagnosis; increases log volume.
    public String language = "auto"; // UI language override: "auto", "en_us", "es_cl".
    public boolean showHud = true; // Controls HUD visibility in-game.
    public boolean showHudSuggestions = true; // Shows action suggestions in HUD for guidance.
    public String hudMode = "ANALYST"; // HUD density preset: COMPACT, ANALYST, EXPERT.
    public String hudAnchor = "TOP_LEFT"; // HUD anchor position on screen edges.
    public int hudOffsetX = 0; // Horizontal offset (pixels) from anchor.
    public int hudOffsetY = 0; // Vertical offset (pixels) from anchor.
    public double hudScale = 1.0; // HUD scale factor for readability (1.0 = 100%).
    public int tutorialStep = 0; // Onboarding step (0=welcome, 1=apply, 2=export, 3=complete).

    // Version tracking for migrations
    public int configVersion = 0; // Migration tracking (0=v0.1.0, 1=legacy, 2=v0.2.0-alpha).

    // Feature Toggles
    public boolean allowAutoTuning = false; // Allows automatic actions without manual confirmation.
    public boolean allowGameplayImpactActions = false; // Enables higher-impact actions (Phase 8 L2/L3).
    public boolean safeModeForce = false; // Forces safe mode when compatibility risk is detected.
    public boolean rollbackEnabled = true; // Enables rollback when telemetry worsens after changes.
    public boolean hybridModelEnabled = true; // Enables hybrid decision logic for stability.

    // Tuning Parameters (Rollback)
    public int rollbackWindowMillis = 45000; // Lookback window for rollback evaluation (ms).
    public double improvementEpsilonAvgMs = 0.5; // Avg frametime delta required to call it "better".
    public double improvementEpsilonP95Ms = 1.0; // p95 frametime delta required to call it "better".
    public int rollbackEvaluationTicks = 100; // Sample count before concluding rollback is needed.
    public int rollbackCooldownMillis = 60000; // Cooldown between rollbacks to avoid oscillation.
    public int observationWindowSeconds = 5; // Telemetry window; 5s ~ 300 samples @ 60 FPS.
    public double hybridModelBlockConfidence = 0.65; // Confidence to block risky actions in hybrid mode.
    public int governorDecisionBudgetMs = 8; // Max CPU budget per decision tick (ms).

    // Performance Targets
    public int targetFps = 60; // Target FPS aligned with typical 60Hz panels (adjust to 120/144/240).
    public String optimizationProfile = "BALANCED"; // Overall aggressiveness of tuning.
    public double reverseEpsilonMs = 1.5; // Delta before reversing changes (ms).

    // Benchmark & Calibration
    public boolean benchmarkModeEnabled = false; // Enables synthetic benchmarking for calibration.
    public int benchmarkMicroIntervalMillis = 5000; // Granularity of benchmark telemetry snapshots.
    public String hardwareProfile = ""; // Persisted HW profile hash for telemetry baselines.

    // Adaptive Visual Quality
    public boolean adaptiveVisualQualityEnabled = true; // Allows adaptive graphics tradeoffs.
    public double adaptiveVisualQualitySensitivityMs = 1.5; // Sensitivity to frametime spikes (ms).
    public int adaptiveVisualQualityMinStep = 0; // Lower bound on adaptive quality steps.
    public int adaptiveVisualQualityMaxStep = 17; // Upper bound on adaptive quality steps.

    // Limits & Cooldowns
    public int historyMaxEntries = 50; // Max telemetry/action history entries stored in-memory.
    public int historyCommandLimit = 10; // Max entries shown per history command request.
    public int cooldownActionMillis = 120000; // Per-action cooldown to avoid repeating tweaks too fast.
    public int cooldownGlobalMinIntervalMillis = 60000; // Global minimum interval between any actions.
    public int maxChangesPerSession = 2; // Max applied changes per session for stability.
    public int evalPeriodTicks = 100; // Ticks between evaluation cycles.

    // Migration / Legacy compatibility (hidden/mapped)
    // We remove old fields. JSON parsing will ignore unknown keys in the file,
    // effectively "migrating" by discarding old keys and using defaults for new
    // ones
    // unless we write a custom deserializer. For 0.1.0 this is acceptable.

    // Transient state
    private transient boolean wasCorrected = false;
    private transient java.util.List<String> criticalCorrections = new java.util.ArrayList<>();

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

        if (language == null || language.isBlank()) {
            language = "auto";
            corrected = true;
        }

        // targetFps: 30-240
        if (targetFps < 30 || targetFps > 240) {
            targetFps = clamp(targetFps, 30, 240);
            corrected = true;
            addCriticalCorrection("targetFps clamped to " + targetFps + " (30-240)");
        }

        if (optimizationProfile == null
                || (!optimizationProfile.equalsIgnoreCase("BALANCED")
                        && !optimizationProfile.equalsIgnoreCase("AGGRESSIVE"))) {
            optimizationProfile = "BALANCED";
            corrected = true;
        }

        if (!isFinite(reverseEpsilonMs)) {
            reverseEpsilonMs = 1.5;
            corrected = true;
        } else if (reverseEpsilonMs < 0.0 || reverseEpsilonMs > 8.0) {
            reverseEpsilonMs = clamp(reverseEpsilonMs, 0.0, 8.0);
            corrected = true;
        }

        if (benchmarkMicroIntervalMillis < 1000 || benchmarkMicroIntervalMillis > 30000) {
            benchmarkMicroIntervalMillis = clamp(benchmarkMicroIntervalMillis, 1000, 30000);
            corrected = true;
        }

        if (!isFinite(adaptiveVisualQualitySensitivityMs)) {
            adaptiveVisualQualitySensitivityMs = 1.5;
            corrected = true;
        } else if (adaptiveVisualQualitySensitivityMs < 0.25 || adaptiveVisualQualitySensitivityMs > 8.0) {
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
        if (!isFinite(improvementEpsilonAvgMs)) {
            improvementEpsilonAvgMs = 0.5;
            corrected = true;
        } else if (improvementEpsilonAvgMs < 0.0 || improvementEpsilonAvgMs > 5.0) {
            improvementEpsilonAvgMs = clamp(improvementEpsilonAvgMs, 0.0, 5.0);
            corrected = true;
        }

        // improvementEpsilonP95Ms: 0.0-5.0
        if (!isFinite(improvementEpsilonP95Ms)) {
            improvementEpsilonP95Ms = 1.0;
            corrected = true;
        } else if (improvementEpsilonP95Ms < 0.0 || improvementEpsilonP95Ms > 5.0) {
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
            addCriticalCorrection("observationWindowSeconds clamped to " + observationWindowSeconds + " (3-10)");
        }

        if (!isFinite(hybridModelBlockConfidence)) {
            hybridModelBlockConfidence = 0.65;
            corrected = true;
        } else if (hybridModelBlockConfidence < 0.0 || hybridModelBlockConfidence > 1.0) {
            hybridModelBlockConfidence = clamp(hybridModelBlockConfidence, 0.0, 1.0);
            corrected = true;
        }

        if (governorDecisionBudgetMs < 1 || governorDecisionBudgetMs > 50) {
            governorDecisionBudgetMs = clamp(governorDecisionBudgetMs, 1, 50);
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
            addCriticalCorrection("cooldownActionMillis clamped to " + cooldownActionMillis + " (30000-600000)");
        }

        // cooldownGlobalMinIntervalMillis: 10000-300000
        if (cooldownGlobalMinIntervalMillis < 10000 || cooldownGlobalMinIntervalMillis > 300000) {
            cooldownGlobalMinIntervalMillis = clamp(cooldownGlobalMinIntervalMillis, 10000, 300000);
            corrected = true;
            addCriticalCorrection("cooldownGlobalMinIntervalMillis clamped to "
                    + cooldownGlobalMinIntervalMillis + " (10000-300000)");
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

        if (!isFinite(hudScale)) {
            hudScale = 1.0;
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

    private boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    private void addCriticalCorrection(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        criticalCorrections.add(message);
    }

    public java.util.List<String> consumeCriticalCorrections() {
        if (criticalCorrections.isEmpty()) {
            return java.util.List.of();
        }
        java.util.List<String> snapshot = new java.util.ArrayList<>(criticalCorrections);
        criticalCorrections.clear();
        return snapshot;
    }
}
