package dev.nozh.core.report;

import dev.nozh.core.governor.GovernorMode;
import dev.nozh.core.preset.HardwareTier;
import dev.nozh.core.issues.ParanoiaLevel;

import java.util.List;

/**
 * Decision report (Explainability System - Phase 2).
 * 
 * Complete record of governor decision process.
 * PURE - serializable, deterministic.
 */
public record DecisionReport(
        long timestampMillis,
        GovernorMode governorMode,
        HardwareTier presetTier,
        ParanoiaLevel paranoiaLevel,
        long cooldownRemainingMs,
        List<CandidateReport> candidatesConsidered, // Max 10
        SelectedActionReport selected, // null if no action taken
        String reasonKey // i18n key explaining decision
) {
    /**
     * Decision with no action (governor idle or blocked).
     */
    public static DecisionReport noAction(
            GovernorMode mode,
            HardwareTier tier,
            ParanoiaLevel paranoia,
            long cooldown,
            String reasonKey) {
        return new DecisionReport(
                System.currentTimeMillis(),
                mode,
                tier,
                paranoia,
                cooldown,
                List.of(),
                null,
                reasonKey);
    }
}
