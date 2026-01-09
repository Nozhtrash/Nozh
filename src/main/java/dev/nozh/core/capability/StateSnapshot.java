package dev.nozh.core.capability;

/**
 * State snapshot for capability rollback.
 * 
 * Stores the previous state of a game setting before modification,
 * allowing safe rollback if an action needs to be reverted.
 * 
 * @since v0.2.0-alpha
 */
public record StateSnapshot(
        String actionId,
        Object previousValue,
        long timestamp) {
    
    /**
     * Create a new state snapshot.
     * 
     * @param actionId the action that was executed
     * @param previousValue the value before modification
     * @param timestamp when the snapshot was taken (System.currentTimeMillis())
     */
    public StateSnapshot {
        if (actionId == null || actionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Action ID cannot be null or empty");
        }
    }
    
    /**
     * Create a snapshot for the current time.
     * 
     * @param actionId the action that was executed
     * @param previousValue the value before modification
     * @return new snapshot
     */
    public static StateSnapshot of(String actionId, Object previousValue) {
        return new StateSnapshot(actionId, previousValue, System.currentTimeMillis());
    }
}