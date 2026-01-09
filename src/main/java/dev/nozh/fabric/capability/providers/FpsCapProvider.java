package dev.nozh.fabric.capability.providers;

import dev.nozh.core.capability.CapabilityId;
import dev.nozh.core.capability.CapabilityValue;
import dev.nozh.core.capability.*;
import dev.nozh.fabric.capability.MinecraftOptionsAdapter;

import java.util.Optional;
import java.util.Set;

/**
 * FPS cap provider (Phase D - Tier 0).
 * 
 * Supports: 30, 45, 60, 90, 120, UNLIMITED (IntValue or EnumValue)
 * Safety: SAFE
 * Rollback: STRONG
 * SideEffects: affectsInputLag = true (explicit)
 */
public final class FpsCapProvider implements CapabilityProvider {

    private final MinecraftOptionsAdapter options;
    private ProviderStatus currentStatus;
    private String statusReason;

    public FpsCapProvider(MinecraftOptionsAdapter options) {
        this.options = options;
        this.currentStatus = ProviderStatus.HEALTHY;
        this.statusReason = null;
    }

    @Override
    public CapabilityId id() {
        return CapabilityId.FPS_CAP;
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
            return options.getFpsCap();
        } catch (Exception e) {
            currentStatus = ProviderStatus.DEGRADED;
            statusReason = "Cannot read FPS cap: " + e.getMessage();
            return Optional.empty();
        }
    }

    @Override
    public ApplyResult apply(CapabilityValue value) {
        // Accept either IntValue or EnumValue
        if (value instanceof CapabilityValue.IntValue intValue) {
            int val = intValue.value();
            if (!Set.of(30, 45, 60, 90, 120, 260).contains(val)) { // 260 = UNLIMITED
                return new ApplyResult.Rejected("Invalid FPS cap value: " + val);
            }
        } else if (value instanceof CapabilityValue.EnumValue enumValue) {
            String val = enumValue.name();
            if (!"UNLIMITED".equals(val)) {
                return new ApplyResult.Rejected("Invalid FPS cap enum: " + val);
            }
        } else {
            return new ApplyResult.Rejected("FPS cap requires IntValue or EnumValue");
        }

        // Atomic apply with STRONG rollback
        Optional<CapabilityValue> previousOpt = getCurrentValueSafe();
        if (previousOpt.isEmpty()) {
            return new ApplyResult.Failed("Cannot read current value", false, false);
        }

        CapabilityValue previous = previousOpt.get();

        try {
            boolean success = options.setFpsCap(value);
            if (!success) {
                return new ApplyResult.Failed("setFpsCap returned false", false, false);
            }

            // Verify
            Optional<CapabilityValue> verifyOpt = options.getFpsCap();
            if (verifyOpt.isEmpty() || !verifyOpt.get().equals(value)) {
                boolean rollbackSuccess = options.setFpsCap(previous);
                return new ApplyResult.Failed("Verification failed", true, rollbackSuccess);
            }

            currentStatus = ProviderStatus.HEALTHY;
            statusReason = null;
            return new ApplyResult.Success(previous, value);

        } catch (Exception e) {
            try {
                boolean rollbackSuccess = options.setFpsCap(previous);
                return new ApplyResult.Failed("Exception: " + e.getMessage(), true, rollbackSuccess);
            } catch (Exception rollbackException) {
                return new ApplyResult.Failed("Apply+rollback threw", true, false);
            }
        }
    }

    private static class Metadata implements ProviderMetadata {
        @Override
        public SideEffects sideEffects() {
            return new SideEffects(
                    true, // touchesOptions
                    false, // requiresRestart
                    true, // affectsInputLag (EXPLICIT)
                    false // breaksDeterminism
            );
        }

        @Override
        public SafetyLevel safetyLevel() {
            return SafetyLevel.SAFE;
        }

        @Override
        public RollbackGuarantee rollbackGuarantee() {
            return RollbackGuarantee.STRONG;
        }

        @Override
        public ImpactLevel gameplayImpact() {
            return ImpactLevel.LOW; // Input lag affects gameplay
        }

        @Override
        public CostLevel costLevel() {
            return CostLevel.MED;
        }

        @Override
        public ImpactLevel visualImpact() {
            return ImpactLevel.NONE;
        }

        @Override
        public double expectedGainMs() {
            return 0.0; // FPS cap doesn't improve performance
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
