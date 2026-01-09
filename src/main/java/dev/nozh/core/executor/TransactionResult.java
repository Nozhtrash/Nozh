package dev.nozh.core.executor;

import dev.nozh.core.telemetry.TelemetrySnapshot;

/**
 * Result of a transactional action execution.
 * 
 * @author Nozh Team
 * @since 0.5.0 (Phase 1 Sprint 2)
 */
public final class TransactionResult {
    private final boolean success;
    private final boolean rolledBack;
    private final String message;
    private final TelemetrySnapshot afterSnapshot;
    
    private TransactionResult(boolean success, boolean rolledBack, String message, TelemetrySnapshot afterSnapshot) {
        this.success = success;
        this.rolledBack = rolledBack;
        this.message = message;
        this.afterSnapshot = afterSnapshot;
    }
    
    public static TransactionResult success(TelemetrySnapshot afterSnapshot) {
        return new TransactionResult(true, false, "Transaction successful", afterSnapshot);
    }
    
    public static TransactionResult rolledBack(String reason) {
        return new TransactionResult(false, true, reason, null);
    }
    
    public static TransactionResult failed(String error) {
        return new TransactionResult(false, false, error, null);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public boolean wasRolledBack() {
        return rolledBack;
    }
    
    public String getMessage() {
        return message;
    }
    
    public TelemetrySnapshot getAfterSnapshot() {
        return afterSnapshot;
    }
    
    @Override
    public String toString() {
        if (success) {
            return "TransactionResult[SUCCESS]";
        } else if (rolledBack) {
            return "TransactionResult[ROLLED_BACK: " + message + "]";
        } else {
            return "TransactionResult[FAILED: " + message + "]";
        }
    }
}
