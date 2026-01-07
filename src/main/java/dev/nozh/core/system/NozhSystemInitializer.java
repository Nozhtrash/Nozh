package dev.nozh.core.system;

import dev.nozh.NozhConstants;
import dev.nozh.core.compat.ModPermissionRegistry;
import dev.nozh.core.safety.ProviderBlacklist;
import dev.nozh.fabric.compat.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.MinecraftClient;

/**
 * One-stop initialization for all NOZH systems.
 * 
 * Initializes:
 * - All mod adapters (Sodium, Iris, Lithium, EntityCulling)
 * - Permission registry
 * - Provider blacklist
 * - Telemetry systems
 * - Decision logging
 * 
 * Call during mod initialization.
 * 
 * INTEGRATION: Complete system wiring
 */
public final class NozhSystemInitializer {

    private static boolean initialized = false;
    private static final ModPermissionRegistry permissionRegistry = new ModPermissionRegistry();
    private static final ProviderBlacklist providerBlacklist = new ProviderBlacklist();

    /**
     * Initialize all NOZH systems.
     * Safe to call multiple times (idempotent).
     */
    public static void initialize() {
        if (initialized) {
            return;
        }

        NozhConstants.LOGGER.info("=== NOZH System Initialization ===");

        // Initialize mod adapters
        initializeAdapters();

        // Initialize permission system
        initializePermissions();

        // Initialize safety systems
        initializeSafety();

        // Register lifecycle hooks
        registerLifecycleHooks();

        initialized = true;
        NozhConstants.LOGGER.info("=== NOZH Initialization Complete ===");
    }

    private static void initializeAdapters() {
        NozhConstants.LOGGER.info("Initializing mod adapters...");

        // Sodium
        if (SodiumAdapterExpanded.initialize()) {
            permissionRegistry.registerMod("sodium");
        }

        // Iris
        if (IrisAdapter.initialize()) {
            permissionRegistry.registerMod("iris");
        }

        // Lithium
        if (LithiumAdapter.initialize()) {
            permissionRegistry.registerMod("lithium");
        }

        // EntityCulling
        if (EntityCullingAdapter.initialize()) {
            permissionRegistry.registerMod("entityculling");
        }

        NozhConstants.LOGGER.info("Mod adapters initialized");
    }

    private static void initializePermissions() {
        NozhConstants.LOGGER.info("Initializing permission registry...");
        permissionRegistry.initializeDefaults();
        NozhConstants.LOGGER.info("Permission registry ready");
    }

    private static void initializeSafety() {
        NozhConstants.LOGGER.info("Initializing safety systems...");
        providerBlacklist.initializeDefaults();
        NozhConstants.LOGGER.info("Safety systems ready");
    }

    private static void registerLifecycleHooks() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            NozhConstants.LOGGER.info("NOZH fully operational");
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            NozhConstants.LOGGER.info("NOZH shutting down gracefully");
        });
    }

    /**
     * Get permission registry instance.
     */
    public static ModPermissionRegistry getPermissionRegistry() {
        return permissionRegistry;
    }

    /**
     * Get provider blacklist instance.
     */
    public static ProviderBlacklist getProviderBlacklist() {
        return providerBlacklist;
    }

    /**
     * Check if system is initialized.
     */
    public static boolean isInitialized() {
        return initialized;
    }
}
