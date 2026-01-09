package dev.nozh.core.safety;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;

/**
 * State data for crash loop detection and safe mode.
 * Persisted to disk for cross-session tracking.
 * 
 * Phase 2 Iteration 1: Safe mode causes as EnumSet (extensible, prioritized)
 */
public class NozhState {

    // Number of boot attempts without clean shutdown
    public int bootAttempts = 0;

    // Timestamp of last clean shutdown (epoch millis)
    public long lastCleanShutdown = 0;

    // Safe mode causes (Phase 2 Iteration 1: EnumSet replaces 3 booleans)
    public Set<SafeModeCause> safeModeCauses = EnumSet.noneOf(SafeModeCause.class);

    // Whether the current session has been marked stable
    public boolean sessionStable = false;

    // Timestamp when safe mode was last activated
    public long safeModeActivatedAt = 0;

    // Session ID for tracking
    public long sessionStartTime = 0;

    // Last failure context captured (for crash recovery decisions)
    public CrashFailureContext lastFailureContext = null;

    // Capability quarantine map (crash recovery)
    public java.util.Map<dev.nozh.core.bus.CapabilityId, Long> quarantinedCapabilities = new java.util.EnumMap<>(
            dev.nozh.core.bus.CapabilityId.class);

    /**
     * Check if safe mode is currently active (any cause).
     */
    public boolean isSafeModeActive() {
        return !safeModeCauses.isEmpty();
    }

    /**
     * Get primary (highest priority) cause for safe mode.
     * Returns null if no causes active.
     */
    public SafeModeCause getPrimaryCause() {
        return safeModeCauses.stream()
                .min(Comparator.comparingInt(SafeModeCause::getPriority))
                .orElse(null);
    }

    /**
     * Get human-readable reason for safe mode.
     */
    public String getSafeModeReason() {
        SafeModeCause primary = getPrimaryCause();
        if (primary == null) {
            return "off";
        }

        // If multiple causes, show primary + count
        if (safeModeCauses.size() > 1) {
            return primary.getReasonText() + " (+" + (safeModeCauses.size() - 1) + " more)";
        }

        return primary.getReasonText();
    }

    /**
     * Reset boot attempts (called after clean shutdown or manual reset)
     */
    public void resetBootAttempts() {
        this.bootAttempts = 0;
        this.lastCleanShutdown = System.currentTimeMillis();
    }

    /**
     * Increment boot attempts (called on startup)
     */
    public void incrementBootAttempts() {
        this.bootAttempts++;
        this.sessionStartTime = System.currentTimeMillis();
        this.sessionStable = false;
    }

    /**
     * Mark session as stable (called after N ticks of smooth operation)
     */
    public void markStable() {
        this.sessionStable = true;
        this.bootAttempts = 0;
    }

    /**
     * Activate safe mode due to crash loop
     */
    public void activateSafeModeCrashLoop() {
        this.safeModeCauses.add(SafeModeCause.CRASH_LOOP);
        this.safeModeActivatedAt = System.currentTimeMillis();
    }

    /**
     * Activate safe mode manually (user/command)
     */
    public void activateSafeModeUser() {
        this.safeModeCauses.add(SafeModeCause.USER_ENABLED);
        this.safeModeActivatedAt = System.currentTimeMillis();
    }

    /**
     * Sync config force flag (called on startup)
     */
    public void syncConfigForce(boolean forceEnabled) {
        if (forceEnabled) {
            this.safeModeCauses.add(SafeModeCause.CONFIG_FORCE);
        } else {
            this.safeModeCauses.remove(SafeModeCause.CONFIG_FORCE);
        }
    }

    public void deactivateSafeMode() {
        this.safeModeCauses.remove(SafeModeCause.USER_ENABLED);
        this.safeModeCauses.remove(SafeModeCause.CRASH_LOOP);
        // Note: CONFIG_FORCE not removed (controlled by config only)

        if (safeModeCauses.isEmpty()) {
            this.safeModeActivatedAt = 0;
        }
    }

    /**
     * Store last failure context for crash-loop recovery.
     */
    public void setLastFailureContext(CrashFailureContext context) {
        this.lastFailureContext = context;
    }

    /**
     * Quarantine a capability until a given timestamp (epoch millis).
     */
    public void quarantineCapability(dev.nozh.core.bus.CapabilityId capabilityId, long retryAtMillis) {
        if (capabilityId == null) {
            return;
        }
        quarantinedCapabilities.put(capabilityId, retryAtMillis);
    }

    /**
     * Check if a capability is still quarantined.
     */
    public boolean isCapabilityQuarantined(dev.nozh.core.bus.CapabilityId capabilityId, long nowMillis) {
        if (capabilityId == null) {
            return false;
        }
        Long retryAt = quarantinedCapabilities.get(capabilityId);
        if (retryAt == null) {
            return false;
        }
        if (retryAt <= nowMillis) {
            quarantinedCapabilities.remove(capabilityId);
            return false;
        }
        return true;
    }

    public java.util.OptionalLong getCapabilityRetryAt(dev.nozh.core.bus.CapabilityId capabilityId) {
        Long retryAt = quarantinedCapabilities.get(capabilityId);
        return retryAt != null ? java.util.OptionalLong.of(retryAt) : java.util.OptionalLong.empty();
    }

    /**
     * Remove any expired quarantines.
     */
    public void cleanupExpiredQuarantines(long nowMillis) {
        if (quarantinedCapabilities.isEmpty()) {
            return;
        }
        quarantinedCapabilities.entrySet().removeIf(entry -> entry.getValue() <= nowMillis);
    }

    // Phase 6: Action History (Reversibility)
    public java.util.List<dev.nozh.core.executor.ExecutedAction> executionHistory = new java.util.ArrayList<>();
}
