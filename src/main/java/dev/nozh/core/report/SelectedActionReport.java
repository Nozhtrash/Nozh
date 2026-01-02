package dev.nozh.core.report;

/**
 * Selected action report (Explainability System - Phase 2).
 * 
 * Details of the action selected by governor.
 */
public record SelectedActionReport(
        String capabilityId,
        String targetValue,
        double confidence,
        String rationaleKey // i18n key explaining why this was chosen
) {
}
