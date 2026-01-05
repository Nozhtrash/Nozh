package dev.nozh.fabric.capability.providers;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;
import dev.nozh.core.capability.ApplyResult;
import dev.nozh.core.capability.CapabilityProvider;
import dev.nozh.core.capability.CostLevel;
import dev.nozh.core.capability.ImpactLevel;
import dev.nozh.core.capability.ProviderMetadata;
import dev.nozh.core.capability.ProviderStatus;
import dev.nozh.core.capability.RollbackGuarantee;
import dev.nozh.core.capability.SafetyLevel;
import dev.nozh.core.capability.SideEffects;
import dev.nozh.fabric.compat.DynamicLightingBridge;

import java.util.Optional;
import java.util.Set;

public final class DynamicLightingProvider implements CapabilityProvider {

    private final DynamicLightingBridge bridge;
    private ProviderStatus status = ProviderStatus.HEALTHY;
    private String statusReason;

    public DynamicLightingProvider() {
        this.bridge = new DynamicLightingBridge();
    }

    @Override
    public CapabilityId id() {
        return CapabilityId.DYNAMIC_LIGHTING;
    }

    @Override
    public ProviderMetadata metadata() {
        return new Metadata();
    }

    @Override
    public ProviderStatus status() {
        return status;
    }

    @Override
    public Optional<String> statusReason() {
        return Optional.ofNullable(statusReason);
    }

    @Override
    public boolean isAvailable() {
        return bridge.isAvailable();
    }

    @Override
    public Optional<CapabilityValue> getCurrentValueSafe() {
        try {
            Optional<CapabilityValue> value = bridge.getCurrentValue();
            if (value.isEmpty()) {
                status = ProviderStatus.DEGRADED;
                statusReason = "Dynamic lighting value unavailable";
            }
            return value;
        } catch (Exception e) {
            status = ProviderStatus.DEGRADED;
            statusReason = "Dynamic lighting read failed: " + e.getMessage();
            return Optional.empty();
        }
    }

    @Override
    public ApplyResult apply(CapabilityValue value) {
        if (!(value instanceof CapabilityValue.BoolValue)) {
            return new ApplyResult.Rejected("Dynamic lighting requires BoolValue");
        }
        Optional<CapabilityValue> previous = getCurrentValueSafe();
        if (previous.isEmpty()) {
            return new ApplyResult.Failed("Dynamic lighting read failed", false, false);
        }
        boolean success = bridge.apply(value);
        if (!success) {
            return new ApplyResult.Failed("Dynamic lighting apply failed", false, false);
        }
        status = ProviderStatus.HEALTHY;
        statusReason = null;
        return new ApplyResult.Success(previous.get(), value);
    }

    private static final class Metadata implements ProviderMetadata {
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
            return RollbackGuarantee.WEAK;
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
            return 0.8;
        }

        @Override
        public Set<String> requiredMods() {
            return Set.of("lambdynlights");
        }

        @Override
        public Set<String> conflictingMods() {
            return Set.of();
        }
    }
}
