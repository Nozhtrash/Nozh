package dev.nozh.core.testing;

import java.util.List;

/**
 * Chaos test report (Phase 4 - Contract 11.1).
 * 
 * Complete chaos test suite execution report.
 */
public record ChaosTestReport(
        List<ChaosScenarioResult> results,
        int totalScenarios,
        int passed,
        int failed,
        long totalDurationMs,
        ChaosReportMetadata metadata) {
    /**
     * Check if all scenarios passed.
     */
    public boolean allPassed() {
        return failed == 0;
    }
}
