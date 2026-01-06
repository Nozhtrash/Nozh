package dev.nozh.core.safety;

/**
 * Decision record for crash-loop recovery actions.
 */
public record CrashRecoveryDecision(
        CrashRecoveryAction action,
        String capabilityId,
        long retryAtMillis,
        String reason) {

    public static CrashRecoveryDecision none() {
        return new CrashRecoveryDecision(CrashRecoveryAction.NONE, null, 0L, "");
    }

    public static CrashRecoveryDecision quarantined(String capabilityId, long retryAtMillis, String reason) {
        return new CrashRecoveryDecision(CrashRecoveryAction.QUARANTINED_CAPABILITY, capabilityId, retryAtMillis, reason);
    }

    public static CrashRecoveryDecision safeMode(String reason) {
        return new CrashRecoveryDecision(CrashRecoveryAction.SAFE_MODE, null, 0L, reason != null ? reason : "");
    }
}
