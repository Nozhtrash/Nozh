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

    // Phase 6: Action History (Reversibility)
    public java.util.List<dev.nozh.core.executor.ExecutedAction> executionHistory = new java.util.ArrayList<>();
}
