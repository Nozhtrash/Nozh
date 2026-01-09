package dev.nozh.core.compatibility;

import dev.nozh.api.capability.CapabilityContract;
import dev.nozh.api.capability.CapabilityContractDeclaration;
import dev.nozh.core.capability.CapabilityId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CapabilityContractRegistry {

    private static final Map<String, CapabilityContractDeclaration> DECLARATIONS = new ConcurrentHashMap<>();

    public static void register(CapabilityContractDeclaration declaration) {
        if (declaration == null || declaration.modId() == null || declaration.modId().isBlank()) {
            return;
        }
        DECLARATIONS.put(declaration.modId(), declaration);
    }

    public static List<CapabilityContractDeclaration> getDeclarations() {
        return List.copyOf(DECLARATIONS.values());
    }

    public static List<CapabilityContract> getContracts(CapabilityId capabilityId) {
        if (capabilityId == null) {
            return List.of();
        }
        List<CapabilityContract> results = new ArrayList<>();
        for (CapabilityContractDeclaration declaration : DECLARATIONS.values()) {
            for (CapabilityContract contract : declaration.contracts()) {
                if (capabilityId == contract.capabilityId()) {
                    results.add(contract);
                }
            }
        }
        return results;
    }

    private CapabilityContractRegistry() {
        // Static registry
    }
}
