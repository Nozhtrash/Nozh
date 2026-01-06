package dev.nozh.core.telemetry;

import dev.nozh.api.Scenario;
import dev.nozh.core.capability.CapabilityId;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Generates professional telemetry reports in multiple formats.
 */
public final class TelemetryReportGenerator {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault());
    
    private final TelemetryDashboard dashboard;
    
    public TelemetryReportGenerator(TelemetryDashboard dashboard) {
        this.dashboard = dashboard;
    }
    
    public String generateMarkdownReport() {
        var snapshot = dashboard.createSnapshot();
        var sb = new StringBuilder();
        
        sb.append("# NOZH Telemetry Report\n\n");
        sb.append("Generated: ").append(formatTimestamp(snapshot.timestamp())).append("\n\n");
        
        // Global Summary
        sb.append("## Global Summary\n\n");
        var global = snapshot.globalMetrics();
        sb.append("- **Total Samples**: ").append(global.totalSamples()).append("\n");
        sb.append("- **Total Spikes**: ").append(global.totalSpikes()).append("\n");
        sb.append("- **Average Frametime**: ").append(String.format("%.2f ms", global.avgFrametime())).append("\n");
        sb.append("- **Scenarios Encountered**: ").append(global.scenariosEncountered()).append("\n");
        sb.append("- **Total Decisions**: ").append(snapshot.totalDecisions()).append("\n");
        sb.append("- **Avg Decision Impact**: ").append(String.format("%.2f ms", snapshot.avgDecisionImpact())).append("\n\n");
        
        // Performance by Scenario
        sb.append("## Performance by Scenario\n\n");
        sb.append("| Scenario | Samples | Spikes | Avg P95 | Avg P99 | Duration |\n");
        sb.append("|----------|---------|--------|---------|---------|----------|\n");
        
        for (var entry : snapshot.scenarioMetrics().entrySet()) {
            var scenario = entry.getKey();
            var metrics = entry.getValue();
            sb.append(String.format("| %s | %d | %d | %.2f ms | %.2f ms | %d s |\n",
                scenario.name(),
                metrics.sampleCount(),
                metrics.spikeCount(),
                metrics.avgP95(),
                metrics.avgP99(),
                metrics.durationMs() / 1000
            ));
        }
        sb.append("\n");
        
        // Top Capabilities
        sb.append("## Top Performing Capabilities\n\n");
        sb.append("| Capability | Success Rate | Avg Impact | Usage Count | Score |\n");
        sb.append("|------------|--------------|------------|-------------|-------|\n");
        
        for (var cap : snapshot.topCapabilities()) {
            sb.append(String.format("| %s | %.1f%% | %.2f ms | %d | %.2f |\n",
                cap.capability().name(),
                cap.successRate() * 100,
                cap.avgImpact(),
                cap.usageCount(),
                cap.score()
            ));
        }
        sb.append("\n");
        
        // Recent Decisions
        sb.append("## Recent Decisions\n\n");
        for (var decision : snapshot.recentDecisions()) {
            sb.append("- **").append(formatTimestamp(decision.timestamp())).append("** ");
            sb.append("[").append(decision.scenario().name()).append("] ");
            sb.append(decision.capability().name()).append(": ");
            sb.append(decision.oldValue()).append(" → ").append(decision.newValue());
            sb.append(" (").append(decision.outcome()).append(")\n");
            if (decision.measuredImpactMs() != null) {
                sb.append("  - Impact: ").append(String.format("%.2f ms", decision.measuredImpactMs())).append("\n");
            }
        }
        sb.append("\n");
        
        // Critical Events
        sb.append("## Critical Events\n\n");
        var criticalEvents = snapshot.recentEvents().stream()
            .filter(e -> e.severity() == EventTimeline.EventSeverity.ERROR 
                      || e.severity() == EventTimeline.EventSeverity.CRITICAL)
            .toList();
        
        if (criticalEvents.isEmpty()) {
            sb.append("*No critical events recorded*\n\n");
        } else {
            for (var event : criticalEvents) {
                sb.append("- **[").append(event.severity()).append("] ");
                sb.append(formatTimestamp(event.timestamp())).append("** ");
                sb.append(event.message()).append("\n");
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    public String generateJsonReport() {
        var snapshot = dashboard.createSnapshot();
        // Simple JSON generation without external dependencies
        var sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"timestamp\": ").append(snapshot.timestamp()).append(",\n");
        sb.append("  \"generated\": \"").append(formatTimestamp(snapshot.timestamp())).append("\",\n");
        
        var global = snapshot.globalMetrics();
        sb.append("  \"global\": {\n");
        sb.append("    \"totalSamples\": ").append(global.totalSamples()).append(",\n");
        sb.append("    \"totalSpikes\": ").append(global.totalSpikes()).append(",\n");
        sb.append("    \"avgFrametime\": ").append(global.avgFrametime()).append(",\n");
        sb.append("    \"scenariosEncountered\": ").append(global.scenariosEncountered()).append("\n");
        sb.append("  },\n");
        
        sb.append("  \"totalDecisions\": ").append(snapshot.totalDecisions()).append(",\n");
        sb.append("  \"avgDecisionImpact\": ").append(snapshot.avgDecisionImpact()).append(",\n");
        
        sb.append("  \"topCapabilities\": [\n");
        var caps = snapshot.topCapabilities();
        for (int i = 0; i < caps.size(); i++) {
            var cap = caps.get(i);
            sb.append("    {\n");
            sb.append("      \"capability\": \"").append(cap.capability().name()).append("\",\n");
            sb.append("      \"successRate\": ").append(cap.successRate()).append(",\n");
            sb.append("      \"avgImpact\": ").append(cap.avgImpact()).append(",\n");
            sb.append("      \"usageCount\": ").append(cap.usageCount()).append(",\n");
            sb.append("      \"score\": ").append(cap.score()).append("\n");
            sb.append("    }").append(i < caps.size() - 1 ? "," : "").append("\n");
        }
        sb.append("  ]\n");
        
        sb.append("}\n");
        return sb.toString();
    }
    
    private String formatTimestamp(long timestamp) {
        return FORMATTER.format(Instant.ofEpochMilli(timestamp));
    }
}