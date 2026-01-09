package dev.nozh.core.compatibility;

import dev.nozh.api.compat.StewardshipDeclaration;
import dev.nozh.api.compat.StewardshipMode;
import dev.nozh.core.bus.CapabilityId;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class StewardshipHandshakeRegistry {

    private static final Map<String, StewardshipDeclaration> DECLARATIONS = new ConcurrentHashMap<>();

    public static void register(StewardshipDeclaration declaration) {
        if (declaration == null || declaration.modId() == null || declaration.modId().isBlank()) {
            return;
        }
        DECLARATIONS.put(declaration.modId(), declaration);
    }

    public static StewardshipDecision resolveDecision(CapabilityId capability) {
        if (capability == null) {
            return null;
        }
        for (StewardshipDeclaration declaration : DECLARATIONS.values()) {
            if (!isModLoaded(declaration.modId())) {
                continue;
            }
            StewardshipMode mode = declaration.modeFor(capability);
            if (mode == StewardshipMode.NONE) {
                continue;
            }
            return new StewardshipDecision(
                    capability,
                    declaration.displayName(),
                    mode,
                    declaration.reason());
        }
        return null;
    }

    public static List<StewardshipDecision> getTraces() {
        List<StewardshipDecision> traces = new ArrayList<>();
        for (StewardshipDeclaration declaration : DECLARATIONS.values()) {
            if (!isModLoaded(declaration.modId())) {
                continue;
            }
            for (CapabilityId capability : CapabilityId.values()) {
                StewardshipMode mode = declaration.modeFor(capability);
                if (mode == StewardshipMode.NONE) {
                    continue;
                }
                traces.add(new StewardshipDecision(
                        capability,
                        declaration.displayName(),
                        mode,
                        declaration.reason()));
            }
        }
        return traces;
    }

    public static List<StewardshipDeclaration> getDeclarations() {
        return List.copyOf(DECLARATIONS.values());
    }

    private static boolean isModLoaded(String modId) {
        try {
            return FabricLoader.getInstance().isModLoaded(modId);
        } catch (Exception e) {
            return false;
        }
    }

    private StewardshipHandshakeRegistry() {
        // Static registry
    }
}
