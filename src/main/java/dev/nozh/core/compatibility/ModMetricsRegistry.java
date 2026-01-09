package dev.nozh.core.compatibility;

import dev.nozh.api.metrics.ModMetricsDeclaration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ModMetricsRegistry {

    private static final Map<String, ModMetricsDeclaration> DECLARATIONS = new ConcurrentHashMap<>();

    public static void register(ModMetricsDeclaration declaration) {
        if (declaration == null || declaration.modId() == null || declaration.modId().isBlank()) {
            return;
        }
        DECLARATIONS.put(declaration.modId(), declaration);
    }

    public static List<ModMetricsDeclaration> getDeclarations() {
        return List.copyOf(DECLARATIONS.values());
    }

    private ModMetricsRegistry() {
        // Static registry
    }
}
