/**
 * NOZH - Adaptive Performance Optimization
 * Copyright (c) 2025 NOZH Project
 * 
 * Licensed under the MIT License.
 * 
 * This file defines a CORE ARCHITECTURAL CONTRACT.
 * Changes here affect system-wide invariants.
 * 
 * Read docs/v0.2-alpha.md before modifying.
 */
package dev.nozh.core.capability;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;

import java.util.Optional;

/**
 * CapabilityProvider interface (Contract 3).
 * 
 * WHY THIS EXISTS:
 * CapabilityProvider enforces ISOLATION: one broken provider MUST NOT crash
 * the entire system. This is critical because providers interact with Minecraft
 * (particles, clouds, FPS cap) which can fail in unpredictable ways:
 * - Mod conflicts (shaders override particles)
 * - Version mismatches (API changes between MC versions)
 * - Hardware limits (GPU driver rejects FPS cap value)
 * 
 * ISOLATION GUARANTEE:
 * If ParticlesProvider.apply() throws an exception, the system:
 * 1. Catches it (never propagates upward)
 * 2. Marks provider as BROKEN
 * 3. Continues operating with other providers
 * 4. Logs issue for user debugging
 * 
 * WHY "NEVER THROW" MATTERS:
 * Without this rule, a single broken provider crashes governor tick →
 * freezes entire mod → user uninstalls. With this rule, partial degradation
 * is acceptable (3 of 4 providers working) rather than total failure.
 * 
 * CONTRACT RULES:
 * - NEVER throw exceptions from any method
 * - getCurrentValueSafe() returns Optional-like pattern, never crashes
 * - apply() is atomic or best-effort atomic (declare via metadata)
 * - One broken provider MUST NOT crash registry
 * 
 * PURITY:
 * - Interface is pure (in /core)
 * - Implementations MAY use Minecraft (in /fabric or /integration)
 * 
 * WHY ATOMIC apply():
 * If apply() is NOT atomic, failed changes leave system in partial state:
 * - particles=MEDIUM attempted but failed
 * - Minecraft shows particles=HIGH (old value)
 * - State shows particles=MEDIUM (new value)
 * → Mismatch causes governor confusion, repeat attempts, flapping.
 * Atomicity (or rollback on failure) prevents this.
 */
public interface CapabilityProvider {

    /**
     * Capability ID this provider manages.
     */
    CapabilityId id();

    /**
     * Provider metadata (static characteristics).
     */
    ProviderMetadata metadata();

    /**
     * Current operational status.
     */
    ProviderStatus status();

    /**
     * Reason for current status (if not HEALTHY).
     * 
     * @return Human-readable status reason, or empty if HEALTHY
     */
    Optional<String> statusReason();

    /**
     * Check if provider is available (mod/version requirements met).
     * 
     * @return true if provider can be used, false otherwise
     */
    boolean isAvailable();

    /**
     * Get current capability value safely.
     * 
     * NEVER throws. If cannot read -> return Optional.empty() and mark DEGRADED.
     * 
     * @return Current value, or empty if unavailable
     */
    Optional<CapabilityValue> getCurrentValueSafe();

    /**
     * Apply a new capability value.
     * 
     * MUST be atomic (or best-effort atomic, declare via metadata).
     * NEVER throws exceptions upward.
     * 
     * On failure with STRONG rollback guarantee:
     * - Capture previousValue
     * - Attempt apply
     * - If fails: attempt rollback
     * - Return Failed with rollback status
     * 
     * @param value New value to apply
     * @return ApplyResult (Success/Failed/Rejected)
     */
    ApplyResult apply(CapabilityValue value);
}
