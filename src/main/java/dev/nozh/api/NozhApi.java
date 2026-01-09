package dev.nozh.api;

import dev.nozh.api.capability.CapabilityContractDeclaration;
import dev.nozh.api.compat.StewardshipDeclaration;
import dev.nozh.api.metrics.ModMetricsDeclaration;
import dev.nozh.core.compatibility.CapabilityContractRegistry;
import dev.nozh.core.compatibility.ModMetricsRegistry;
import dev.nozh.core.compatibility.StewardshipHandshakeRegistry;

/**
 * Entry point for third-party mods integrating with NOZH.
 */
public final class NozhApi {

    public static void registerStewardship(StewardshipDeclaration declaration) {
        StewardshipHandshakeRegistry.register(declaration);
    }

    public static void registerCapabilityContracts(CapabilityContractDeclaration declaration) {
        CapabilityContractRegistry.register(declaration);
    }

    public static void registerModMetrics(ModMetricsDeclaration declaration) {
        ModMetricsRegistry.register(declaration);
    }

    private NozhApi() {
        // Utility class
    }
}
