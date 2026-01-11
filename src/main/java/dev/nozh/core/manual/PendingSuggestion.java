package dev.nozh.core.manual;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;

/**
 * Represents a pending optimization suggestion in manual mode.
 * 
 * Manual mode allows the user to review and approve/reject suggestions
 * before they are applied.
 */
public record PendingSuggestion(
    CapabilityId capability,
    CapabilityValue suggestedValue,
    String reason,
    long createdAt,
    long expiresAt
) {
    public static final long DEFAULT_TIMEOUT_MS = 60_000;
    
    public static PendingSuggestion create(CapabilityId cap, CapabilityValue val, String reason) {
        long now = System.currentTimeMillis();
        return new PendingSuggestion(cap, val, reason, now, now + DEFAULT_TIMEOUT_MS);
    }
    
    public boolean isExpired() { return System.currentTimeMillis() > expiresAt; }
    public long timeRemainingMs() { return Math.max(0, expiresAt - System.currentTimeMillis()); }
    public int timeRemainingSeconds() { return (int)(timeRemainingMs() / 1000); }
    
    public String toDisplayString() {
        return String.format("%s → %s (%s, %ds)", 
            capability.name(), formatValue(suggestedValue), reason, timeRemainingSeconds());
    }
    
    private static String formatValue(CapabilityValue value) {
        if (value instanceof CapabilityValue.IntValue iv) return String.valueOf(iv.value());
        if (value instanceof CapabilityValue.EnumValue ev) return ev.name();
        if (value instanceof CapabilityValue.BoolValue bv) return bv.value() ? "ON" : "OFF";
        if (value instanceof CapabilityValue.FloatValue fv) return String.format("%.2f", fv.value());
        return value.toString();
    }
}
