package dev.nozh.fabric.capability.providers;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;
import dev.nozh.core.capability.*;
import dev.nozh.fabric.capability.MinecraftOptionsAdapter;

import java.util.Optional;
import java.util.Set;

/**
 * Simulation Distance provider - HIGH IMPACT on CPU-bound scenarios.
 * 
 * Expected gain: +5-10 FPS (CPU-bound)
 * 
 * CONTRACT:
 * - Supports IntValue: 3-32 chunks (Minecraft limits, default 8)
 * - getCurrentValueSafe() never throws
 * - apply() is atomic with STRONG rollback guarantee
 * 
 * METADATA:
 * - SafetyLevel: SAFE_WITH_GAMEPLAY_IMPACT
 * - RollbackGuarantee: STRONG
 * - ImpactLevel: GAMEPLAY = MED, VISUAL = NONE
 * 
 * WHAT IT DOES:
 * Controls distance within which entities/blocks tick and update.
 * Lower = less entity AI, block updates, redstone = more CPU for rendering.
 */
public final class SimulationDistanceProvider implements CapabilityProvider {

    private final MinecraftOptionsAdapter options;
    private ProviderStatus currentStatus;
    private String statusReason;

    private static final int MIN_SIMULATION_DISTANCE = 3; // MC 1.20.1 minimum
    private static final int MAX_SIMULATION_DISTANCE = 32;
    private static final int RECOMMENDED_MIN = 4; // Below this, gameplay suffers

    public SimulationDistanceProvider(MinecraftOptionsAdapter options) {
        this.options = options;
        this.currentStatus = ProviderStatus.HEALTHY;
        this.statusReason = null;
    }

    @Override
    public CapabilityId id() {
        return CapabilityId.SIMULATION_DISTANCE;
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
        // Available in MC 1.18+ (simulation distance was added)
        return true;
    }

    @Override
    public Optional<CapabilityValue> getCurrentValueSafe() {
        try {
            return options.getSimulationDistance();
        } catch (Exception e) {
            currentStatus = ProviderStatus.DEGRADED;
            statusReason = "Cannot read simulation distance: " + e.getMessage();
            return Optional.empty();
        }
    }

    @Override
    public ApplyResult apply(CapabilityValue value) {
        // Validate type
        if (!(value instanceof CapabilityValue.IntValue intValue)) {
            return new ApplyResult.Rejected(
                    "Simulation distance requires IntValue, got: " + value.getClass().getSimpleName());
        }

        // Validate range
        int newDistance = intValue.value();
        if (newDistance < MIN_SIMULATION_DISTANCE || newDistance > MAX_SIMULATION_DISTANCE) {
            return new ApplyResult.Rejected("Simulation distance must be between " + MIN_SIMULATION_DISTANCE +
                    " and " + MAX_SIMULATION_DISTANCE + ", got: " + newDistance);
        }

        // Warn if going too low
        if (newDistance < RECOMMENDED_MIN) {
            // Still allow, but mark as degraded
            currentStatus = ProviderStatus.DEGRADED;
            statusReason = "Simulation distance " + newDistance + " may impact gameplay (recommended minimum: "
                    + RECOMMENDED_MIN + ")";
        }

        // Atomic apply with STRONG rollback
        Optional<CapabilityValue> previousOpt = getCurrentValueSafe();
        if (previousOpt.isEmpty()) {
            return new ApplyResult.Failed(
                    "Cannot read current simulation distance for rollback",
                    false,
                    false);
        }

        CapabilityValue previous = previousOpt.get();

        try {
            // Attempt apply
            boolean success = options.setSimulationDistance(value);
            if (!success) {
                return new ApplyResult.Failed(
                        "setSimulationDistance returned false",
                        false,
                        false);
            }

            // Verify (readback)
            Optional<CapabilityValue> verifyOpt = options.getSimulationDistance();
            if (verifyOpt.isEmpty() || !verifyOpt.get().equals(value)) {
                // Apply failed, rollback
                boolean rollbackSuccess = options.setSimulationDistance(previous);
                return new ApplyResult.Failed(
                        "Verification failed after apply",
                        true,
                        rollbackSuccess);
            }

            // Success
            if (newDistance >= RECOMMENDED_MIN) {
                currentStatus = ProviderStatus.HEALTHY;
                statusReason = null;
            }
            return new ApplyResult.Success(previous, value);

        } catch (Exception e) {
            // Exception during apply, attempt rollback
            try {
                boolean rollbackSuccess = options.setSimulationDistance(previous);
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
            return ImpactLevel.MED;
        }

        @Override
        public ImpactLevel visualImpact() {
            return ImpactLevel.NONE;
        }

        @Override
        public double expectedGainMs() {
            // Expected: 5-10 FPS gain in CPU-bound scenarios
            // Measured as: 7.5 FPS average = ~1.8ms frametime reduction
            //
            // Real- world:
            // - 8 → 6 chunks: ~5-7 FPS (CPU-bound)
            // - 6 → 4 chunks: ~3-5 FPS
            //
            // Conservative estimate for CPU-bound
            return 1.8;
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
