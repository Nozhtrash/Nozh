package dev.nozh.core.report;

/**
 * Candidate report (Explainability System - Phase 2).
 * 
 * Details of a single action candidate considered by governor.
 */
public record CandidateReport(
        String capabilityId,
        String targetValue,
        int tier,
        double confidence,
        double expectedGain, // Expected FPS improvement (-1 if unknown)
        String rejectedReason // i18n key if rejected, empty string if selected
) {
}
