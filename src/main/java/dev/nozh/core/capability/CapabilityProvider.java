package dev.nozh.core.capability;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;
import java.util.Optional;

/**
 * Core interface for capability providers (Phase C).
 * 
 * A CapabilityProvider is responsible for reading and modifying a specific game setting
 * in a safe, reversible manner.
 * 
 * Contract:
 * - id() returns unique CapabilityId
 * - metadata() returns immutable metadata about the provider
 * - status() returns current health status
 * - isAvailable() returns whether the provider can be used
 * - getCurrentValueSafe() returns current value or empty if unavailable
 * - apply() applies a new value atomically with rollback support
 * 
 * Providers must be isolated - one failing provider cannot crash others.
 * 
 * @since v0.2.0-alpha
 */
public interface CapabilityProvider {
    
    /**
     * Get the unique identifier for this provider.
     * 
     * @return capability identifier
     */
    CapabilityId id();
    
    /**
     * Get immutable metadata about this provider.
     * 
     * @return provider metadata
     */
    ProviderMetadata metadata();
    
    /**
     * Get current provider health status.
     * 
     * @return current status (HEALTHY, DEGRADED, or BROKEN)
     */
    ProviderStatus status();
    
    /**
     * Get the reason for current status if not HEALTHY.
     * 
     * @return optional status reason
     */
    Optional<String> statusReason();
    
    /**
     * Check if this provider is available for use.
     * A provider is available if it's not BROKEN and all dependencies are met.
     * 
     * @return true if available
     */
    boolean isAvailable();
    
    /**
     * Safely get the current value of this capability.
     * Returns empty if the value cannot be read.
     * 
     * @return current value or empty
     */
    Optional<CapabilityValue> getCurrentValueSafe();
    
    /**
     * Apply a new value to this capability.
     * This method is atomic and includes automatic rollback on failure.
     * 
     * @param value the new value to apply
     * @return result of the apply operation
     */
    ApplyResult apply(CapabilityValue value);
}
