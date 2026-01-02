package dev.nozh.core.issues;

/**
 * Issue type classification (Contract 9).
 * 
 * Each type represents a distinct diagnostic pattern.
 */
public enum IssueType {
    PROVIDER_DEGRADED,
    PROVIDER_BROKEN,
    MIXIN_CONFLICT,
    BENCHMARK_NOISE,
    GOVERNOR_FLAPPING,
    TELEMETRY_STARVATION,
    UNSUPPORTED_SHADER,
    UNKNOWN
}
