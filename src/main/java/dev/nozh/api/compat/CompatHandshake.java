package dev.nozh.api.compat;

import dev.nozh.core.compatibility.StewardshipHandshakeRegistry;

public final class CompatHandshake {

    public static void registerStewardship(StewardshipDeclaration declaration) {
        StewardshipHandshakeRegistry.register(declaration);
    }

    public static StewardshipDeclaration.Builder declare(String modId, String displayName) {
        return StewardshipDeclaration.builder(modId, displayName);
    }

    private CompatHandshake() {
        // API entrypoint
    }
}
