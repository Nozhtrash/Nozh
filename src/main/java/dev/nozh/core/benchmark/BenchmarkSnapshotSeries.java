package dev.nozh.core.benchmark;

import dev.nozh.api.PerfSnapshot;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Captures a series of perf snapshots during a scenario.
 */
public final class BenchmarkSnapshotSeries {

    private final List<PerfSnapshot> snapshots = new ArrayList<>();

    public void addSnapshot(PerfSnapshot snapshot) {
        if (snapshot == null || snapshot.sampleCount() <= 0) {
            return;
        }
        snapshots.add(snapshot);
    }

    public boolean isEmpty() {
        return snapshots.isEmpty();
    }

    public int size() {
        return snapshots.size();
    }

    public PerfSnapshot latestSnapshot() {
        if (snapshots.isEmpty()) {
            return null;
        }
        return snapshots.get(snapshots.size() - 1);
    }

    public void clear() {
        snapshots.clear();
    }

    public Path writeJson(Path outputFile) throws Exception {
        Files.writeString(outputFile, toJson(), StandardCharsets.UTF_8);
        return outputFile;
    }

    private String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"samples\": [\n");
        for (int i = 0; i < snapshots.size(); i++) {
            PerfSnapshot snapshot = snapshots.get(i);
            sb.append("    {\n");
            sb.append("      \"timestampMillis\": ").append(snapshot.timestampMillis()).append(",\n");
            sb.append("      \"avgFrametimeMs\": ").append(formatMetric(snapshot.avgFrametimeMs())).append(",\n");
            sb.append("      \"p95FrametimeMs\": ").append(formatMetric(snapshot.p95FrametimeMs())).append(",\n");
            sb.append("      \"p99FrametimeMs\": ").append(formatMetric(snapshot.p99FrametimeMs())).append(",\n");
            sb.append("      \"frametimeStddevMs\": ").append(formatMetric(snapshot.frametimeStddevMs())).append(",\n");
            sb.append("      \"spikeCount\": ").append(snapshot.spikeCount()).append(",\n");
            sb.append("      \"sampleCount\": ").append(snapshot.sampleCount()).append(",\n");
            sb.append("      \"windowSeconds\": ").append(snapshot.windowSeconds()).append("\n");
            sb.append("    }");
            if (i < snapshots.size() - 1) {
                sb.append(',');
            }
            sb.append('\n');
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    private String formatMetric(double value) {
        if (!Double.isFinite(value)) {
            return "\"--\"";
        }
        return String.format("%.3f", value);
    }

}
