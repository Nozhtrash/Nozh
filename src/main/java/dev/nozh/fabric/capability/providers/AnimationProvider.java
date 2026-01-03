package dev.nozh.fabric.capability.providers;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;
import dev.nozh.core.capability.*;
import dev.nozh.fabric.capability.MinecraftOptionsAdapter;

import java.util.Optional;
import java.util.Set;

/**
 * Animation visibility provider.
 * 
 * Toggles expensive texture animations/particles globally.
 */
public final class AnimationProvider implements CapabilityProvider {

    private final MinecraftOptionsAdapter options;
    private ProviderStatus currentStatus;
    private String statusReason;

    public AnimationProvider(MinecraftOptionsAdapter options) {
        this.options = options;
        this.currentStatus = ProviderStatus.HEALTHY;
        this.statusReason = null;
    }

    @Override
    public CapabilityId id() {
        return CapabilityId.ANIMATIONS;
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
            return options.getAnimations();
        } catch (Exception e) {
            currentStatus = ProviderStatus.DEGRADED;
            statusReason = "Cannot read animation settings: " + e.getMessage();
            return Optional.empty();
        }
    }

    @Override
    public ApplyResult apply(CapabilityValue value) {
        if (!(value instanceof CapabilityValue.BoolValue)) {
            return new ApplyResult.Rejected("Animations requires BoolValue");
        }

        Optional<CapabilityValue> previousOpt = getCurrentValueSafe();
        if (previousOpt.isEmpty()) {
            return new ApplyResult.Failed("Cannot read current value for rollback", false, false);
        }

        CapabilityValue previous = previousOpt.get();

        try {
            boolean success = options.setAnimations(value);
            if (!success) {
                return new ApplyResult.Failed("setAnimations returned false", false, false);
            }

            Optional<CapabilityValue> verifyOpt = options.getAnimations();
            if (verifyOpt.isEmpty() || !verifyOpt.get().equals(value)) {
                boolean rollbackSuccess = options.setAnimations(previous);
                return new ApplyResult.Failed("Verification failed", true, rollbackSuccess);
            }

            currentStatus = ProviderStatus.HEALTHY;
            statusReason = null;
            return new ApplyResult.Success(previous, value);

        } catch (Exception e) {
            try {
                boolean rollbackSuccess = options.setAnimations(previous);
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
            return ImpactLevel.NONE;
        }

        @Override
        public ImpactLevel visualImpact() {
            return ImpactLevel.MED;
        }

        @Override
        public double expectedGainMs() {
            return 0.8;
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
