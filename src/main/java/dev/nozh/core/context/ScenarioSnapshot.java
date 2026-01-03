package dev.nozh.core.context;

/**
 * Snapshot of scenario detection with confidence.
 */
public record ScenarioSnapshot(
        Scenario scenario,
        double confidence) {
    public ScenarioSnapshot {
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Scenario confidence must be between 0.0 and 1.0");
        }
    }
}
