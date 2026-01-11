package dev.nozh.core.manual;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;

/**
 * Represents a pending suggestion for manual mode approval.
 * 
 * When NOZH is in manual mode, instead of auto-applying actions,
 * it queues suggestions that the user can approve or dismiss.
 * 
 * PRIORITY 2 - Manual Mode with Confirmation (Task 3)
 */
public record PendingSuggestion(
    CapabilityId capability,
    CapabilityValue suggestedValue,
    String reason,
    long createdAt,
    long expiresAt
) {
    /**
     * Default timeout for suggestions: 60 seconds.
     */
    public static final long DEFAULT_TIMEOUT_MS = 60_000;

    /**
     * Create a new pending suggestion with default timeout.
     * 
     * @param capability The capability to change
     * @param suggestedValue The suggested new value
     * @param reason Human-readable reason for the suggestion
     * @return A new PendingSuggestion
     */
    public static PendingSuggestion create(
            CapabilityId capability,
            CapabilityValue suggestedValue,
            String reason) {
        long now = System.currentTimeMillis();
        return new PendingSuggestion(
            capability,
            suggestedValue,
            reason,
            now,
            now + DEFAULT_TIMEOUT_MS
        );
    }

    /**
     * Create a new pending suggestion with custom timeout.
     * 
     * @param capability The capability to change
     * @param suggestedValue The suggested new value
     * @param reason Human-readable reason for the suggestion
     * @param timeoutMs Custom timeout in milliseconds
     * @return A new PendingSuggestion
     */
    public static PendingSuggestion create(
            CapabilityId capability,
            CapabilityValue suggestedValue,
            String reason,
            long timeoutMs) {
        long now = System.currentTimeMillis();
        return new PendingSuggestion(
            capability,
            suggestedValue,
            reason,
            now,
            now + timeoutMs
        );
    }

    /**
     * Check if this suggestion has expired.
     * 
     * @return true if current time > expiresAt
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    /**
     * Get time remaining in milliseconds.
     * 
     * @return milliseconds until expiration, or 0 if already expired
     */
    public long timeRemainingMs() {
        long remaining = expiresAt - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    /**
     * Get time remaining in seconds.
     * 
     * @return seconds until expiration, or 0 if already expired
     */
    public int timeRemainingSeconds() {
        return (int) (timeRemainingMs() / 1000);
    }

    /**
     * Get formatted display string for HUD.
     * 
     * @return Formatted string like "RENDER_DISTANCE → 8 (Low FPS detected, 45s)"
     */
    public String toDisplayString() {
        String valueStr = formatValue(suggestedValue);
        return String.format("%s → %s (%s, %ds)",
            capability.name(),
            valueStr,
            reason,
            timeRemainingSeconds()
        );
    }

    /**
     * Format CapabilityValue for display.
     */
    private static String formatValue(CapabilityValue value) {
        return switch (value) {
            case CapabilityValue.IntValue iv -> String.valueOf(iv.value());
            case CapabilityValue.EnumValue ev -> ev.name();
            case CapabilityValue.BoolValue bv -> bv.value() ? "ON" : "OFF";
            case CapabilityValue.FloatValue fv -> String.format("%.2f", fv.value());
        };
    }
}
