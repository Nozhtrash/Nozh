package dev.nozh.core.context;

/**
 * Detects the current gameplay scenario.
 * Contract: Must be fast (called every tick or so).
 */
public interface ScenarioDetector {
    /**
     * Detect current scenario based on game state.
     */
    ScenarioSnapshot detect();

    /**
     * Return current entity count estimate, or -1 if unavailable.
     */
    default int getEntityCount() {
        return -1;
    }
}
