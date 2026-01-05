package dev.nozh.fabric.capability.providers;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;
import dev.nozh.core.capability.*;
import dev.nozh.fabric.capability.MinecraftOptionsAdapter;

import java.util.Optional;
import java.util.Set;

/**
 * Fog provider (controls fog rendering).
 * 
 * Note: Access to fog toggle might be limited in vanilla 1.20+ without mods
 * (Sodium/Iris).
 * This provider acts as a proxy or stub if vanilla doesn't expose it directly.
 */
public final class FogProvider implements CapabilityProvider {

    private final MinecraftOptionsAdapter options;
    private final ProviderStatus currentStatus;
    private final String statusReason;

    public FogProvider(MinecraftOptionsAdapter options) {
        this.options = options;
        // Mark as DEGRADED by default since vanilla doesn't offer direct toggle
        // and our adapter methods for fog are stubs.
        this.currentStatus = ProviderStatus.DEGRADED;
        this.statusReason = "Not implemented in vanilla adapter";
    }

    @Override
    public CapabilityId id() {
        return CapabilityId.FOG;
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
        return false;
    }

    @Override
    public Optional<CapabilityValue> getCurrentValueSafe() {
        return options.getFogDistance();
    }

    @Override
    public ApplyResult apply(CapabilityValue value) {
        return new ApplyResult.Rejected("Fog control not implemented");
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
            return ImpactLevel.LOW;
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
            return 0.4;
        }

        @Override
        public Set<String> requiredMods() {
            return Set.of("sodium"); // Hint that it might work with Sodium later
        }

        @Override
        public Set<String> conflictingMods() {
            return Set.of();
        }
    }
}
