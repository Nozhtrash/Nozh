package dev.nozh.fabric.capability.providers;

import dev.nozh.core.capability.CapabilityId;
import dev.nozh.core.capability.CapabilityValue;
import dev.nozh.core.capability.*;
import dev.nozh.fabric.capability.MinecraftOptionsAdapter;

import java.util.Optional;
import java.util.Set;

/**
 * Smooth Lighting provider (OFF, MIN, MAX).
 * 
 * Stubbed due to symbol resolution issues with AoMode in 1.20.1 mappings.
 */
public final class SmoothLightingProvider implements CapabilityProvider {

    public SmoothLightingProvider(MinecraftOptionsAdapter options) {
        // Stub
    }

    @Override
    public CapabilityId id() {
        return CapabilityId.SMOOTH_LIGHTING;
    }

    @Override
    public ProviderMetadata metadata() {
        return new Metadata();
    }

    @Override
    public ProviderStatus status() {
        return ProviderStatus.DEGRADED;
    }

    @Override
    public Optional<String> statusReason() {
        return Optional.of("Implementation disabled due to mapping issues");
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public Optional<CapabilityValue> getCurrentValueSafe() {
        return Optional.empty();
    }

    @Override
    public ApplyResult apply(CapabilityValue value) {
        return new ApplyResult.Rejected("Smooth Lighting control temporarily disabled");
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
            return RollbackGuarantee.NONE;
        }

        @Override
        public ImpactLevel gameplayImpact() {
            return ImpactLevel.NONE;
        }

        @Override
        public CostLevel costLevel() {
            return CostLevel.MED;
        }

        @Override
        public ImpactLevel visualImpact() {
            return ImpactLevel.MED;
        }

        @Override
        public double expectedGainMs() {
            return 0.0;
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
