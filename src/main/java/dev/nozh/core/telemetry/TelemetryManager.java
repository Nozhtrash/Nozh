package dev.nozh.core.telemetry;

import dev.nozh.api.PerfSnapshot;
import dev.nozh.api.Scenario;
import dev.nozh.core.capability.CapabilityId;
import dev.nozh.core.governor.ActionOutcome;
import dev.nozh.core.safety.CrashFailureContext;
import dev.nozh.core.safety.CrashRecoveryAction;
import dev.nozh.core.safety.CrashRecoveryDecision;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Central orchestrator for all telemetry systems.
 * Coordinates aggregation, tracking, and reporting.
 */
public final class TelemetryManager {
    private final TelemetryAggregator aggregator;
    private final DecisionTracker decisionTracker;
    private final CapabilityEffectivenessTracker effectivenessTracker;
    private final EventTimeline timeline;
    private final TelemetryDashboard dashboard;
    private final TelemetryReportGenerator reportGenerator;
    private dev.nozh.core.profiler.CriticalEventSink criticalEventSink;
    
    public TelemetryManager() {
        this.aggregator = new TelemetryAggregator();
        this.decisionTracker = new DecisionTracker();
        this.effectivenessTracker = new CapabilityEffectivenessTracker();
        this.timeline = new EventTimeline();
        this.dashboard = new TelemetryDashboard(
            aggregator,
            decisionTracker,
            effectivenessTracker,
            timeline
        );
        this.reportGenerator = new TelemetryReportGenerator(dashboard);
    }
    
    public void recordPerformance(PerfSnapshot snapshot, Scenario scenario) {
        aggregator.recordSnapshot(snapshot, scenario);
    }
    
    public void recordDecision(
        Scenario scenario,
        CapabilityId capability,
        dev.nozh.core.capability.CapabilityValue oldValue,
        dev.nozh.core.capability.CapabilityValue newValue,
        double expectedImpact,
        String reason
    ) {
        var record = new DecisionTracker.DecisionRecord(
            System.currentTimeMillis(),
            scenario,
            capability,
            oldValue,
            newValue,
            expectedImpact,
            null,
            ActionOutcome.NEUTRAL,
            reason
        );
        decisionTracker.recordDecision(record);
        timeline.recordEvent(
            EventTimeline.EventSeverity.INFO,
            EventTimeline.EventCategory.GOVERNOR_DECISION,
            "Decision: " + capability.name() + " = " + newValue
        );
    }
    
    public void recordDecisionOutcome(
        CapabilityId capability,
        ActionOutcome outcome,
        double measuredImpact
    ) {
        effectivenessTracker.recordOutcome(capability, outcome, measuredImpact);
        
        if (outcome == ActionOutcome.NEGATIVE) {
            timeline.recordEvent(
                EventTimeline.EventSeverity.WARNING,
                EventTimeline.EventCategory.GOVERNOR_DECISION,
                "Negative outcome for " + capability.name() + " (" + measuredImpact + "ms)"
            );
        }
    }
    
    public void recordEvent(
        EventTimeline.EventSeverity severity,
        EventTimeline.EventCategory category,
        String message
    ) {
        timeline.recordEvent(severity, category, message);
        forwardCriticalEvent(severity, category, message);
    }

    public void recordCrashContext(CrashFailureContext context) {
        if (context == null) {
            return;
        }
        Map<String, String> details = new HashMap<>();
        details.put("source", String.valueOf(context.source()));
        details.put("capabilityId", String.valueOf(context.capabilityId()));
        details.put("commandType", String.valueOf(context.commandType()));
        details.put("requestedValue", String.valueOf(context.requestedValue()));
        details.put("errorMessage", String.valueOf(context.errorMessage()));
        details.put("exceptionType", String.valueOf(context.exceptionType()));
        timeline.recordEvent(
            EventTimeline.EventSeverity.ERROR,
            EventTimeline.EventCategory.CRASH_LOOP,
            "Crash context captured",
            details
        );
        forwardCriticalEvent(EventTimeline.EventSeverity.ERROR, EventTimeline.EventCategory.CRASH_LOOP,
                "Crash context captured");
    }

    public void recordCrashRecovery(CrashRecoveryDecision decision, CrashFailureContext context) {
        if (decision == null || decision.action() == null) {
            return;
        }
        Map<String, String> details = new HashMap<>();
        details.put("action", decision.action().name());
        details.put("capabilityId", String.valueOf(decision.capabilityId()));
        if (decision.retryAtMillis() > 0L) {
            details.put("retryAtMillis", String.valueOf(decision.retryAtMillis()));
        }
        if (context != null) {
            details.put("source", String.valueOf(context.source()));
            details.put("errorMessage", String.valueOf(context.errorMessage()));
            details.put("exceptionType", String.valueOf(context.exceptionType()));
        }
        EventTimeline.EventSeverity severity = decision.action() == CrashRecoveryAction.SAFE_MODE
            ? EventTimeline.EventSeverity.CRITICAL
            : EventTimeline.EventSeverity.WARNING;
        timeline.recordEvent(
            severity,
            EventTimeline.EventCategory.CRASH_LOOP,
            "Crash recovery applied: " + decision.action(),
            details
        );
        forwardCriticalEvent(severity, EventTimeline.EventCategory.CRASH_LOOP,
                "Crash recovery applied: " + decision.action());
    }

    public void setCriticalEventSink(dev.nozh.core.profiler.CriticalEventSink sink) {
        this.criticalEventSink = sink;
    }
    
    public TelemetryDashboard.DashboardSnapshot getDashboardSnapshot() {
        return dashboard.createSnapshot();
    }
    
    public Path generateMarkdownReport(Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        String timestamp = String.valueOf(System.currentTimeMillis());
        Path reportPath = outputDir.resolve("nozh_report_" + timestamp + ".md");
        Files.writeString(reportPath, reportGenerator.generateMarkdownReport());
        return reportPath;
    }
    
    public Path generateJsonReport(Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        String timestamp = String.valueOf(System.currentTimeMillis());
        Path reportPath = outputDir.resolve("nozh_report_" + timestamp + ".json");
        Files.writeString(reportPath, reportGenerator.generateJsonReport());
        return reportPath;
    }
    
    public void reset() {
        aggregator.reset();
        decisionTracker.reset();
        effectivenessTracker.reset();
        timeline.reset();
    }

    private void forwardCriticalEvent(
            EventTimeline.EventSeverity severity,
            EventTimeline.EventCategory category,
            String message) {
        if (criticalEventSink == null) {
            return;
        }
        if (severity != EventTimeline.EventSeverity.ERROR && severity != EventTimeline.EventSeverity.CRITICAL) {
            return;
        }
        criticalEventSink.recordCriticalEvent(
                severity.name(),
                category != null ? category.name() : "",
                message);
    }
}
