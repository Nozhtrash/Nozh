package dev.nozh.core.ui;

import dev.nozh.core.capability.CapabilityProvider;
import dev.nozh.core.capability.ProviderRegistry;
import dev.nozh.core.capability.ProviderStatus;
import dev.nozh.core.governor.GovernorMode;
import dev.nozh.core.issues.Issue;
import dev.nozh.core.issues.IssueSeverity;
import dev.nozh.core.issues.ParanoiaLevel;
import dev.nozh.core.preset.HardwareTier;
import dev.nozh.core.state.RuntimeState;
import dev.nozh.core.telemetry.TelemetrySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD View Model Builder (Contract 7).
 * 
 * Pure transformer: RuntimeState + Telemetry + Issues + Preset → HudViewModel
 * 
 * NO decision logic, NO Optional, NO streams.
 * PURE - deterministic transformation only.
 */
public final class HudViewModelBuilder {

    /**
     * Build view model from current system state.
     * 
     * @param state     Runtime state
     * @param telemetry Telemetry snapshot
     * @param issues    Active issues
     * @param tier      Current hardware tier
     * @param registry  Provider registry for health check
     * @return View model ready for rendering
     */
    public static HudViewModel build(
            RuntimeState state,
            TelemetrySnapshot telemetry,
            List<Issue> issues,
            HardwareTier tier,
            ProviderRegistry registry) {
        if (state == null || telemetry == null) {
            return HudViewModel.EMPTY;
        }

        // Calculate uptime
        long now = System.currentTimeMillis();
        long uptimeSeconds = (now - state.sessionStartTime()) / 1000;

        // Provider health summary
        List<CapabilityProvider> allProviders = registry != null
                ? new ArrayList<>(registry.getAllProviders())
                : List.of();
        int total = allProviders.size();
        int healthy = 0;
        int degraded = 0;
        int broken = 0;

        List<HudViewModel.ProviderViewModel> providerVMs = new ArrayList<>();
        for (CapabilityProvider provider : allProviders) {
            ProviderStatus status = provider.status();
            if (status == ProviderStatus.HEALTHY)
                healthy++;
            else if (status == ProviderStatus.DEGRADED)
                degraded++;
            else if (status == ProviderStatus.BROKEN)
                broken++;

            providerVMs.add(new HudViewModel.ProviderViewModel(
                    provider.id().toString(),
                    status,
                    provider.statusReason().orElse("")));
        }

        // Issues summary
        int issuesTotal = issues != null ? issues.size() : 0;
        int critical = 0;
        int warning = 0;

        if (issues != null) {
            for (Issue issue : issues) {
                if (issue.severity() == IssueSeverity.CRITICAL)
                    critical++;
                else if (issue.severity() == IssueSeverity.WARNING)
                    warning++;
            }
        }

        // TODO: Get from RuntimeState when DecisionReport is integrated
        String lastDecisionReason = "";
        long lastDecisionTimestamp = 0;

        // TODO: Get from RuntimeState when Benchmark is integrated
        boolean benchmarkRunning = false;
        String benchmarkValidity = "NONE";

        // TODO: Get from RuntimeState execution history
        List<String> historyEntries = List.of();

        // TODO: Get actual bound from state (for now, placeholder)
        String currentBound = "BALANCED";

        // TODO: Get governor mode from state (for now, default)
        GovernorMode governorMode = GovernorMode.AUTO_CONSERVATIVE;

        // TODO: Get paranoia level from state (for now, default)
        ParanoiaLevel paranoiaLevel = ParanoiaLevel.NORMAL;

        return new HudViewModel(
                true, // systemEnabled (TODO: from config)
                governorMode,
                paranoiaLevel,
                tier != null ? tier : HardwareTier.MEDIUM,
                uptimeSeconds,
                currentBound,

                telemetry.avgFrametimeMs(),
                telemetry.p95FrametimeMs(),
                telemetry.spikeCount(),
                telemetry.sampleCount(),
                telemetry.droppedSamples(),
                telemetry.sufficientData(),

                total,
                healthy,
                degraded,
                broken,

                issuesTotal,
                critical,
                warning,

                lastDecisionReason,
                lastDecisionTimestamp,

                benchmarkRunning,
                benchmarkValidity,

                providerVMs,
                issues != null ? new ArrayList<>(issues) : List.of(),
                historyEntries);
    }

    private HudViewModelBuilder() {
        // Static utility
    }
}
