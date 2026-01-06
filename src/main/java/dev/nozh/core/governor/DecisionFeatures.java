package dev.nozh.core.governor;

import dev.nozh.core.context.Scenario;

public record DecisionFeatures(
        double avgFrametimeMs,
        double p95FrametimeMs,
        int spikeCount,
        String bound,
        GovernorMode mode,
        Scenario scenario) {

    public double featureValue(String featureName) {
        if (featureName == null) {
            return 0.0;
        }
        return switch (featureName) {
            case "avgFrametimeMs" -> avgFrametimeMs;
            case "p95FrametimeMs" -> p95FrametimeMs;
            case "spikeCount" -> spikeCount;
            case "boundCpu" -> "CPU".equalsIgnoreCase(bound) ? 1.0 : 0.0;
            case "boundGpu" -> "GPU".equalsIgnoreCase(bound) ? 1.0 : 0.0;
            case "modeAggressive" -> mode == GovernorMode.AUTO_AGGRESSIVE ? 1.0 : 0.0;
            case "modeConservative" -> mode == GovernorMode.AUTO_CONSERVATIVE ? 1.0 : 0.0;
            case "scenarioCombat" -> scenario == Scenario.COMBAT ? 1.0 : 0.0;
            default -> 0.0;
        };
    }
}
