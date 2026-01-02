package dev.nozh.fabric.capability.providers;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;
import dev.nozh.core.capability.*;
import dev.nozh.fabric.capability.MinecraftOptionsAdapter;

import java.util.Optional;
import java.util.Set;

/**
 * Particles provider (Phase C - Canary).
 * 
 * First real provider implementation.
 * 
 * CONTRACT:
 * - Supports EnumValue: ALL, DECREASED, MINIMAL
 * - getCurrentValueSafe() never throws
 * - apply() is atomic with STRONG rollback guarantee
 * - Degrades gracefully if options unavailable
 * 
 * METADATA:
 * - SafetyLevel: SAFE_WITH_VISUAL_CHANGE
 * - RollbackGuarantee: STRONG
 * - SideEffects: touchesOptions = true
 */
public final class ParticlesProvider implements CapabilityProvider {

    private final MinecraftOptionsAdapter options;
    private ProviderStatus currentStatus;
    private String statusReason;

    public ParticlesProvider(MinecraftOptionsAdapter options) {
        this.options = options;
        this.currentStatus = ProviderStatus.HEALTHY;
        this.statusReason = null;
    }

    @Override
    public CapabilityId id() {
        return CapabilityId.PARTICLES;
    }

    @Override
    public ProviderMetadata metadata() {
        return new Metadata();
    }

    @Override
    public ProviderStatus status() {
        return currentStatus;
    }

    @Override
    public Optional<String> statusReason() {
        return Optional.ofNullable(statusReason);
    }

    @Override
    public boolean isAvailable() {
        // Always available (fallback to defaults if needed)
        return true;
    }

    @Override
    public Optional<CapabilityValue> getCurrentValueSafe() {
        try {
            return options.getParticles();
        } catch (Exception e) {
            // Degrade, don't crash
            currentStatus = ProviderStatus.DEGRADED;
            statusReason = "Cannot read particles: " + e.getMessage();
            return Optional.empty();
        }
    }

    @Override
    public ApplyResult apply(CapabilityValue value) {
        // Validate type
        if (!(value instanceof CapabilityValue.EnumValue enumValue)) {
            return new ApplyResult.Rejected("Particles requires EnumValue, got: " + value.getClass().getSimpleName());
        }

        // Validate allowed values
        String val = enumValue.name();
        if (!Set.of("ALL", "DECREASED", "MINIMAL").contains(val)) {
            return new ApplyResult.Rejected("Invalid particles value: " + val);
        }

        // Atomic apply with STRONG rollback
        Optional<CapabilityValue> previousOpt = getCurrentValueSafe();
        if (previousOpt.isEmpty()) {
            return new ApplyResult.Failed(
                    "Cannot read current value for rollback",
                    false,
                    false);
        }

        CapabilityValue previous = previousOpt.get();

        try {
            // Attempt apply
            boolean success = options.setParticles(value);
            if (!success) {
                return new ApplyResult.Failed(
                        "setParticles returned false",
                        false,
                        false);
            }

            // Verify (readback)
            Optional<CapabilityValue> verifyOpt = options.getParticles();
            if (verifyOpt.isEmpty() || !verifyOpt.get().equals(value)) {
                // Apply failed, rollback
                boolean rollbackSuccess = options.setParticles(previous);
                return new ApplyResult.Failed(
                        "Verification failed after apply",
                        true,
                        rollbackSuccess);
            }

            // Success
            currentStatus = ProviderStatus.HEALTHY;
            statusReason = null;
            return new ApplyResult.Success(previous, value);

        } catch (Exception e) {
            // Exception during apply, attempt rollback
            try {
                boolean rollbackSuccess = options.setParticles(previous);
                return new ApplyResult.Failed(
                        "Exception during apply: " + e.getMessage(),
                        true,
                        rollbackSuccess);
            } catch (Exception rollbackException) {
                return new ApplyResult.Failed(
                        "Apply threw + rollback threw: " + e.getMessage(),
                        true,
                        false);
            }
        }
    }

    /**
     * Particles metadata.
     */
    private static class Metadata implements ProviderMetadata {
        @Override
        public SideEffects sideEffects() {
            return SideEffects.optionsOnly();
        }

        @Override
        public SafetyLevel safetyLevel() {
            return SafetyLevel.SAFE_WITH_VISUAL_CHANGE;
        }

        @Override
        public RollbackGuarantee rollbackGuarantee() {
            return RollbackGuarantee.STRONG;
        }

        @Override
        public ImpactLevel gameplayImpact() {
            return ImpactLevel.NONE;
        }

        @Override
        public ImpactLevel visualImpact() {
            return ImpactLevel.MED;
        }

        @Override
        public double expectedGainMs() {
            return 0.5; // Rough estimate: 0.5ms gain on MINIMAL
        }

        @Override
        public Set<String> requiredMods() {
            return Set.of(); // No mod dependencies
        }

        @Override
        public Set<String> conflictingMods() {
            return Set.of(); // No known conflicts
        }
    }
}
