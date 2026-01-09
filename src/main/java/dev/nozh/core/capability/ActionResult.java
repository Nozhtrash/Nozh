package dev.nozh.core.capability;

import dev.nozh.core.state.StateSnapshot;

/**
 * Result of a capability provider action execution.
 * Encapsulates success status, error messages, and state snapshots for rollback.
 * 
 * <p>Thread-safe immutable result object.
 * 
 * @author Nozh Team
 * @since 0.5.0 (Phase 1 Sprint 1)
 */
public final class ActionResult {
    private final boolean success;
    private final String errorMessage;
    private final StateSnapshot snapshot;
    private final String method; // "Sodium" or "Vanilla"
    
    private ActionResult(boolean success, String errorMessage, StateSnapshot snapshot, String method) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.snapshot = snapshot;
        this.method = method;
    }
    
    public static ActionResult success(StateSnapshot snapshot) {
        return new ActionResult(true, null, snapshot, "Vanilla");
    }
    
    public static ActionResult success(StateSnapshot snapshot, String method) {
        return new ActionResult(true, null, snapshot, method);
    }
    
    public static ActionResult error(String errorMessage) {
        return new ActionResult(false, errorMessage, null, null);
    }
    
    public static ActionResult invalid(String errorMessage) {
        return new ActionResult(false, errorMessage, null, null);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getError() {
        return errorMessage;
    }
    
    public StateSnapshot getSnapshot() {
        return snapshot;
    }
    
    public String getMethod() {
        return method != null ? method : "Unknown";
    }
    
    @Override
    public String toString() {
        if (success) {
            return "ActionResult[SUCCESS, method=" + method + "]";
        } else {
            return "ActionResult[FAILED: " + errorMessage + "]";
        }
    }
}
