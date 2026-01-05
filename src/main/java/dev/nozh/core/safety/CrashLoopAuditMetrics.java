package dev.nozh.core.safety;

import java.util.EnumSet;
import java.util.Set;

/**
 * Audit-friendly crash loop and safe mode metrics snapshot.
 */
public record CrashLoopAuditMetrics(
        int bootAttempts,
        boolean sessionStable,
        boolean safeModeActive,
        String safeModeReason,
        Set<SafeModeCause> safeModeCauses,
        long safeModeActivatedAt,
        long lastCleanShutdown,
        long sessionStartTime,
        int ticksSinceStart,
        boolean initialized,
        boolean crashLoopDetected) {

    public static CrashLoopAuditMetrics empty(int ticksSinceStart, boolean initialized) {
        return new CrashLoopAuditMetrics(
                0,
                false,
                false,
                "off",
                EnumSet.noneOf(SafeModeCause.class),
                0L,
                0L,
                0L,
                ticksSinceStart,
                initialized,
                false);
    }

    public static CrashLoopAuditMetrics fromState(NozhState state, int ticksSinceStart, boolean initialized,
            boolean crashLoopDetected) {
        Set<SafeModeCause> causes = state.safeModeCauses != null && !state.safeModeCauses.isEmpty()
                ? EnumSet.copyOf(state.safeModeCauses)
                : EnumSet.noneOf(SafeModeCause.class);
        return new CrashLoopAuditMetrics(
                state.bootAttempts,
                state.sessionStable,
                state.isSafeModeActive(),
                state.getSafeModeReason(),
                causes,
                state.safeModeActivatedAt,
                state.lastCleanShutdown,
                state.sessionStartTime,
                ticksSinceStart,
                initialized,
                crashLoopDetected);
    }
}
