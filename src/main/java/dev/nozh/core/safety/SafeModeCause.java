package dev.nozh.core.safety;

/**
 * Reasons why safe mode can be active.
 * 
 * Phase 2 Iteration 1: Replaces 3 separate booleans with extensible enum set.
 * 
 * Priority order (for getPrimaryCause):
 * 1. CONFIG_FORCE - highest priority (admin decision)
 * 2. CRASH_LOOP - detected instability
 * 3. USER_ENABLED - manual user toggle
 * 
 * Future causes can be added without breaking changes.
 */
public enum SafeModeCause {
    /**
     * Safe mode forced by config (safeModeForce = true).
     * Highest priority - cannot be overridden by user.
     */
    CONFIG_FORCE(1),

    /**
     * Safe mode activated due to repeated crashes.
     * Resets when user explicitly resets safe mode.
     */
    CRASH_LOOP(2),

    /**
     * Safe mode manually enabled by user via command/GUI.
     * Lowest priority.
     */
    USER_ENABLED(3);

    /**
     * Priority for determining primary cause (lower = higher priority).
     */
    private final int priority;

    SafeModeCause(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    /**
     * Human-readable reason string for UX.
     */
    public String getReasonText() {
        return switch (this) {
            case CONFIG_FORCE -> "config forced";
            case CRASH_LOOP -> "crash loop";
            case USER_ENABLED -> "user enabled";
        };
    }
}
