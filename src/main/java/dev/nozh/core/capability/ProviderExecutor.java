package dev.nozh.core.capability;

import dev.nozh.NozhConstants;
import dev.nozh.core.bus.CapabilityValue;
import net.minecraft.client.MinecraftClient;
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
 * <p>
 * THREAD SAFETY FIX (Audit 2026):
 * All provider executions are now strictly marshalled to the Minecraft Main
 * Thread.
 * Background threads only handle resolution and logging.
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
     * <p>
     * Execution flow:
     * 1. Resolution (Async)
     * 2. Execution (Main Thread)
     * 3. Result handling (Async)
     *
     * @param actionId Action ID from learning engine
     * @return CompletableFuture with execution result
     */
    public CompletableFuture<ExecutionResult> executeAction(String actionId) {
        return CompletableFuture.supplyAsync(() -> {
            // Step 1: Resolve action to provider ID (Safe off-thread)
            long startTime = System.nanoTime();
            try {
                Optional<String> providerIdOpt = ActionProviderMapping.getProviderIdForAction(actionId);
                if (providerIdOpt.isEmpty()) {
                    LOGGER.warn("No provider mapping for action: {}", actionId);
                    return new ResolutionResult(null, startTime, "No provider mapping");
                }

                String providerId = providerIdOpt.get();
                OptimizationProvider provider = registry.getProvider(providerId);

                if (provider == null) {
                    LOGGER.warn("Provider not registered: {} (for action: {})", providerId, actionId);
                    return new ResolutionResult(null, startTime, "Provider not registered: " + providerId);
                }

                return new ResolutionResult(provider, startTime, null);
            } catch (Exception e) {
                return new ResolutionResult(null, startTime, "Resolution error: " + e.getMessage());
            }
        }, asyncExecutor).thenCompose(resolution -> {
            // Step 2: Switch to Main Thread for execution
            if (resolution.provider == null) {
                return CompletableFuture.completedFuture(
                        new ExecutionResult(false, resolution.startTime, resolution.error));
            }

            CompletableFuture<ExecutionResult> mainThreadFuture = new CompletableFuture<>();
            MinecraftClient client = MinecraftClient.getInstance();

            if (client == null) {
                return CompletableFuture.completedFuture(
                        new ExecutionResult(false, resolution.startTime, "Client is null"));
            }

            client.execute(() -> {
                try {
                    // Double-check thread validily just in case
                    if (!client.isOnThread()) {
                        LOGGER.error("CRITICAL: Failed to schedule on main thread for {}", actionId);
                        mainThreadFuture
                                .complete(new ExecutionResult(false, resolution.startTime, "Thread scheduling failed"));
                        return;
                    }

                    OptimizationProvider provider = resolution.provider;
                    // Check availability on main thread (safe to touch options)
                    if (!provider.canExecute()) {
                        LOGGER.debug("Provider cannot execute (checked on main thread): {}", provider.getId());
                        mainThreadFuture
                                .complete(new ExecutionResult(false, resolution.startTime, "Provider cannot execute"));
                        return;
                    }

                    LOGGER.info("Executing provider on Main Thread: {}", provider.getId());
                    boolean success = provider.execute();

                    if (success) {
                        mainThreadFuture.complete(new ExecutionResult(true, resolution.startTime, "Success"));
                    } else {
                        mainThreadFuture
                                .complete(new ExecutionResult(false, resolution.startTime, "Execution returned false"));
                    }
                } catch (Exception e) {
                    LOGGER.error("Exception during provider execution on Main Thread: {}", actionId, e);
                    mainThreadFuture
                            .complete(new ExecutionResult(false, resolution.startTime, "Exception: " + e.getMessage()));
                }
            });

            return mainThreadFuture;
        });
    }

    /**
     * Execute a CapabilityProvider directly with a specific value.
     */
    public CompletableFuture<ExecutionResult> executeCapabilityProvider(
            CapabilityProvider provider,
            CapabilityValue value) {
        return CompletableFuture.supplyAsync(() -> {
            // Pre-check basic availability off-thread if possible, or just pass through
            return System.nanoTime();
        }, asyncExecutor).thenCompose(startTime -> {
            CompletableFuture<ExecutionResult> mainThreadFuture = new CompletableFuture<>();
            MinecraftClient client = MinecraftClient.getInstance();

            if (client == null) {
                return CompletableFuture.completedFuture(
                        new ExecutionResult(false, startTime, "Client is null"));
            }

            client.execute(() -> {
                try {
                    String providerName = provider.id().name();

                    if (!provider.isAvailable()) {
                        mainThreadFuture.complete(new ExecutionResult(false, startTime, "Provider not available"));
                        return;
                    }

                    LOGGER.info("Executing capability provider on Main Thread: {}", providerName);
                    ApplyResult result = provider.apply(value);

                    if (result instanceof ApplyResult.Success) {
                        mainThreadFuture.complete(new ExecutionResult(true, startTime, "Success"));
                    } else {
                        mainThreadFuture.complete(new ExecutionResult(false, startTime, "Apply failed"));
                    }
                } catch (Exception e) {
                    LOGGER.error("Exception during capability provider execution", e);
                    mainThreadFuture.complete(new ExecutionResult(false, startTime, "Exception: " + e.getMessage()));
                }
            });

            return mainThreadFuture;
        });
    }

    // Process-local helper
    private record ResolutionResult(OptimizationProvider provider, long startTime, String error) {
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