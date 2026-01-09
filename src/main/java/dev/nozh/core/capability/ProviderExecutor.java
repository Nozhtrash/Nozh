package dev.nozh.core.capability;

import dev.nozh.NozhConstants;
import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Executes optimization providers safely with error handling and logging.
 * <p>
 * Updated to support BOTH:
 * - OptimizationProvider (new interface)
 * - CapabilityProvider (legacy via adapter)
 */
public final class ProviderExecutor {

    private static final Logger LOGGER = NozhConstants.LOGGER;
    private final ProviderRegistry registry;
    private final Executor asyncExecutor;

    public ProviderExecutor(ProviderRegistry registry, Executor asyncExecutor) {
        this.registry = registry;
        this.asyncExecutor = asyncExecutor;
    }

    /**
     * Execute an action by finding and invoking its corresponding provider.
     *
     * @param actionId Action ID from learning engine
     * @return CompletableFuture with execution result
     */
    public CompletableFuture<ExecutionResult> executeAction(String actionId) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.nanoTime();

            try {
                // Step 1: Resolve action to provider ID
                Optional<String> providerIdOpt = ActionProviderMapping.getProviderIdForAction(actionId);
                if (providerIdOpt.isEmpty()) {
                    LOGGER.warn("No provider mapping for action: {}", actionId);
                    return new ExecutionResult(false, startTime, "No provider mapping");
                }

                String providerId = providerIdOpt.get();

                // Step 2: Get provider from registry
                OptimizationProvider provider = registry.getProvider(providerId);
                if (provider == null) {
                    LOGGER.warn("Provider not registered: {} (for action: {})", providerId, actionId);
                    return new ExecutionResult(false, startTime, "Provider not registered: " + providerId);
                }

                // Step 3: Check if provider can execute
                if (!provider.canExecute()) {
                    LOGGER.debug("Provider cannot execute: {} (for action: {})", providerId, actionId);
                    return new ExecutionResult(false, startTime, "Provider cannot execute");
                }

                // Step 4: Execute provider
                LOGGER.info("Executing provider: {} (for action: {})", providerId, actionId);
                boolean success = provider.execute();

                if (success) {
                    LOGGER.info("✓ Provider executed successfully: {}", providerId);
                    return new ExecutionResult(true, startTime, "Success");
                } else {
                    LOGGER.warn("✗ Provider execution returned false: {}", providerId);
                    return new ExecutionResult(false, startTime, "Execution returned false");
                }

            } catch (Exception e) {
                LOGGER.error("Exception during provider execution for action: {}", actionId, e);
                return new ExecutionResult(false, startTime, "Exception: " + e.getMessage());
            }
        }, asyncExecutor);
    }

    /**
     * Execute a CapabilityProvider directly with a specific value.
     * <p>
     * This is for advanced usage where you want to bypass the action mapping.
     *
     * @param provider CapabilityProvider to execute
     * @param value    Value to apply
     * @return CompletableFuture with execution result
     */
    public CompletableFuture<ExecutionResult> executeCapabilityProvider(
            CapabilityProvider provider,
            CapabilityValue value
    ) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.nanoTime();

            try {
                String providerName = provider.id().name();

                if (!provider.isAvailable()) {
                    return new ExecutionResult(false, startTime, "Provider not available");
                }

                LOGGER.info("Executing capability provider: {}", providerName);
                ApplyResult result = provider.apply(value);

                if (result instanceof ApplyResult.Success) {
                    LOGGER.info("✓ Capability provider executed successfully: {}", providerName);
                    return new ExecutionResult(true, startTime, "Success");
                } else {
                    LOGGER.warn("✗ Capability provider failed: {}", providerName);
                    return new ExecutionResult(false, startTime, "Apply failed");
                }

            } catch (Exception e) {
                LOGGER.error("Exception during capability provider execution", e);
                return new ExecutionResult(false, startTime, "Exception: " + e.getMessage());
            }
        }, asyncExecutor);
    }

    /**
     * Result of provider execution.
     */
    public static class ExecutionResult {
        private final boolean success;
        private final long startTimeNanos;
        private final String message;

        public ExecutionResult(boolean success, long startTimeNanos, String message) {
            this.success = success;
            this.startTimeNanos = startTimeNanos;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public long getExecutionTimeMs() {
            return (System.nanoTime() - startTimeNanos) / 1_000_000;
        }
        
        public String getMessage() {
            return message;
        }
    }
}