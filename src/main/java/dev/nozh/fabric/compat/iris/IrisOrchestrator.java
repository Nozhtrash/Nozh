package dev.nozh.fabric.compat.iris;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;
import dev.nozh.core.capability.*;
import java.util.Optional;
import java.util.Set;

/**
 * Orchestrator for Iris Shaders.
 * Allows NOZH to dynamically enable/disable shaders based on performance
 * pressure.
 * Phase 3 Feature.
 */
public class IrisOrchestrator implements CapabilityProvider {

    private final ProviderMetadata metadata = new Metadata();

    @Override
    public CapabilityId id() {
        return CapabilityId.SHADERS;
    }

    @Override
    public ProviderMetadata metadata() {
        return metadata;
    }

    @Override
    public ProviderStatus status() {
        return ProviderStatus.HEALTHY;
    }

    @Override
    public Optional<String> statusReason() {
        return Optional.empty();
    }

    @Override
    public boolean isAvailable() {
        return IrisBridge.areShadersEnabled() || canEnable();
    }

    private boolean canEnable() {
        try {
            Class.forName("net.coderbot.iris.Iris");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Optional<CapabilityValue> getCurrentValueSafe() {
        boolean enabled = IrisBridge.areShadersEnabled();
        return Optional.of(new CapabilityValue.BoolValue(enabled));
    }

    @Override
    public ApplyResult apply(CapabilityValue value) {
        if (!(value instanceof CapabilityValue.BoolValue boolVal)) {
            return new ApplyResult.Rejected("Invalid type for SHADERS, expected BoolValue");
        }

        boolean target = boolVal.value();
        boolean previous = IrisBridge.areShadersEnabled();

        try {
            IrisBridge.setShadersEnabled(target);

            // Verify
            boolean actual = IrisBridge.areShadersEnabled();
            if (actual == target) {
                // Success
                return new ApplyResult.Success(
                        new CapabilityValue.BoolValue(previous),
                        new CapabilityValue.BoolValue(target));
            } else {
                return new ApplyResult.Failed("Verification failed: Iris state did not change", false, false);
            }
        } catch (Exception e) {
            return new ApplyResult.Failed("Exception in IrisBridge: " + e.getMessage(), false, false);
        }
    }

    // Inner metadata class
    private static class Metadata implements ProviderMetadata {
        // id, displayName, description removed as they are not in interface

        @Override
        public ImpactLevel gameplayImpact() {
            return ImpactLevel.HIGH;
        }

        @Override
        public ImpactLevel visualImpact() {
            return ImpactLevel.HIGH;
        } // EXTREME not available

        @Override
        public CostLevel costLevel() {
            return CostLevel.HIGH;
        } // EXTREME not available

        @Override
        public SafetyLevel safetyLevel() {
            return SafetyLevel.EXPERIMENTAL;
        }

        @Override
        public RollbackGuarantee rollbackGuarantee() {
            return RollbackGuarantee.STRONG;
        } // changed to STRONG if bridge works

        @Override
        public double expectedGainMs() {
            return 50.0;
        }

        @Override
        public Set<String> requiredMods() {
            return Set.of();
        }

        @Override
        public Set<String> conflictingMods() {
            return Set.of();
        }

        @Override
        public SideEffects sideEffects() {
            return SideEffects.none();
        }
    }
}
