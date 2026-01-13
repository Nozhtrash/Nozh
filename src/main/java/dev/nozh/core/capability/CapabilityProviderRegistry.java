package dev.nozh.core.capability;

import dev.nozh.NozhConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.client.option.CloudRenderMode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for all capability providers.
 * 
 * @deprecated This is the LEGACY provider registry. The modern system uses
 *             {@link ProviderRegistry} with {@link OptimizationProvider}
 *             implementations.
 *             See ProviderRegistry.discoverProviders() for the active provider
 *             registration.
 * 
 *             Providers are the actual implementations that modify game
 *             settings.
 *             This registry manages their lifecycle and provides safe
 *             execution.
 * 
 * @since 1.0.0
 */
public class CapabilityProviderRegistry {

    private static final Map<String, CapabilityProvider> providers = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    /**
     * Initialize all built-in providers.
     * Should be called during mod initialization.
     * 
     * NOTE: This legacy registry is no longer actively used.
     * The new system uses ProviderRegistry with OptimizationProvider.
     * See IntegratedGovernor constructor for the modern initialization.
     */
    public static synchronized void initialize() {
        if (initialized) {
            NozhConstants.LOGGER.warn("CapabilityProviderRegistry already initialized");
            return;
        }

        try {
            // Legacy registry - providers have been migrated to ProviderRegistry
            // See dev.nozh.core.capability.providers package for modern providers

            initialized = true;
            NozhConstants.LOGGER
                    .info("CapabilityProviderRegistry initialized (legacy API - use ProviderRegistry instead)");

        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to initialize CapabilityProviderRegistry", e);
            throw new RuntimeException("Critical: Provider registry initialization failed", e);
        }
    }

    /**
     * Register a custom capability provider.
     * 
     * @param actionId unique identifier for the action
     * @param provider the provider implementation
     * @throws IllegalArgumentException if actionId is null, empty, or already
     *                                  registered
     * @throws NullPointerException     if provider is null
     */
    public static void register(String actionId, CapabilityProvider provider) {
        if (actionId == null || actionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Action ID cannot be null or empty");
        }
        if (provider == null) {
            throw new NullPointerException("Provider cannot be null");
        }

        if (providers.containsKey(actionId)) {
            NozhConstants.LOGGER.warn("Overwriting existing provider: {}", actionId);
        }

        providers.put(actionId, provider);
        NozhConstants.LOGGER.debug("Registered provider: {}", actionId);
    }

    /**
     * Execute an action with the registered provider.
     * 
     * @deprecated Use {@link ProviderRegistry} with {@link ProviderExecutor}
     *             instead.
     *             The new system uses OptimizationProvider implementations in the
     *             providers/ package.
     * 
     * @param actionId the action to execute
     * @param client   Minecraft client instance
     * @param params   additional parameters for the action
     * @return ActionResult indicating success or failure
     */
    public static ActionResult execute(String actionId, MinecraftClient client, Object... params) {
        NozhConstants.LOGGER.warn("CapabilityProviderRegistry.execute() is deprecated and disabled");
        NozhConstants.LOGGER.warn("Please migrate to new CapabilityProvider.apply() API");
        return ActionResult.error("Method deprecated - needs migration");

        return ActionResult.error("Method completely disabled. Use ProviderRegistry.");
    }

    /**
     * Restore a setting to a previous value.
     * Used by rollback system.
     * 
     * @deprecated Use the rollback mechanism in {@link ProviderExecutor} instead.
     * 
     * @param actionId the action that was executed
     * @param snapshot the snapshot containing the old value
     * @return true if restored successfully
     */
    public static boolean restore(String actionId, StateSnapshot snapshot) {
        NozhConstants.LOGGER.warn("CapabilityProviderRegistry.restore() is deprecated and disabled");
        NozhConstants.LOGGER.warn("Please migrate to new rollback mechanism");
        if (snapshot != null) {
            // Mark snapshot as intentionally unused while implementation is disabled
            snapshot.hashCode();
        }
        return false;

        /*
         * TEMPORARILY DISABLED - NEEDS MIGRATION TO NEW API
         * if (actionId == null || snapshot == null) {
         * NozhConstants.LOGGER.error("Cannot restore with null parameters");
         * return false;
         * }
         * 
         * CapabilityProvider provider = providers.get(actionId);
         * if (provider == null) {
         * NozhConstants.LOGGER.warn("Cannot restore unknown action: {}", actionId);
         * return false;
         * }
         * 
         * // TODO: Check if provider supports rollback in new API
         * // if (!provider.supportsRollback()) {
         * // NozhConstants.LOGGER.warn("Provider {} does not support rollback",
         * actionId);
         * // return false;
         * // }
         * 
         * try {
         * // TODO: Implement rollback with new API
         * // provider.rollback(snapshot);
         * NozhConstants.LOGGER.info("Rolled back action: {}", actionId);
         * return true;
         * } catch (Exception e) {
         * NozhConstants.LOGGER.error("Failed to rollback action: {}", actionId, e);
         * return false;
         * }
         */
    }

    /**
     * Check if a provider exists for the given action.
     * 
     * @param actionId the action to check
     * @return true if provider exists
     */
    public static boolean hasProvider(String actionId) {
        return providers.containsKey(actionId);
    }

    /**
     * Get all registered action IDs.
     * 
     * @return array of action IDs
     */
    public static String[] getRegisteredActions() {
        return providers.keySet().toArray(new String[0]);
    }

    /**
     * Check if registry is initialized.
     * 
     * @return true if initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Clear all providers (for testing).
     */
    static void clearForTesting() {
        providers.clear();
        initialized = false;
    }
}
