package dev.nozh.fabric.capability.providers;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;
import dev.nozh.core.capability.*;
import dev.nozh.fabric.capability.MinecraftOptionsAdapter;

import java.util.Optional;
import java.util.Set;

/**
 * Clouds provider (Phase D - Tier 0).
 * 
 * Supports: FANCY, FAST, OFF
 * Safety: SAFE_WITH_VISUAL_CHANGE
 * Rollback: STRONG
 */
public final class CloudsProvider implements CapabilityProvider {

    private final MinecraftOptionsAdapter options;
    private ProviderStatus currentStatus;
    private String statusReason;

    public CloudsProvider(MinecraftOptionsAdapter options) {
        this.options = options;
        this.currentStatus = ProviderStatus.HEALTHY;
        this.statusReason = null;
    }

    @Override
    public CapabilityId id() {
        return CapabilityId.CLOUDS;
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
        return true;
    }

    @Override
    public Optional<CapabilityValue> getCurrentValueSafe() {
        try {
            return options.getClouds();
        } catch (Exception e) {
            currentStatus = ProviderStatus.DEGRADED;
            statusReason = "Cannot read clouds: " + e.getMessage();
            return Optional.empty();
        }
    }

    @Override
    public ApplyResult apply(CapabilityValue value) {
        if (!(value instanceof CapabilityValue.EnumValue enumValue)) {
            return new ApplyResult.Rejected("Clouds requires EnumValue");
        }

        String val = enumValue.name();
        if (!Set.of("FANCY", "FAST", "OFF").contains(val)) {
            return new ApplyResult.Rejected("Invalid clouds value: " + val);
        }

        // Atomic apply with STRONG rollback
        Optional<CapabilityValue> previousOpt = getCurrentValueSafe();
        if (previousOpt.isEmpty()) {
            return new ApplyResult.Failed("Cannot read current value", false, false);
        }

        CapabilityValue previous = previousOpt.get();

        try {
            boolean success = options.setClouds(value);
            if (!success) {
                return new ApplyResult.Failed("setClouds returned false", false, false);
            }

            // Verify
            Optional<CapabilityValue> verifyOpt = options.getClouds();
            if (verifyOpt.isEmpty() || !verifyOpt.get().equals(value)) {
                boolean rollbackSuccess = options.setClouds(previous);
                return new ApplyResult.Failed("Verification failed", true, rollbackSuccess);
            }

            currentStatus = ProviderStatus.HEALTHY;
            statusReason = null;
            return new ApplyResult.Success(previous, value);

        } catch (Exception e) {
            try {
                boolean rollbackSuccess = options.setClouds(previous);
                return new ApplyResult.Failed("Exception: " + e.getMessage(), true, rollbackSuccess);
            } catch (Exception rollbackException) {
                return new ApplyResult.Failed("Apply+rollback threw", true, false);
            }
        }
    }

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
            return ImpactLevel.LOW;
        }

        @Override
        public double expectedGainMs() {
            return 0.3;
        }

        @Override
        public Set<String> requiredMods() {
            return Set.of();
        }

        @Override
        public Set<String> conflictingMods() {
            return Set.of();
        }
    }
}
