package dev.nozh.core.matrix;

import dev.nozh.core.bus.CapabilityId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Action success tracker (Contract 5).
 * 
 * Tracks historical success/failure per capability.
 * Includes environment hash versioning for confidence decay on env changes.
 * 
 * PURE - no MC dependencies.
 */
public final class ActionSuccessTracker {

    private final Map<CapabilityId, SuccessRecord> records = new ConcurrentHashMap<>();
    private String currentEnvironmentHash;

    public ActionSuccessTracker(String initialEnvironmentHash) {
        this.currentEnvironmentHash = initialEnvironmentHash;
    }

    /**
     * Record a success.
     */
    public void recordSuccess(CapabilityId id) {
        long now = System.currentTimeMillis();
        records.compute(id, (key, record) -> {
            if (record == null) {
                return new SuccessRecord(1, 0, now, 0, currentEnvironmentHash);
            }
            return new SuccessRecord(
                    record.successCount + 1,
                    record.failCount,
                    now,
                    record.lastFailureMillis,
                    currentEnvironmentHash);
        });
    }

    /**
     * Record a failure.
     */
    public void recordFailure(CapabilityId id) {
        long now = System.currentTimeMillis();
        records.compute(id, (key, record) -> {
            if (record == null) {
                return new SuccessRecord(0, 1, 0, now, currentEnvironmentHash);
            }
            return new SuccessRecord(
                    record.successCount,
                    record.failCount + 1,
                    record.lastSuccessMillis,
                    now,
                    currentEnvironmentHash);
        });
    }

    /**
     * Get success rate for a capability.
     * 
     * @return Success rate [0..1], or 0.5 if no history
     */
    public double getSuccessRate(CapabilityId id) {
        SuccessRecord record = records.get(id);
        if (record == null) {
            return 0.5; // No data -> neutral
        }

        int total = record.successCount + record.failCount;
        if (total == 0) {
            return 0.5;
        }

        return (double) record.successCount / total;
    }

    /**
     * Get last success timestamp.
     */
    public long getLastSuccessMillis(CapabilityId id) {
        SuccessRecord record = records.get(id);
        return record != null ? record.lastSuccessMillis : 0;
    }

    /**
     * Get last failure timestamp.
     */
    public long getLastFailureMillis(CapabilityId id) {
        SuccessRecord record = records.get(id);
        return record != null ? record.lastFailureMillis : 0;
    }

    /**
     * Check if environment has changed since record was created.
     * If changed -> confidence should decay.
     */
    public boolean isEnvironmentChanged(CapabilityId id) {
        SuccessRecord record = records.get(id);
        if (record == null) {
            return false;
        }
        return !currentEnvironmentHash.equals(record.environmentHash);
    }

    /**
     * Update environment hash (e.g., mod list changed).
     * This will cause confidence decay for all existing records.
     */
    public void updateEnvironmentHash(String newHash) {
        this.currentEnvironmentHash = newHash;
    }

    /**
     * Internal success record.
     */
    private record SuccessRecord(
            int successCount,
            int failCount,
            long lastSuccessMillis,
            long lastFailureMillis,
            String environmentHash) {
    }
}
