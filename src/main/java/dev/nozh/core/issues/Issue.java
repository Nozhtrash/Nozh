package dev.nozh.core.issues;

/**
 * Issue record (Contract 9).
 * 
 * Structured diagnostic - NOT a log message.
 * Aggregated by type to avoid spam.
 */
public record Issue(
        IssueType type,
        IssueSeverity severity,
        String reasonKey, // i18n key (e.g., "nozh.issue.provider.degraded")
        long firstSeenMillis,
        long lastSeenMillis,
        int occurrences) {
    /**
     * Create new issue (first occurrence).
     */
    public static Issue create(IssueType type, IssueSeverity severity, String reasonKey, long nowMillis) {
        return new Issue(type, severity, reasonKey, nowMillis, nowMillis, 1);
    }

    /**
     * Update existing issue with new occurrence.
     */
    public Issue withOccurrence(long nowMillis) {
        return new Issue(type, severity, reasonKey, firstSeenMillis, nowMillis, occurrences + 1);
    }

    /**
     * Check if this issue matches another (same type + reason).
     */
    public boolean matches(Issue other) {
        return this.type == other.type && this.reasonKey.equals(other.reasonKey);
    }
}
