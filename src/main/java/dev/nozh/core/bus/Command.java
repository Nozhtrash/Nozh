package dev.nozh.core.bus;

import java.util.UUID;

/**
 * Sealed command hierarchy (Contract 2).
 * 
 * All commands are sealed, typed, immutable records.
 * NO generic Map<String, Object> params.
 * 
 * Contract 2, Rule C2.X: Bus does NOT know "why" commands exist.
 * Contract 2, Rule C2.Z: Commands are testable without Minecraft.
 */
public sealed interface Command {

    /**
     * Unique command ID for audit trail.
     */
    UUID id();

    /**
     * Command type.
     */
    CommandType type();

    /**
     * Apply a capability value.
     */
    record ApplyCapability(
            UUID id,
            CapabilityId capability,
            CapabilityValue value) implements Command {
        public ApplyCapability(CapabilityId capability, CapabilityValue value) {
            this(UUID.randomUUID(), capability, value);
        }

        @Override
        public CommandType type() {
            return CommandType.APPLY;
        }
    }

    /**
     * Reset a capability to default.
     */
    record ResetCapability(
            UUID id,
            CapabilityId capability) implements Command {
        public ResetCapability(CapabilityId capability) {
            this(UUID.randomUUID(), capability);
        }

        @Override
        public CommandType type() {
            return CommandType.RESET;
        }
    }

    /**
     * Preview a capability change (no actual application).
     */
    record PreviewCapability(
            UUID id,
            CapabilityId capability,
            CapabilityValue value) implements Command {
        public PreviewCapability(CapabilityId capability, CapabilityValue value) {
            this(UUID.randomUUID(), capability, value);
        }

        @Override
        public CommandType type() {
            return CommandType.PREVIEW;
        }
    }

    /**
     * Run benchmark.
     */
    record RunBenchmark(
            UUID id,
            int durationSeconds) implements Command {
        public RunBenchmark(int durationSeconds) {
            this(UUID.randomUUID(), durationSeconds);
        }

        @Override
        public CommandType type() {
            return CommandType.BENCHMARK;
        }
    }
}
