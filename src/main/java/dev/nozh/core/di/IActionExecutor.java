package dev.nozh.core.di;

/**
 * Interface for action execution - enables DI.
 * 
 * Simplified version using only basic types to avoid compilation issues.
 */
public interface IActionExecutor {
    /**
     * Executes an action asynchronously.
     * 
     * @param actionId action identifier
     * @param executionLogic logic to execute
     */
    void executeAsync(String actionId, Runnable executionLogic);
    
    /**
     * Gets the number of pending actions.
     * 
     * @return pending count
     */
    int getPendingCount();
    
    /**
     * Shuts down the executor.
     */
    void shutdown();
}
