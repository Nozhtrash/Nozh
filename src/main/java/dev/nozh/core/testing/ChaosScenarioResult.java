package dev.nozh.core.testing;

/**
 * Chaos scenario result (Phase 4 - Contract 11.1).
 * 
 * Result of a single chaos test scenario execution.
 */
public record ChaosScenarioResult(
        ChaosScenario scenario,
        boolean passed,
        String failureReason, // Empty string if passed
        long durationMs) {
    /**
     * Create passing result.
     */
    public static ChaosScenarioResult pass(ChaosScenario scenario, long durationMs) {
        return new ChaosScenarioResult(scenario, true, "", durationMs);
    }

    /**
     * Create failing result.
     */
    public static ChaosScenarioResult fail(ChaosScenario scenario, String reason, long durationMs) {
        return new ChaosScenarioResult(scenario, false, reason, durationMs);
    }
}
