package dev.nozh.core.bus;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Fake executor for testing Contract 2 (Paso 2.5).
 * 
 * Configurable behavior:
 * - Return success
 * - Throw exception
 * - Return failure
 * 
 * Rollback ALWAYS succeeds (for testing rollback success path).
 * To test rollback failure, use a different fake or modify test accordingly.
 * 
 * Zero Minecraft dependencies.
 * Zero real capability logic.
 * Pure test infrastructure.
 */
public final class FakeCapabilityExecutor implements CapabilityExecutor {

    private final Map<CapabilityId, Behavior> behaviors = new HashMap<>();
    private final Map<CapabilityId, CapabilityValue> currentValues = new HashMap<>();

    /**
     * Configure behavior for a capability.
     */
    public FakeCapabilityExecutor when(CapabilityId id, Behavior behavior) {
        behaviors.put(id, behavior);
        return this;
    }

    /**
     * Set current value (for rollback tests).
     */
    public FakeCapabilityExecutor withCurrentValue(CapabilityId id, CapabilityValue value) {
        currentValues.put(id, value);
        return this;
    }

    @Override
    public ExecutionResult execute(CapabilityId id, CapabilityValue value) throws Exception {
        Behavior behavior = behaviors.getOrDefault(id, Behavior.SUCCESS);

        return switch (behavior) {
            case SUCCESS -> {
                currentValues.put(id, value);
                yield new ExecutionResult.Success();
            }
            case FAIL -> new ExecutionResult.Failure("Simulated failure for " + id);
            case THROW -> throw new RuntimeException("Simulated exception for " + id);
        };
    }

    @Override
    public ExecutionResult rollback(CapabilityId id, CapabilityValue oldValue) throws Exception {
        // Rollback ALWAYS succeeds (for testing rollback success scenarios)
        // This allows us to test StandardActionProcessor's rollback handling
        currentValues.put(id, oldValue);
        return new ExecutionResult.Success();
    }

    @Override
    public boolean supportsRollback(CapabilityId id) {
        return true; // Fake always supports rollback
    }

    @Override
    public Optional<CapabilityValue> getCurrentValue(CapabilityId id) {
        return Optional.ofNullable(currentValues.get(id));
    }

    /**
     * Behavior configuration.
     */
    public enum Behavior {
        SUCCESS, // Execute succeeds
        FAIL, // Execute returns failure
        THROW // Execute throws exception
    }
}
