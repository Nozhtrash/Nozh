package dev.nozh.core.ui;

import dev.nozh.core.capability.CapabilityProvider;
import dev.nozh.core.capability.ProviderRegistry;
import dev.nozh.core.capability.ProviderStatus;
import dev.nozh.core.context.Scenario;
import dev.nozh.core.compatibility.CompatibilityMatrix;
import dev.nozh.core.governor.ActionOutcome;
import dev.nozh.core.issues.Issue;
import dev.nozh.core.issues.IssueSeverity;
import dev.nozh.core.preset.HardwareTier;
import dev.nozh.core.state.RuntimeState;
import dev.nozh.core.state.ActionHistoryEntry;
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

        CompatibilityMatrix compatibilityMatrix = null;
        try {
            compatibilityMatrix = new CompatibilityMatrix();
        } catch (Throwable e) {
            // Ignore compatibility initialization failures (tests/headless)
        }
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
                    provider.statusReason().orElse(""),
                    compatibilityMatrix != null ? compatibilityMatrix.getSteward(provider.id()) : "NOZH"));
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

        // DEFAULTS for removed RuntimeState fields
        String lastDecisionReason = "";
        long lastDecisionTimestamp = 0L;
        String directorSteward = "NOZH";
        String currentBound = "BALANCED";

        boolean benchmarkRunning = state.benchmarkRunning();
        String benchmarkValidity = state.benchmarkValidity();

        List<HudViewModel.ActionHistoryEntryView> recentActions = new ArrayList<>();
        if (state.actionHistory() != null) {
            for (ActionHistoryEntry entry : state.actionHistory()) {
                recentActions.add(new HudViewModel.ActionHistoryEntryView(
                        entry.timestampMillis(),
                        entry.actionSummary(),
                        formatOutcome(entry.outcome(), entry.rollbackApplied())));
            }
        }

        String lastActionSummary = "";
        String lastActionOutcome = "";
        if (!recentActions.isEmpty()) {
            HudViewModel.ActionHistoryEntryView last = recentActions.get(recentActions.size() - 1);
            lastActionSummary = last.actionSummary();
            lastActionOutcome = last.outcome();
        }
        Scenario scenario = state.currentScenario() != null ? state.currentScenario() : Scenario.STANDARD;
        String scenarioKey = scenario.translationKey();
        double scenarioConfidence = state.scenarioConfidence();

        return new HudViewModel(
                state.enabled(),
                dev.nozh.core.governor.GovernorMode.AUTO_CONSERVATIVE, // Default
                dev.nozh.core.issues.ParanoiaLevel.NORMAL, // Default
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
                directorSteward,
                lastActionSummary,
                lastActionOutcome,
                scenario,
                scenarioKey,
                scenarioConfidence,

                benchmarkRunning,
                benchmarkValidity,

                providerVMs,
                issues != null ? new ArrayList<>(issues) : List.of(),
                recentActions);
    }

    private static String formatOutcome(ActionOutcome outcome, boolean rollbackApplied) {
        if (outcome == null) {
            return "";
        }
        if (rollbackApplied) {
            return "ROLLBACK";
        }
        return outcome.name();
    }

    private HudViewModelBuilder() {
        // Static utility
    }
}
