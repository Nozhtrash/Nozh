package dev.nozh.fabric.capability.providers;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;
import dev.nozh.core.capability.*;
import dev.nozh.fabric.capability.MinecraftOptionsAdapter;

import java.util.Optional;
import java.util.Set;

/**
 * Block Entity visibility provider (Chests, Shulkers, Signs, Bells).
 * 
 * Hides all block entities (TEMPORARY measure during extreme load).
 */
public final class BlockEntityProvider implements CapabilityProvider {

    private final MinecraftOptionsAdapter options;
    private ProviderStatus currentStatus;
    private String statusReason;

    public BlockEntityProvider(MinecraftOptionsAdapter options) {
        this.options = options;
        this.currentStatus = ProviderStatus.HEALTHY;
        this.statusReason = null;
    }

    @Override
    public CapabilityId id() {
        return CapabilityId.BLOCK_ENTITIES;
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
            return options.getBlockEntities();
        } catch (Exception e) {
            currentStatus = ProviderStatus.DEGRADED;
            statusReason = "Cannot read block entities: " + e.getMessage();
            return Optional.empty();
        }
    }

    @Override
    public ApplyResult apply(CapabilityValue value) {
        if (!(value instanceof CapabilityValue.BoolValue)) {
            return new ApplyResult.Rejected("BlockEntities requires BoolValue");
        }

        Optional<CapabilityValue> previousOpt = getCurrentValueSafe();
        if (previousOpt.isEmpty()) {
            return new ApplyResult.Failed("Cannot read current value for rollback", false, false);
        }

        CapabilityValue previous = previousOpt.get();

        try {
            boolean success = options.setBlockEntities(value);
            if (!success) {
                return new ApplyResult.Failed("setBlockEntities returned false", false, false);
            }

            Optional<CapabilityValue> verifyOpt = options.getBlockEntities();
            if (verifyOpt.isEmpty() || !verifyOpt.get().equals(value)) {
                boolean rollbackSuccess = options.setBlockEntities(previous);
                return new ApplyResult.Failed("Verification failed", true, rollbackSuccess);
            }

            currentStatus = ProviderStatus.HEALTHY;
            statusReason = null;
            return new ApplyResult.Success(previous, value);

        } catch (Exception e) {
            try {
                boolean rollbackSuccess = options.setBlockEntities(previous);
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
            return ImpactLevel.NONE; // Chests still work, just invisible
        }

        @Override
        public ImpactLevel visualImpact() {
            return ImpactLevel.HIGH; // Invisible chests are noticeable
        }

        @Override
        public double expectedGainMs() {
            return 2.5; // Very high gain in bases
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
