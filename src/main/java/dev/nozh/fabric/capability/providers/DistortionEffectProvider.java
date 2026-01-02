package dev.nozh.fabric.capability.providers;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;
import dev.nozh.core.capability.*;
import dev.nozh.fabric.capability.MinecraftOptionsAdapter;

import java.util.Optional;
import java.util.Set;

/**
 * Distortion Effect Scale provider (0.0 - 1.0).
 * Controls things like nausea effect intensity.
 */
public final class DistortionEffectProvider implements CapabilityProvider {

    private final MinecraftOptionsAdapter options;
    private ProviderStatus currentStatus;
    private String statusReason;

    public DistortionEffectProvider(MinecraftOptionsAdapter options) {
        this.options = options;
        this.currentStatus = ProviderStatus.HEALTHY;
        this.statusReason = null;
    }

    @Override
    public CapabilityId id() {
        return CapabilityId.DISTORTION_EFFECT_SCALE;
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
            return options.getDistortionEffectScale();
        } catch (Exception e) {
            currentStatus = ProviderStatus.DEGRADED;
            statusReason = "Cannot read distortion effect scale: " + e.getMessage();
            return Optional.empty();
        }
    }

    @Override
    public ApplyResult apply(CapabilityValue value) {
        // Validate type (Float or Int 0/1)
        if (!(value instanceof CapabilityValue.FloatValue) && !(value instanceof CapabilityValue.IntValue)) {
            return new ApplyResult.Rejected("DistortionEffect requires FloatValue or IntValue");
        }

        // Atomic apply with STRONG rollback
        Optional<CapabilityValue> previousOpt = getCurrentValueSafe();
        if (previousOpt.isEmpty()) {
            return new ApplyResult.Failed("Cannot read current value for rollback", false, false);
        }

        CapabilityValue previous = previousOpt.get();

        try {
            // Attempt apply
            boolean success = options.setDistortionEffectScale(value);
            if (!success) {
                return new ApplyResult.Failed("setDistortionEffectScale returned false", false, false);
            }

            // Verify
            Optional<CapabilityValue> verifyOpt = options.getDistortionEffectScale();
            // Allow small float epsilon error?
            // Simple check for now
            if (verifyOpt.isEmpty()) {
                boolean rollbackSuccess = options.setDistortionEffectScale(previous);
                return new ApplyResult.Failed("Verification failed (readback empty)", true, rollbackSuccess);
            }

            currentStatus = ProviderStatus.HEALTHY;
            statusReason = null;
            return new ApplyResult.Success(previous, value);

        } catch (Exception e) {
            try {
                boolean rollbackSuccess = options.setDistortionEffectScale(previous);
                return new ApplyResult.Failed("Exception during apply: " + e.getMessage(), true, rollbackSuccess);
            } catch (Exception rollbackException) {
                return new ApplyResult.Failed("Apply threw + rollback threw: " + e.getMessage(), true, false);
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
            return ImpactLevel.LOW;
        }

        @Override
        public ImpactLevel visualImpact() {
            return ImpactLevel.LOW;
        }

        @Override
        public double expectedGainMs() {
            return 0.1; // Minimal gain, mostly for comfort
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
