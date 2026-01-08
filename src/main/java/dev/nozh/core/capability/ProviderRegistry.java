package dev.nozh.core.capability;

import dev.nozh.core.bus.CapabilityId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provider registry (Contract 3).
 * 
 * Central registry for all CapabilityProviders AND OptimizationProviders.
 * 
 * CONTRACT RULES:
 * - Explicit registration only (no reflection scan)
 * - BROKEN providers excluded by default
 * - Thread-safe registration and lookup
 * - One broken provider MUST NOT crash registry
 * 
 * DUAL SUPPORT:
 * - CapabilityProvider: Legacy contract-based providers (registered by CapabilityId)
 * - OptimizationProvider: Modern optimization providers (registered by String ID)
 * 
 * DETERMINISTIC BUILDS:
 * Manual provider list prevents classpath surprises.
 * 
 * @since 0.4.0 - Added OptimizationProvider support
 */
public final class ProviderRegistry {

    // Legacy: CapabilityProvider by CapabilityId
    private final Map<CapabilityId, CapabilityProvider> capabilityProviders = new ConcurrentHashMap<>();
    
    // Modern: OptimizationProvider by String ID
    private final Map<String, OptimizationProvider> optimizationProviders = new ConcurrentHashMap<>();
    
    private final ProviderHealthTracker healthTracker;

    public ProviderRegistry(ProviderHealthTracker healthTracker) {
        this.healthTracker = healthTracker;
    }

    /**
     * Register a CapabilityProvider (legacy).
     * 
     * If provider init throws, it is marked BROKEN and excluded.
     * Registry continues to function.
     * 
     * @param provider Provider to register
     */
    public void register(CapabilityProvider provider) {
        try {
            CapabilityId id = provider.id();

            // Check availability
            if (!provider.isAvailable()) {
                healthTracker.markBroken(id, "Provider not available (mod/version check failed)");
                return; // Don't register unavailable providers
            }

            // Check current status
            ProviderStatus status = provider.status();
            if (status == ProviderStatus.BROKEN) {
                healthTracker.markBroken(id, provider.statusReason().orElse("Provider self-reported BROKEN"));
                return; // Don't register broken providers
            }

            // Register
            capabilityProviders.put(id, provider);
            healthTracker.markHealthy(id);

        } catch (Exception e) {
            // Provider threw during registration -> mark BROKEN, continue
            try {
                CapabilityId id = provider.id(); // May throw again, catch below
                healthTracker.markBroken(id, "Registration threw: " + e.getMessage());
            } catch (Exception idException) {
                // Can't even get ID -> log and skip
                // (In production, would use logger here)
            }
        }
    }

    /**
     * Register an OptimizationProvider (modern).
     * 
     * @param provider OptimizationProvider to register
     * @since 0.4.0
     */
    public void register(OptimizationProvider provider) {
        if (provider == null) {
            return;
        }
        
        try {
            String id = provider.getId();
            if (id == null || id.isEmpty()) {
                return;
            }
            
            // Check if provider can execute
            if (!provider.canExecute()) {
                // Don't register if provider can't execute
                return;
            }
            
            optimizationProviders.put(id.toLowerCase(), provider);
            
        } catch (Exception e) {
            // Provider threw during registration -> skip
        }
    }

    /**
     * Get a CapabilityProvider by capability ID.
     * 
     * @param id Capability ID
     * @return Provider, or empty if not registered or BROKEN
     */
    public Optional<CapabilityProvider> get(CapabilityId id) {
        // Exclude BROKEN providers
        if (healthTracker.isBroken(id)) {
            return Optional.empty();
        }

        return Optional.ofNullable(capabilityProviders.get(id));
    }

    /**
     * Get an OptimizationProvider by string ID.
     * 
     * @param id Provider ID (e.g., "render_distance", "particles")
     * @return Provider, or null if not registered
     * @since 0.4.0
     */
    public OptimizationProvider getProvider(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        return optimizationProviders.get(id.toLowerCase());
    }

    /**
     * Get all registered CapabilityProvider IDs (excluding BROKEN).
     */
    public Set<CapabilityId> getRegisteredIds() {
        return capabilityProviders.keySet().stream()
                .filter(id -> !healthTracker.isBroken(id))
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Get all registered OptimizationProvider IDs.
     * 
     * @return Set of provider IDs
     * @since 0.4.0
     */
    public Set<String> getRegisteredProviderIds() {
        return new HashSet<>(optimizationProviders.keySet());
    }

    /**
     * Get all CapabilityProviders (excluding BROKEN).
     */
    public Collection<CapabilityProvider> getAllProviders() {
        return capabilityProviders.entrySet().stream()
                .filter(entry -> !healthTracker.isBroken(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
    }

    /**
     * Get all OptimizationProviders.
     * 
     * @return Collection of providers
     * @since 0.4.0
     */
    public Collection<OptimizationProvider> getAllOptimizationProviders() {
        return new ArrayList<>(optimizationProviders.values());
    }

    /**
     * Get effective provider coverage (what % of capabilities we control).
     */
    public ProviderCoverage coverage() {
        int totalCapabilities = CapabilityId.values().length;
        int controlledCapabilities = getRegisteredIds().size();
        return ProviderCoverage.of(totalCapabilities, controlledCapabilities);
    }

    /**
     * Discover and register providers (manual list for determinism).
     * 
     * This is where you explicitly list all providers.
     * NO reflection, NO classpath scanning.
     * 
     * Call this during mod initialization.
     */
    public static void discoverProviders(ProviderRegistry registry) {
        // Explicit provider list here
        // Example:
        // registry.register(new ParticlesProvider());
        // registry.register(new CloudsProvider());
        // ... etc

        // For now, empty (will be populated in canary phase)
    }
}