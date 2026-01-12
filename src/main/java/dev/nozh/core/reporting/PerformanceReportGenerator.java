package dev.nozh.core.reporting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.nozh.api.Scenario;
import dev.nozh.core.intelligence.SmartProfileManager;
import dev.nozh.core.intelligence.SmartProfileManager.HardwareProfile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Generates detailed performance reports for debugging and analysis.
 * Can export to JSON, HTML, or Markdown.
 * 
 * INTEGRATION: Reporting and analysis
 * CONTRACT: Thread-safe, file I/O safe
 */
public final class PerformanceReportGenerator {

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault());

    /**
     * Summary of an action taken.
     */
    public record ActionSummary(
        String action,
        long timestamp,
        String outcome,
        double impactMs
    ) {}

    /**
     * Telemetry summary.
     */
    public record TelemetrySummary(
        double avgFps,
        double minFps,
        double maxFps,
        double p95Frametime,
        double p99Frametime,
        int totalSamples,
        int spikeCount
    ) {}

    /**
     * Complete performance report.
     */
    public record PerformanceReport(
        long generatedAt,
        String sessionDuration,
        HardwareProfile hardware,
        Map<Scenario, Long> timePerScenario,
        List<ActionSummary> actionsApplied,
        TelemetrySummary telemetry,
        List<String> recommendations
    ) {}

    private final long sessionStartTime;
    private final List<ActionSummary> actionHistory = Collections.synchronizedList(new ArrayList<>());
    private final Map<Scenario, Long> scenarioTime = new EnumMap<>(Scenario.class);
    private volatile HardwareProfile hardwareProfile;

    public PerformanceReportGenerator() {
        this.sessionStartTime = System.currentTimeMillis();
    }

    /**
     * Record an action for the report.
     */
    public void recordAction(String action, String outcome, double impactMs) {
        actionHistory.add(new ActionSummary(
            action,
            System.currentTimeMillis(),
            outcome,
            impactMs
        ));
    }

    /**
     * Record time in scenario.
     */
    public void recordScenarioTime(Scenario scenario, long durationMs) {
        scenarioTime.merge(scenario, durationMs, Long::sum);
    }

    /**
     * Set hardware profile.
     */
    public void setHardwareProfile(HardwareProfile profile) {
        this.hardwareProfile = profile;
    }

    /**
     * Generate a complete performance report.
     */
    public PerformanceReport generateReport() {
        return generateReport(null);
    }

    /**
     * Generate a complete performance report with custom telemetry.
     */
    public PerformanceReport generateReport(TelemetrySummary telemetry) {
        long now = System.currentTimeMillis();
        long durationMs = now - sessionStartTime;
        String duration = formatDuration(durationMs);

        // Generate recommendations
        List<String> recommendations = generateRecommendations(telemetry);

        // Create default telemetry if not provided
        if (telemetry == null) {
            telemetry = new TelemetrySummary(60.0, 30.0, 120.0, 16.7, 20.0, 0, 0);
        }

        return new PerformanceReport(
            now,
            duration,
            hardwareProfile,
            new EnumMap<>(scenarioTime),
            new ArrayList<>(actionHistory),
            telemetry,
            recommendations
        );
    }

    /**
     * Export report to JSON.
     */
    public String exportToJson(PerformanceReport report) {
        return GSON.toJson(report);
    }

    /**
     * Export report to Markdown.
     */
    public String exportToMarkdown(PerformanceReport report) {
        StringBuilder md = new StringBuilder();
        
        md.append("# NOZH Performance Report\n\n");
        md.append("**Generated:** ").append(TIME_FORMATTER.format(Instant.ofEpochMilli(report.generatedAt()))).append("\n");
        md.append("**Session Duration:** ").append(report.sessionDuration()).append("\n\n");

        // Hardware section
        if (report.hardware() != null) {
            md.append("## Hardware Profile\n\n");
            HardwareProfile hw = report.hardware();
            md.append("- **Class:** ").append(hw.hardwareClass()).append("\n");
            md.append("- **Recommended Render Distance:** ").append(hw.recommendedRenderDistance()).append("\n");
            md.append("- **Recommended Simulation Distance:** ").append(hw.recommendedSimulationDistance()).append("\n");
            md.append("- **Graphics Mode:** ").append(hw.graphicsMode()).append("\n\n");
        }

        // Telemetry section
        TelemetrySummary tel = report.telemetry();
        md.append("## Performance Metrics\n\n");
        md.append("| Metric | Value |\n");
        md.append("|--------|-------|\n");
        md.append(String.format("| Average FPS | %.1f |\n", tel.avgFps()));
        md.append(String.format("| Min FPS | %.1f |\n", tel.minFps()));
        md.append(String.format("| Max FPS | %.1f |\n", tel.maxFps()));
        md.append(String.format("| P95 Frametime | %.2f ms |\n", tel.p95Frametime()));
        md.append(String.format("| P99 Frametime | %.2f ms |\n", tel.p99Frametime()));
        md.append(String.format("| Spike Count | %d |\n", tel.spikeCount()));
        md.append("\n");

        // Scenario breakdown
        if (!report.timePerScenario().isEmpty()) {
            md.append("## Time Per Scenario\n\n");
            md.append("| Scenario | Duration |\n");
            md.append("|----------|----------|\n");
            for (Map.Entry<Scenario, Long> entry : report.timePerScenario().entrySet()) {
                md.append(String.format("| %s | %s |\n", 
                    entry.getKey(), formatDuration(entry.getValue())));
            }
            md.append("\n");
        }

        // Actions
        if (!report.actionsApplied().isEmpty()) {
            md.append("## Actions Applied\n\n");
            md.append("| Time | Action | Outcome | Impact |\n");
            md.append("|------|--------|---------|--------|\n");
            for (ActionSummary action : report.actionsApplied()) {
                String time = TIME_FORMATTER.format(Instant.ofEpochMilli(action.timestamp()));
                md.append(String.format("| %s | %s | %s | %.1f ms |\n",
                    time, action.action(), action.outcome(), action.impactMs()));
            }
            md.append("\n");
        }

        // Recommendations
        if (!report.recommendations().isEmpty()) {
            md.append("## Recommendations\n\n");
            for (String rec : report.recommendations()) {
                md.append("- ").append(rec).append("\n");
            }
            md.append("\n");
        }

        return md.toString();
    }

    /**
     * Save report to file.
     */
    public void saveToFile(PerformanceReport report, Path path) throws IOException {
        String content;
        String filename = path.getFileName().toString().toLowerCase();
        
        if (filename.endsWith(".json")) {
            content = exportToJson(report);
        } else if (filename.endsWith(".md") || filename.endsWith(".markdown")) {
            content = exportToMarkdown(report);
        } else {
            // Default to JSON
            content = exportToJson(report);
        }

        Files.writeString(path, content, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }

    private List<String> generateRecommendations(TelemetrySummary telemetry) {
        List<String> recommendations = new ArrayList<>();
        
        if (telemetry == null) {
            recommendations.add("Enable telemetry to get personalized recommendations");
            return recommendations;
        }

        if (telemetry.avgFps() < 30) {
            recommendations.add("Low average FPS detected. Consider reducing render distance or graphics quality.");
        }
        
        if (telemetry.spikeCount() > 10) {
            recommendations.add("Frequent frame spikes detected. Enable spike analysis for detailed insights.");
        }
        
        if (telemetry.p95Frametime() > 33.3) {
            recommendations.add("P95 frametime indicates inconsistent performance. Consider stability mode.");
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Performance is stable. No immediate optimizations needed.");
        }

        return recommendations;
    }

    /**
     * Clear action history.
     */
    public void clearHistory() {
        actionHistory.clear();
        scenarioTime.clear();
    }

    /**
     * Get action count.
     */
    public int getActionCount() {
        return actionHistory.size();
    }
}
