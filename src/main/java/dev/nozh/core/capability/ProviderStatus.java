package dev.nozh.core.capability;

/**
 * Provider health status.
 * 
 * Contract 3: Provider Guarantees
 * Tracks operational status of a CapabilityProvider.
 * 
 * Enforces isolation: one BROKEN provider must not crash the registry.
 */
public enum ProviderStatus {
    /**
     * Provider is fully operational.
     * All operations (getCurrentValue, apply, rollback) working as expected.
     */
    HEALTHY,

    /**
     * Provider is operational but with reduced functionality.
     * Example: Rollback unavailable, slow response times, partial MC API access.
     */
    DEGRADED,

    /**
     * Provider is non-functional.
     * Example: Init threw exception, required mod missing, MC API incompatible.
     * 
     * CRITICAL: BROKEN providers are excluded from registry by default.
     */
    BROKEN
}
