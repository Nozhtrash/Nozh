package dev.nozh.core.capability;

import dev.nozh.core.capability.CapabilityValue;

import java.util.Optional;

/**
 * Health checks for capability providers to avoid unstable applies.
 */
public final class ProviderHealthCheck {

    private ProviderHealthCheck() {
        // Utility class
    }

    public static Result check(CapabilityProvider provider) {
        if (provider == null) {
            return new Result(false, ProviderStatus.BROKEN, "Provider missing");
        }
        ProviderStatus status = provider.status();
        if (status == ProviderStatus.BROKEN) {
            return new Result(false, status, provider.statusReason().orElse("Provider broken"));
        }
        if (status == ProviderStatus.DEGRADED) {
            return new Result(false, status, provider.statusReason().orElse("Provider degraded"));
        }
        Optional<CapabilityValue> current = provider.getCurrentValueSafe();
        if (current.isEmpty()) {
            return new Result(false, ProviderStatus.DEGRADED, "Health check read failed");
        }
        return new Result(true, status, "");
    }

    public record Result(boolean healthy, ProviderStatus status, String reason) {
    }
}
