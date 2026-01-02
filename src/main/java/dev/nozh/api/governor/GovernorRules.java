package dev.nozh.api.governor;

/**
 * Configuration rules for the Governor.
 * Injected dependency to keep Governor pure.
 */
public record GovernorRules(
        double targetFrameMs,
        double avgWarnMultiplier, // e.g. 1.2
        double avgCriticalMultiplier, // e.g. 1.5
        double p95WarnMultiplier, // e.g. 1.4
        double p95CriticalMultiplier, // e.g. 1.8
        boolean logWouldActions) {
}
