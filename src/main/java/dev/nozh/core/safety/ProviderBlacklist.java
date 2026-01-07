package dev.nozh.core.safety;

import dev.nozh.NozhConstants;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manages blacklist of risky/broken providers.
 * 
 * Providers can be blacklisted for:
 * - Repeated failures (3+ failed attempts)
 * - Rollback failures (can't revert changes)
 * - Known incompatibilities
 * - Manual override (safe mode)
 * 
 * Blacklisted providers are excluded from governor decisions.
 * 
 * TASK 4: Safe rollback - risk management
 */
public final class ProviderBlacklist {

    private static final int FAILURE_THRESHOLD = 3;
    private static final long BLACKLIST_DURATION_MS = 300_000; // 5 minutes

    private final Set<String> permanentBlacklist = new HashSet<>();
    private final Map<String, BlacklistEntry> temporaryBlacklist = new HashMap<>();
    private final Map<String, Integer> failureCount = new HashMap<>();

    /**
     * Check if provider is blacklisted.
     */
    public boolean isBlacklisted(String providerId) {
        if (permanentBlacklist.contains(providerId)) {
            return true;
        }

        BlacklistEntry entry = temporaryBlacklist.get(providerId);
        if (entry != null) {
            if (entry.isExpired()) {
                temporaryBlacklist.remove(providerId);
                return false;
            }
            return true;
        }

        return false;
    }

    /**
     * Record provider failure.
     * Auto-blacklist after threshold.
     */
    public void recordFailure(String providerId, String reason) {
        int count = failureCount.getOrDefault(providerId, 0) + 1;
        failureCount.put(providerId, count);

        NozhConstants.LOGGER.warn("Provider " + providerId + " failed (" + count + "x): " + reason);

        if (count >= FAILURE_THRESHOLD) {
            blacklistTemporary(providerId, "Repeated failures (" + count + "x)");
        }
    }

    /**
     * Blacklist provider temporarily.
     */
    public void blacklistTemporary(String providerId, String reason) {
        BlacklistEntry entry = new BlacklistEntry(providerId, reason, BLACKLIST_DURATION_MS);
        temporaryBlacklist.put(providerId, entry);
        NozhConstants.LOGGER.error("Provider blacklisted: " + providerId + " - " + reason);
    }

    /**
     * Blacklist provider permanently.
     */
    public void blacklistPermanent(String providerId, String reason) {
        permanentBlacklist.add(providerId);
        temporaryBlacklist.remove(providerId);
        NozhConstants.LOGGER.error("Provider permanently blacklisted: " + providerId + " - " + reason);
    }

    /**
     * Remove from blacklist (whitelist).
     */
    public void whitelist(String providerId) {
        permanentBlacklist.remove(providerId);
        temporaryBlacklist.remove(providerId);
        failureCount.remove(providerId);
        NozhConstants.LOGGER.info("Provider whitelisted: " + providerId);
    }

    /**
     * Get blacklist reason.
     */
    public String getBlacklistReason(String providerId) {
        if (permanentBlacklist.contains(providerId)) {
            return "Permanently blacklisted";
        }

        BlacklistEntry entry = temporaryBlacklist.get(providerId);
        if (entry != null) {
            return entry.reason + " (expires in " + entry.getRemainingMs() + "ms)";
        }

        return "Not blacklisted";
    }

    /**
     * Get all blacklisted providers.
     */
    public Set<String> getBlacklistedProviders() {
        Set<String> result = new HashSet<>(permanentBlacklist);
        result.addAll(temporaryBlacklist.keySet());
        return result;
    }

    /**
     * Clear all blacklists (admin function).
     */
    public void clearAll() {
        permanentBlacklist.clear();
        temporaryBlacklist.clear();
        failureCount.clear();
        NozhConstants.LOGGER.warn("All blacklists cleared");
    }

    /**
     * Blacklist entry record.
     */
    private static class BlacklistEntry {
        final String providerId;
        final String reason;
        final long expiryTime;

        BlacklistEntry(String providerId, String reason, long durationMs) {
            this.providerId = providerId;
            this.reason = reason;
            this.expiryTime = System.currentTimeMillis() + durationMs;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }

        long getRemainingMs() {
            return Math.max(0, expiryTime - System.currentTimeMillis());
        }
    }

    // === PREDEFINED RISKY PROVIDERS ===

    /**
     * Initialize with known risky providers.
     */
    public void initializeDefaults() {
        // Fog control is stub in vanilla - blacklist by default
        blacklistPermanent("fog_control", "Not supported in vanilla");

        NozhConstants.LOGGER.info("Provider blacklist initialized with defaults");
    }
}
