package dev.nozh.core.capability;

import dev.nozh.core.bus.CapabilityId;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provider health tracker (Contract 3).
 * 
 * Tracks operational status of each registered provider.
 * 
 * CRITICAL CONTRACT: One broken provider MUST NOT crash system.
 * This tracker ensures isolation.
 */
public final class ProviderHealthTracker {

    private final Map<CapabilityId, HealthRecord> healthRecords = new ConcurrentHashMap<>();

    /**
     * Mark provider as healthy.
     */
    public void markHealthy(CapabilityId id) {
        healthRecords.put(id, new HealthRecord(ProviderStatus.HEALTHY, null));
    }

    /**
     * Mark provider as degraded with reason.
     */
    public void markDegraded(CapabilityId id, String reason) {
        healthRecords.put(id, new HealthRecord(ProviderStatus.DEGRADED, reason));
    }

    /**
     * Mark provider as broken with reason.
     * 
     * BROKEN providers are excluded from registry by default.
     */
    public void markBroken(CapabilityId id, String reason) {
        healthRecords.put(id, new HealthRecord(ProviderStatus.BROKEN, reason));
    }

    /**
     * Get current status for a provider.
     * 
     * @return Status, or HEALTHY if never registered (optimistic default)
     */
    public ProviderStatus getStatus(CapabilityId id) {
        HealthRecord record = healthRecords.get(id);
        return record != null ? record.status : ProviderStatus.HEALTHY;
    }

    /**
     * Get status reason for a provider.
     * 
     * @return Reason, or empty if HEALTHY or never registered
     */
    public Optional<String> getStatusReason(CapabilityId id) {
        HealthRecord record = healthRecords.get(id);
        return record != null ? Optional.ofNullable(record.reason) : Optional.empty();
    }

    /**
     * Check if provider is healthy.
     */
    public boolean isHealthy(CapabilityId id) {
        return getStatus(id) == ProviderStatus.HEALTHY;
    }

    /**
     * Check if provider is broken.
     */
    public boolean isBroken(CapabilityId id) {
        return getStatus(id) == ProviderStatus.BROKEN;
    }

    /**
     * Get all tracked provider IDs.
     */
    public java.util.Set<CapabilityId> getTrackedProviders() {
        return java.util.Set.copyOf(healthRecords.keySet());
    }

    /**
     * Internal health record.
     */
    private record HealthRecord(
            ProviderStatus status,
            String reason // nullable
    ) {
    }
}
