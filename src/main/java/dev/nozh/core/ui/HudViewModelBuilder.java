package dev.nozh.core.ui;

import dev.nozh.core.capability.CapabilityProvider;
import dev.nozh.core.capability.ProviderCoverage;
import dev.nozh.core.capability.ProviderRegistry;
import dev.nozh.core.capability.ProviderStatus;
import dev.nozh.core.context.Scenario;
import dev.nozh.core.compatibility.CompatibilityMatrix;
import dev.nozh.api.compat.StewardshipMode;
import dev.nozh.core.governor.ActionOutcome;
import dev.nozh.core.issues.Issue;
import dev.nozh.core.issues.IssueSeverity;
import dev.nozh.core.preset.HardwareTier;
import dev.nozh.core.profiler.PerfDiagnosticsSnapshot;
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
            PerfDiagnosticsSnapshot diagnostics,
            List<Issue> issues,
            HardwareTier tier,
            ProviderRegistry registry) {
        if (state == null || telemetry == null || diagnostics == null) {
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
        ProviderCoverage coverage = registry != null ? registry.coverage() : ProviderCoverage.of(0, 0);

        CompatibilityMatrix compatibilityMatrix = null;
        try {
            compatibilityMatrix = new CompatibilityMatrix();
        } catch (Exception e) {
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
        String currentBound = resolveBoundKey(state);

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

        List<HudViewModel.DirectorTrace> stewardshipTraces = new ArrayList<>();
        if (compatibilityMatrix != null) {
            for (var decision : compatibilityMatrix.getStewardshipTraces()) {
                StewardshipMode mode = decision.mode();
                if (mode == null || mode == StewardshipMode.NONE) {
                    continue;
                }
                String modeKey = "nozh.hud.director.mode." + mode.name().toLowerCase();
                String reason = decision.reason() == null ? "" : decision.reason();
                stewardshipTraces.add(new HudViewModel.DirectorTrace(
                        decision.capability().name(),
                        decision.steward(),
                        modeKey,
                        reason));
            }
        }

        return new HudViewModel(
                state.enabled(),
                dev.nozh.core.governor.GovernorMode.AUTO_CONSERVATIVE, // Default
                dev.nozh.core.issues.ParanoiaLevel.NORMAL, // Default
                tier != null ? tier : HardwareTier.MEDIUM,
                uptimeSeconds,
                currentBound,

                telemetry.avgFrametimeMs(),
                telemetry.p95FrametimeMs(),
                telemetry.p99FrametimeMs(),
                telemetry.frametimeVariance(),
                telemetry.spikeCount(),
                telemetry.sampleCount(),
                telemetry.droppedSamples(),
                telemetry.sufficientData(),

                diagnostics.recentGcMs(),
                diagnostics.gcPressureScore(),
                diagnostics.pauseCount(),
                diagnostics.pauseMaxMs(),
                diagnostics.stutterCauseKey(),
                diagnostics.stutterDetail(),
                diagnostics.stutterConfidence(),
                diagnostics.hottestRenderPhaseKey(),
                diagnostics.hottestRenderPhaseMs(),
                diagnostics.hottestRenderPhaseTicks(),

                total,
                healthy,
                degraded,
                broken,
                coverage.coveragePercent(),
                coverage.controlledCapabilities(),
                coverage.totalCapabilities(),

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

                stewardshipTraces,
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

    private static String resolveBoundKey(RuntimeState state) {
        if (state == null) {
            return "nozh.hud.bound.unknown";
        }
        double avgMs = state.avgFrametimeMs();
        double p95Ms = state.p95FrametimeMs();
        double tickAvgMs = state.tickTimeAvg();
        double tickP95Ms = state.tickTimeP95();

        boolean frameAvailable = avgMs >= 0 || p95Ms >= 0;
        boolean tickAvailable = tickAvgMs >= 0 || tickP95Ms >= 0;

        if (!frameAvailable && !tickAvailable) {
            return "nozh.hud.bound.unknown";
        }

        double frameMs = avgMs >= 0 ? avgMs : p95Ms;
        double tickMs = tickAvgMs >= 0 ? tickAvgMs : tickP95Ms;

        boolean frameHigh = frameAvailable && frameMs > 16.67;
        boolean tickHigh = tickAvailable && tickMs > 50.0;

        if (frameHigh && tickHigh) {
            return "nozh.hud.bound.mixed";
        }
        if (tickHigh) {
            return "nozh.hud.bound.cpu";
        }
        if (frameHigh) {
            return "nozh.hud.bound.gpu";
        }
        return "nozh.hud.bound.balanced";
    }

    private HudViewModelBuilder() {
        // Static utility
    }
}
