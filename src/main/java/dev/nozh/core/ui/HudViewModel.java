package dev.nozh.core.ui;

import dev.nozh.core.capability.ProviderStatus;
import dev.nozh.core.issues.Issue;
import dev.nozh.core.governor.GovernorMode;
import dev.nozh.core.issues.ParanoiaLevel;
import dev.nozh.core.preset.HardwareTier;

import java.util.List;

/**
 * HUD View Model (Contract 7).
 * 
 * Pure DTO - NO Optional, NO streams, NO nested complex objects.
 * Input: RuntimeState + TelemetrySnapshot + Issues + Preset
 * Output: Flat DTOs for rendering.
 * 
 * PURE - zero MC dependencies.
 */
public record HudViewModel(
        // Dashboard data
        boolean systemEnabled,
        GovernorMode governorMode,
        ParanoiaLevel paranoiaLevel,
        HardwareTier activeTier,
        long uptimeSeconds,
        String currentBound, // "CPU_BOUND" / "GPU_BOUND" / "BALANCED" / "UNKNOWN"

        // Performance metrics
        double avgFrametimeMs,
        double p95FrametimeMs,
        int spikeCount,
        int sampleCount,
        int droppedSamples,
        boolean sufficientData,

        // Provider summary
        int providersTotal,
        int providersHealthy,
        int providersDegraded,
        int providersBroken,

        // Issues summary
        int issuesTotal,
        int issuesCritical,
        int issuesWarning,

        // Governor decision (last)
        String lastDecisionReason, // i18n key, or empty string if no decision
        long lastDecisionTimestamp,

        // Benchmark status
        boolean benchmarkRunning,
        String benchmarkValidity, // "VALID" / "NOISY" / "INCONCLUSIVE" / "NONE"

        // Detailed data (for respective sections)
        List<ProviderViewModel> providers,
        List<Issue> issues,
        List<String> historyEntries // String summaries (cheap)
) {
    /**
     * Provider view model (nested DTO).
     */
    public record ProviderViewModel(
            String capabilityId,
            ProviderStatus status,
            String statusReason, // Empty string if no reason
            String steward
    ) {
    }

    /**
     * Empty view model for when no data is available.
     */
    public static HudViewModel EMPTY = new HudViewModel(
            false,
            GovernorMode.OFF,
            ParanoiaLevel.OFF,
            HardwareTier.MEDIUM,
            0,
            "UNKNOWN",
            0, 0, 0, 0, 0, false,
            0, 0, 0, 0,
            0, 0, 0,
            "",
            0,
            false,
            "NONE",
            List.of(),
            List.of(),
            List.of());
}
