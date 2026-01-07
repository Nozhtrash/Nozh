package dev.nozh.core.profiler;

public record DecisionLatencyStats(
        long decisionCount,
        double avgLatencyMs,
        long maxLatencyMs,
        long lastLatencyMs) {

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"decisionCount\": ").append(decisionCount).append(",\n");
        sb.append("  \"avgLatencyMs\": ").append(String.format("%.2f", avgLatencyMs)).append(",\n");
        sb.append("  \"maxLatencyMs\": ").append(maxLatencyMs).append(",\n");
        sb.append("  \"lastLatencyMs\": ").append(lastLatencyMs).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}
