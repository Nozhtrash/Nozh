package dev.nozh.core.capability;

import dev.nozh.core.NozhConstants;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Collection;
import java.util.Collections;

/**
 * Registry for all optimization capability providers.
 * <p>
 * Thread-safe central storage for providers that can be queried
 * by ID to execute optimization actions.
 */
public final class ProviderRegistry {

    private static final Logger LOGGER = NozhConstants.LOGGER;
    private final Map<String, OptimizationProvider> providers = new ConcurrentHashMap<>();

    /**
     * Register a provider.
     *
     * @param provider Provider to register
     */
    public void register(OptimizationProvider provider) {
        if (provider == null) {
            LOGGER.warn("Attempted to register null provider");
            return;
        }

        String id = provider.getId();
        if (id == null || id.trim().isEmpty()) {
            LOGGER.warn("Provider has null or empty ID: {}", provider.getClass().getSimpleName());
            return;
        }

        if (providers.containsKey(id)) {
            LOGGER.warn("Provider already registered: {} (overwriting)", id);
        }

        providers.put(id, provider);
        LOGGER.debug("Registered provider: {} ({})", id, provider.getName());
    }

    /**
     * Get a provider by its ID.
     *
     * @param providerId Provider ID
     * @return Provider, or null if not found
     */
    public OptimizationProvider getProvider(String providerId) {
        return providers.get(providerId);
    }

    /**
     * Check if a provider is registered.
     *
     * @param providerId Provider ID
     * @return true if provider exists
     */
    public boolean hasProvider(String providerId) {
        return providers.containsKey(providerId);
    }

    /**
     * Get all registered providers.
     *
     * @return Unmodifiable collection of providers
     */
    public Collection<OptimizationProvider> getAllProviders() {
        return Collections.unmodifiableCollection(providers.values());
    }

    /**
     * Get count of registered providers.
     *
     * @return Number of providers
     */
    public int getProviderCount() {
        return providers.size();
    }

    /**
     * Clear all registered providers.
     * <p>
     * Primarily for testing.
     */
    public void clear() {
        int count = providers.size();
        providers.clear();
        LOGGER.info("Cleared {} providers from registry", count);
    }
}