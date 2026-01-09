package dev.nozh.core.safety;

import dev.nozh.core.capability.CapabilityId;
import dev.nozh.core.capability.CapabilityValue;
import dev.nozh.core.bus.CommandType;

import java.util.Optional;

/**
 * Captures failure context for crash-loop recovery.
 */
public record CrashFailureContext(
        long timestamp,
        String source,
        String capabilityId,
        String commandType,
        String requestedValue,
        String errorMessage,
        String exceptionType) {

    public static CrashFailureContext forCommandFailure(
            String source,
            CapabilityId capability,
            CommandType commandType,
            CapabilityValue requestedValue,
            String errorMessage,
            Throwable throwable) {
        return new CrashFailureContext(
                System.currentTimeMillis(),
                source,
                capability != null ? capability.name() : null,
                commandType != null ? commandType.name() : null,
                requestedValue != null ? requestedValue.toString() : null,
                errorMessage,
                throwable != null ? throwable.getClass().getSimpleName() : null);
    }

    public Optional<CapabilityId> resolveCapabilityId() {
        if (capabilityId == null || capabilityId.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(CapabilityId.valueOf(capabilityId));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
