package dev.nozh.core.bus;
import dev.nozh.core.capability.CapabilityId;
import dev.nozh.core.capability.CapabilityValue;

/**
 * Minimal executor interface for Contract 2 (test-friendly).
 * 
 * This is NOT the legacy ActionExecutor (Decision-based).
 * This is the new Contract 2 executor: pure capability manipulation.
 * 
 * Real implementation comes in Paso 2.6 (Particles canary).
 * For now, tests use FakeCapabilityExecutor.
 */
public interface CapabilityExecutor {

    /**
     * Execute a capability change.
     * 
     * @param id    Capability to modify
     * @param value New value
     * @return Execution result
     * @throws Exception if execution fails
     */
    ExecutionResult execute(CapabilityId id, CapabilityValue value) throws Exception;

    /**
     * Rollback a capability to previous value.
     * 
     * @param id       Capability to rollback
     * @param oldValue Previous value to restore
     * @return Rollback result
     * @throws Exception if rollback fails
     */
    ExecutionResult rollback(CapabilityId id, CapabilityValue oldValue) throws Exception;

    /**
     * Check if rollback is supported for a capability.
     */
    boolean supportsRollback(CapabilityId id);

    /**
     * Get current value of a capability (for rollback).
     */
    java.util.Optional<CapabilityValue> getCurrentValue(CapabilityId id);

    /**
     * Execution result.
     */
    sealed interface ExecutionResult {
        record Success() implements ExecutionResult {
        }

        record Failure(String error) implements ExecutionResult {
        }

        default boolean succeeded() {
            return this instanceof Success;
        }
    }
}
