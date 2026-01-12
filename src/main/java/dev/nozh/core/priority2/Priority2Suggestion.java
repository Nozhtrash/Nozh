package dev.nozh.core.priority2;

/**
 * v0.2: A human-readable suggestion that can be shown in HUD and confirmed manually.
 */
public final class Priority2Suggestion {

    public enum Severity {
        INFO,
        RECOMMENDED,
        URGENT
    }

    public final String id;
    public final String reason;
    public final Severity severity;

    public Priority2Suggestion(String id, String reason, Severity severity) {
        this.id = id;
        this.reason = reason;
        this.severity = severity == null ? Severity.RECOMMENDED : severity;
    }
}
