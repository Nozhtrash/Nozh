package dev.nozh.core.capability;

/**
 * Result of executing a capability action.
 * Contains status, error message, and snapshot for rollback.
 */
public class ActionResult {
    
    public enum Status {
        SUCCESS,        // Action executed successfully
        ERROR,          // Action failed due to error
        INVALID,        // Invalid parameters
        NO_CHANGE       // Action was no-op (already at target)
    }
    
    private final Status status;
    private final String message;
    private final StateSnapshot snapshot;
    
    private ActionResult(Status status, String message, StateSnapshot snapshot) {
        this.status = status;
        this.message = message;
        this.snapshot = snapshot;
    }
    
    public static ActionResult success(StateSnapshot snapshot) {
        return new ActionResult(Status.SUCCESS, "Success", snapshot);
    }
    
    public static ActionResult error(String message) {
        return new ActionResult(Status.ERROR, message, null);
    }
    
    public static ActionResult invalid(String message) {
        return new ActionResult(Status.INVALID, message, null);
    }
    
    public static ActionResult noChange(String message) {
        return new ActionResult(Status.NO_CHANGE, message, null);
    }
    
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
    
    public boolean canRollback() {
        return snapshot != null;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public String getError() {
        return status != Status.SUCCESS ? message : null;
    }
    
    public StateSnapshot getSnapshot() {
        return snapshot;
    }
    
    @Override
    public String toString() {
        return String.format("ActionResult{status=%s, message='%s', hasSnapshot=%s}",
                           status, message, snapshot != null);
    }
}
