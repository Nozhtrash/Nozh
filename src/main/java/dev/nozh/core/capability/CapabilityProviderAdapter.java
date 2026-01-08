package dev.nozh.core.capability;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;

import java.util.Optional;

/**
 * Adapter that bridges CapabilityProvider to OptimizationProvider.
 * <p>
 * This allows the new optimization system (IntegratedGovernor, ProviderExecutor)
 * to work with existing CapabilityProvider implementations without modification.
 * <p>
 * The adapter:
 * - Maps CapabilityId to String IDs
 * - Converts apply() calls to execute() boolean results
 * - Handles metadata and status checks
 */
public final class CapabilityProviderAdapter implements OptimizationProvider {

    private final CapabilityProvider wrapped;
    private final String actionId;
    private final CapabilityValue targetValue;

    /**
     * Create an adapter for a CapabilityProvider.
     *
     * @param wrapped     The original CapabilityProvider
     * @param actionId    The action ID for this provider (from ActionProviderMapping)
     * @param targetValue The value to apply when execute() is called
     */
    public CapabilityProviderAdapter(
            CapabilityProvider wrapped,
            String actionId,
            CapabilityValue targetValue
    ) {
        this.wrapped = wrapped;
        this.actionId = actionId;
        this.targetValue = targetValue;
    }

    @Override
    public String getId() {
        // Convert CapabilityId to string
        CapabilityId capId = wrapped.id();
        return capId != null ? capId.name().toLowerCase() : actionId;
    }

    @Override
    public String getName() {
        CapabilityId capId = wrapped.id();
        if (capId != null) {
            return capId.name().replace('_', ' ');
        }
        return actionId.replace('_', ' ');
    }

    @Override
    public String getDescription() {
        return "Capability provider: " + getName();
    }

    @Override
    public boolean canExecute() {
        // Check if provider is available and healthy
        if (!wrapped.isAvailable()) {
            return false;
        }

        ProviderStatus status = wrapped.status();
        // Allow HEALTHY and DEGRADED, but not UNAVAILABLE or ERRORED
        return status == ProviderStatus.HEALTHY || status == ProviderStatus.DEGRADED;
    }

    @Override
    public boolean execute() {
        try {
            // Apply the target value
            ApplyResult result = wrapped.apply(targetValue);

            // Check if apply was successful
            if (result instanceof ApplyResult.Success) {
                return true;
            } else if (result instanceof ApplyResult.Failed failed) {
                // Log failure reason if available
                return false;
            } else if (result instanceof ApplyResult.Rejected rejected) {
                // Rejected means the value was invalid
                return false;
            }

            return false;
        } catch (Exception e) {
            // Catch any exceptions and return false
            return false;
        }
    }

    @Override
    public double getExpectedFpsImpact() {
        ProviderMetadata metadata = wrapped.metadata();
        if (metadata != null) {
            // Convert frametime gain to FPS
            // Formula: FPS gain ≈ (current_fps² × frametime_gain_ms) / 1000
            // Assuming 60 FPS baseline: (60² × ms) / 1000 = 3.6 × ms
            double frametimeGainMs = metadata.expectedGainMs();
            return 3.6 * frametimeGainMs; // Rough FPS estimate
        }
        return 2.0;
    }

    @Override
    public String getCategory() {
        CapabilityId capId = wrapped.id();
        if (capId != null) {
            // Categorize based on CapabilityId
            String name = capId.name();
            if (name.contains("RENDER") || name.contains("DISTANCE")) {
                return "rendering";
            } else if (name.contains("PARTICLE")) {
                return "particles";
            } else if (name.contains("ENTITY")) {
                return "entities";
            } else if (name.contains("SHADOW") || name.contains("LIGHT")) {
                return "lighting";
            }
        }
        return "general";
    }

    @Override
    public boolean isReversible() {
        ProviderMetadata metadata = wrapped.metadata();
        if (metadata != null) {
            RollbackGuarantee guarantee = metadata.rollbackGuarantee();
            return guarantee == RollbackGuarantee.STRONG ||
                   guarantee == RollbackGuarantee.BEST_EFFORT;
        }
        return true;
    }

    /**
     * Get the wrapped CapabilityProvider.
     *
     * @return Original provider
     */
    public CapabilityProvider getWrapped() {
        return wrapped;
    }
}