package dev.nozh.core.capability;

import dev.nozh.core.bus.CapabilityId;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provider registry (Contract 3).
 * 
 * Central registry for all CapabilityProviders.
 * 
 * CONTRACT RULES:
 * - Explicit registration only (no reflection scan)
 * - BROKEN providers excluded by default
 * - Thread-safe registration and lookup
 * - One broken provider MUST NOT crash registry
 * 
 * DETERMINISTIC BUILDS:
 * Manual provider list prevents classpath surprises.
 */
public final class ProviderRegistry {

    private final Map<CapabilityId, CapabilityProvider> providers = new ConcurrentHashMap<>();
    private final Map<CapabilityId, dev.nozh.core.bus.CapabilityValue> baselineValues = new ConcurrentHashMap<>();
    private final ProviderHealthTracker healthTracker;

    public ProviderRegistry(ProviderHealthTracker healthTracker) {
        this.healthTracker = healthTracker;
    }

    /**
     * Register a provider.
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
            providers.put(id, provider);
            healthTracker.markHealthy(id);
            recordBaselineIfMissing(provider);

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
     * Get a provider by capability ID.
     * 
     * @param id Capability ID
     * @return Provider, or empty if not registered or BROKEN
     */
    public Optional<CapabilityProvider> get(CapabilityId id) {
        // Exclude BROKEN providers
        if (healthTracker.isBroken(id)) {
            return Optional.empty();
        }
        CapabilityProvider provider = providers.get(id);
        if (provider == null) {
            return Optional.empty();
        }
        recordBaselineIfMissing(provider);
        return Optional.of(provider);
    }

    /**
     * Get the baseline value for a capability if known.
     */
    public Optional<dev.nozh.core.bus.CapabilityValue> baselineValue(CapabilityId id) {
        return Optional.ofNullable(baselineValues.get(id));
    }

    /**
     * Get all registered provider IDs (excluding BROKEN).
     */
    public Set<CapabilityId> getRegisteredIds() {
        return providers.keySet().stream()
                .filter(id -> !healthTracker.isBroken(id))
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Get all providers (excluding BROKEN).
     */
    public Collection<CapabilityProvider> getAllProviders() {
        return providers.entrySet().stream()
                .filter(entry -> !healthTracker.isBroken(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
    }

    private void recordBaselineIfMissing(CapabilityProvider provider) {
        CapabilityId id = provider.id();
        if (baselineValues.containsKey(id)) {
            return;
        }
        provider.getCurrentValueSafe().ifPresent(value -> baselineValues.putIfAbsent(id, value));
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
