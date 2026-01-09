package dev.nozh.fabric.capability;

import dev.nozh.core.NozhLogger;
import dev.nozh.core.capability.CapabilityId;
import dev.nozh.core.capability.CapabilityValue;
import dev.nozh.core.bus.Command;
import dev.nozh.core.bus.CommandExecutionReport;
import dev.nozh.core.bus.CommandLifecycle;
import dev.nozh.core.capability.*;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Standard capability executor for production (Phase B).
 * 
 * BRIDGE between Command (Contract 2) and CapabilityProvider (Contract 3).
 * Lives in /fabric integration layer (MC imports allowed here).
 * 
 * CONTRACT RULES:
 * - NO policy decisions (no "if safeMode then...")
 * - NO StateStore access
 * - ONLY translation: Command → Provider → Result
 * - Rollback MANDATORY if apply fails + STRONG guarantee
 * - Never throws exceptions upward
 * 
 * This is MC integration layer. Core remains pure.
 */
public final class StandardCapabilityExecutor implements dev.nozh.core.bus.CapabilityExecutor {

    private final ProviderRegistry registry;
    private final NozhLogger logger;

    public StandardCapabilityExecutor(ProviderRegistry registry, NozhLogger logger) {
        this.registry = registry;
        this.logger = logger;
    }

    @Override
    public ExecutionResult execute(CapabilityId id, CapabilityValue value) throws Exception {
        // Get provider
        Optional<CapabilityProvider> providerOpt = registry.get(id);
        if (providerOpt.isEmpty()) {
            return new ExecutionResult.Failure("Provider not found: " + id);
        }

        CapabilityProvider provider = providerOpt.get();

        ProviderHealthCheck.Result health = ProviderHealthCheck.check(provider);
        if (!health.healthy()) {
            return new ExecutionResult.Failure("Health check failed: " + health.reason());
        }

        // Apply
        ApplyResult result = provider.apply(value);

        if (result instanceof ApplyResult.Success success) {
            return new ExecutionResult.Success();
        } else if (result instanceof ApplyResult.Rejected rejected) {
            return new ExecutionResult.Failure("Rejected: " + rejected.reason());
        } else if (result instanceof ApplyResult.Failed failed) {
            // Failed with potential rollback info
            String reason = "Apply failed: " + failed.reason();
            if (failed.rollbackAttempted()) {
                reason += failed.rollbackSucceeded() ? " (rollback succeeded)" : " (rollback FAILED)";
            }
            return new ExecutionResult.Failure(reason);
        }

        return new ExecutionResult.Failure("Unknown result type");
    }

    @Override
    public ExecutionResult rollback(CapabilityId id, CapabilityValue oldValue) throws Exception {
        // Get provider
        Optional<CapabilityProvider> providerOpt = registry.get(id);
        if (providerOpt.isEmpty()) {
            return new ExecutionResult.Failure("Provider not found for rollback: " + id);
        }

        CapabilityProvider provider = providerOpt.get();

        // Rollback is just another apply (to old value)
        ApplyResult result = provider.apply(oldValue);

        if (result instanceof ApplyResult.Success) {
            return new ExecutionResult.Success();
        } else if (result instanceof ApplyResult.Rejected rejected) {
            return new ExecutionResult.Failure("Rollback rejected: " + rejected.reason());
        } else if (result instanceof ApplyResult.Failed failed) {
            return new ExecutionResult.Failure("Rollback failed: " + failed.reason());
        }

        return new ExecutionResult.Failure("Unknown rollback result");
    }

    @Override
    public boolean supportsRollback(CapabilityId id) {
        return registry.get(id)
                .map(provider -> provider.metadata().rollbackGuarantee() != RollbackGuarantee.NONE)
                .orElse(false);
    }

    @Override
    public Optional<CapabilityValue> getCurrentValue(CapabilityId id) {
        return registry.get(id)
                .flatMap(CapabilityProvider::getCurrentValueSafe);
    }
}
