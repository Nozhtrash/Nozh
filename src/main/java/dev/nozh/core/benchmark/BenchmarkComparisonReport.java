package dev.nozh.core.benchmark;

import dev.nozh.api.PerfSnapshot;

import java.util.Objects;

/**
 * Comparative benchmark report for large modpacks and regression checks.
 */
public record BenchmarkComparisonReport(
        PerfSnapshot baseline,
        PerfSnapshot scenario,
        double avgDeltaMs,
        double p95DeltaMs,
        double p99DeltaMs,
        double avgDeltaPercent,
        double p95DeltaPercent,
        double p99DeltaPercent,
        String verdict) {

    public static BenchmarkComparisonReport compare(PerfSnapshot baseline, PerfSnapshot scenario) {
        Objects.requireNonNull(baseline, "baseline");
        Objects.requireNonNull(scenario, "scenario");
        double avgDelta = scenario.avgFrametimeMs() - baseline.avgFrametimeMs();
        double p95Delta = scenario.p95FrametimeMs() - baseline.p95FrametimeMs();
        double p99Delta = scenario.p99FrametimeMs() - baseline.p99FrametimeMs();
        double avgDeltaPct = percentDelta(baseline.avgFrametimeMs(), scenario.avgFrametimeMs());
        double p95DeltaPct = percentDelta(baseline.p95FrametimeMs(), scenario.p95FrametimeMs());
        double p99DeltaPct = percentDelta(baseline.p99FrametimeMs(), scenario.p99FrametimeMs());
        String verdict = classify(avgDelta, p95Delta, p99Delta);
        return new BenchmarkComparisonReport(
                baseline,
                scenario,
                avgDelta,
                p95Delta,
                p99Delta,
                avgDeltaPct,
                p95DeltaPct,
                p99DeltaPct,
                verdict);
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"verdict\": \"").append(escapeJson(verdict)).append("\",\n");
        sb.append("  \"baseline\": ").append(formatSnapshot(baseline)).append(",\n");
        sb.append("  \"scenario\": ").append(formatSnapshot(scenario)).append(",\n");
        sb.append("  \"deltaMs\": {\n");
        sb.append("    \"avg\": ").append(formatMetric(avgDeltaMs)).append(",\n");
        sb.append("    \"p95\": ").append(formatMetric(p95DeltaMs)).append(",\n");
        sb.append("    \"p99\": ").append(formatMetric(p99DeltaMs)).append("\n");
        sb.append("  },\n");
        sb.append("  \"deltaPercent\": {\n");
        sb.append("    \"avg\": ").append(formatMetric(avgDeltaPercent)).append(",\n");
        sb.append("    \"p95\": ").append(formatMetric(p95DeltaPercent)).append(",\n");
        sb.append("    \"p99\": ").append(formatMetric(p99DeltaPercent)).append("\n");
        sb.append("  }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static double percentDelta(double baselineValue, double scenarioValue) {
        if (!Double.isFinite(baselineValue) || baselineValue <= 0.0 || !Double.isFinite(scenarioValue)) {
            return Double.NaN;
        }
        return ((scenarioValue - baselineValue) / baselineValue) * 100.0;
    }

    private static String classify(double avgDelta, double p95Delta, double p99Delta) {
        double worstDelta = Math.max(avgDelta, Math.max(p95Delta, p99Delta));
        double bestDelta = Math.min(avgDelta, Math.min(p95Delta, p99Delta));
        if (Double.isFinite(bestDelta) && bestDelta <= -0.75) {
            return "IMPROVED";
        }
        if (Double.isFinite(worstDelta) && worstDelta >= 0.75) {
            return "REGRESSED";
        }
        return "NEUTRAL";
    }

    private static String formatSnapshot(PerfSnapshot snapshot) {
        if (snapshot == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"timestampMillis\":").append(snapshot.timestampMillis()).append(",");
        sb.append("\"avgFrametimeMs\":").append(formatMetric(snapshot.avgFrametimeMs())).append(",");
        sb.append("\"p95FrametimeMs\":").append(formatMetric(snapshot.p95FrametimeMs())).append(",");
        sb.append("\"p99FrametimeMs\":").append(formatMetric(snapshot.p99FrametimeMs())).append(",");
        sb.append("\"frametimeStddevMs\":").append(formatMetric(snapshot.frametimeStddevMs())).append(",");
        sb.append("\"spikeCount\":").append(snapshot.spikeCount()).append(",");
        sb.append("\"sampleCount\":").append(snapshot.sampleCount()).append(",");
        sb.append("\"windowSeconds\":").append(snapshot.windowSeconds());
        sb.append("}");
        return sb.toString();
    }

    private static String formatMetric(double value) {
        if (!Double.isFinite(value)) {
            return "\"--\"";
        }
        return String.format("%.3f", value);
    }

    private static String escapeJson(String value) {
        return Objects.toString(value, "")
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
