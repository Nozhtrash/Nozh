package dev.nozh.core.governor.components;

import dev.nozh.NozhConstants;
import java.util.concurrent.*;

/**
 * Manages asynchronous action execution.
 * 
 * Simplified version that works with existing main branch code.
 * 
 * @author Nozh Team
 * @since 0.4.0
 */
public final class ActionExecutor {
    
    private final ScheduledExecutorService asyncExecutor;
    private final ConcurrentHashMap<String, CompletableFuture<ActionResult>> pendingActions;
    
    /**
     * Constructs a new ActionExecutor.
     */
    public ActionExecutor() {
        this.asyncExecutor = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "ActionExecutor-Async");
            t.setDaemon(true);
            return t;
        });
        
        this.pendingActions = new ConcurrentHashMap<>();
        
        NozhConstants.LOGGER.info("ActionExecutor initialized");
    }
    
    /**
     * Executes an action asynchronously.
     * 
     * @param actionId action ID
     * @param executionLogic logic to execute
     */
    public void executeAsync(String actionId, Runnable executionLogic) {
        if (actionId == null || executionLogic == null) {
            NozhConstants.LOGGER.error("Cannot execute action with null parameters");
            return;
        }
        
        if (pendingActions.containsKey(actionId)) {
            NozhConstants.LOGGER.warn("Action already pending: {}", actionId);
            return;
        }
        
        long startTime = System.currentTimeMillis();
        
        CompletableFuture<ActionResult> future = CompletableFuture.supplyAsync(() -> {
            try {
                executionLogic.run();
                return new ActionResult(true, startTime);
            } catch (Exception e) {
                NozhConstants.LOGGER.error("Action execution failed: {}", actionId, e);
                return new ActionResult(false, startTime);
            }
        }, asyncExecutor);
        
        asyncExecutor.schedule(() -> {
            try {
                ActionResult result = future.get();
                long duration = System.currentTimeMillis() - result.startTime;
                NozhConstants.LOGGER.debug("Action {} completed in {}ms (success={})",
                        actionId, duration, result.executionSuccess);
            } catch (Exception e) {
                NozhConstants.LOGGER.error("Failed to measure action results: {}", actionId, e);
            } finally {
                pendingActions.remove(actionId);
            }
        }, 1000, TimeUnit.MILLISECONDS);
        
        pendingActions.put(actionId, future);
    }
    
    /**
     * Gets the number of pending actions.
     */
    public int getPendingCount() {
        return pendingActions.size();
    }
    
    /**
     * Shuts down the executor.
     */
    public void shutdown() {
        asyncExecutor.shutdown();
        try {
            if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        pendingActions.clear();
        NozhConstants.LOGGER.info("ActionExecutor shutdown complete");
    }
    
    /**
     * Simple result holder for async action execution.
     */
    private static class ActionResult {
        final boolean executionSuccess;
        final long startTime;
        
        ActionResult(boolean executionSuccess, long startTime) {
            this.executionSuccess = executionSuccess;
            this.startTime = startTime;
        }
    }
}
