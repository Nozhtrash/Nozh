package dev.nozh.client;

import dev.nozh.NozhConstants;
import dev.nozh.core.NozhLogger;
import dev.nozh.core.bus.ActionBus;
import dev.nozh.core.bus.StandardActionProcessor;
import dev.nozh.core.config.ConfigManager;
import dev.nozh.core.config.ConfigSyncService;
import dev.nozh.core.capability.ProviderRegistry;
import dev.nozh.core.governor.GovernorRunner;
import dev.nozh.core.matrix.ActionSuccessTracker;
import dev.nozh.core.safety.CrashLoopGuard;
import dev.nozh.core.state.StateStore;
import dev.nozh.fabric.FabricNozhLogger;
import dev.nozh.fabric.capability.MinecraftOptionsAdapter;
import dev.nozh.fabric.capability.ProductionMinecraftOptionsAdapter;
import dev.nozh.fabric.capability.ProviderBootstrap;
import dev.nozh.fabric.capability.StandardCapabilityExecutor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

/**
 * NOZH Client-side initializer - FULL INTEGRATION.
 * 
 * Wires all components for functional end-to-end optimization:
 * Telemetry → RuntimeState → Governor → ActionBus → Providers → Game
 */
@Environment(EnvType.CLIENT)
public class NozhModClient implements ClientModInitializer {

    private static dev.nozh.core.profiler.PerfManager perfManager;
    private static dev.nozh.core.monitoring.TickTimeSampler tickTimeSampler;
    private static GovernorRunner governorRunner;
    private static ActionBus actionBus;
    private static StateStore stateStore;
    private static ConfigSyncService configSyncService;
    private static dev.nozh.core.intelligence.SessionLearning sessionLearning;

    private static int tickCounter = 0;
    private static final int TELEMETRY_UPDATE_INTERVAL = 20; // Every second (20 ticks)
    private static final int GOVERNOR_POLL_INTERVAL = 100; // Every 5 seconds (100 ticks)
    private static final int SESSION_SAVE_INTERVAL = 1200; // Every 60 seconds (1200 ticks)

    @Override
    public void onInitializeClient() {
        NozhConstants.LOGGER.info("NOZH Client initializing...");

        // Initialize Config (ensures it's loaded before debug logger)
        ConfigManager.load();

        // Initialize Debug Logger (creates log file if debugLogs=true)
        dev.nozh.core.util.DebugLogger.init();
        dev.nozh.core.util.DebugLogger.log("INIT", "NOZH Client starting...");

        // === CORE INFRASTRUCTURE SETUP ===

        // 1. Create logger
        NozhLogger logger = new FabricNozhLogger();

        // 2. Get StateStore singleton
        stateStore = StateStore.getInstance();
        configSyncService = ConfigSyncService.start(stateStore);

        // 3. Create MinecraftOptionsAdapter
        MinecraftOptionsAdapter optionsAdapter = new ProductionMinecraftOptionsAdapter();

        // 4. Create ProviderHealthTracker and ProviderRegistry
        dev.nozh.core.capability.ProviderHealthTracker healthTracker = new dev.nozh.core.capability.ProviderHealthTracker();
        ProviderRegistry providerRegistry = new ProviderRegistry(healthTracker);
        ProviderBootstrap.registerAll(providerRegistry, optionsAdapter);
        logger.info("Registered " + providerRegistry.getAllProviders().size() + " capability providers");

        // 5. Create ActionSuccessTracker (uses in-memory storage for now)
        String storagePath = ".minecraft/config/nozh/action_success.json";
        ActionSuccessTracker successTracker = new ActionSuccessTracker(storagePath);

        // 6. Create ActionBus with StateStore supplier
        actionBus = new ActionBus(logger, stateStore::snapshot);

        // 7. Create CapabilityExecutor
        StandardCapabilityExecutor capabilityExecutor = new StandardCapabilityExecutor(providerRegistry, logger);

        // 8. Create ActionProcessor bridge
        // Create SessionLearning
        sessionLearning = new dev.nozh.core.intelligence.SessionLearning(
                net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().toFile());

        // 8. Create ActionProcessor bridge
        StandardActionProcessor actionProcessor = new StandardActionProcessor(
                capabilityExecutor,
                logger,
                sessionLearning,
                () -> perfManager != null ? perfManager.getSnapshot() : dev.nozh.api.PerfSnapshot.empty());

        // Create ScenarioDetector (Fabric implementation)
        dev.nozh.core.context.ScenarioDetector scenarioDetector = new dev.nozh.fabric.context.FabricScenarioDetector();

        // 9. Create GovernorRunner with all dependencies
        governorRunner = new GovernorRunner(
                providerRegistry,
                successTracker,
                actionBus,
                stateStore,
                logger,
                sessionLearning,
                scenarioDetector);

        logger.info("Governor system initialized");

        // === TELEMETRY SETUP ===

        // Initialize PerfManager (collects FPS data)
        perfManager = new dev.nozh.core.profiler.PerfManager();
        tickTimeSampler = new dev.nozh.core.monitoring.TickTimeSampler();

        // Register Frametime Sampler (called every frame)
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.END.register(context -> {
            perfManager.onFrame();
        });

        // === TICK HANDLER SETUP ===

        // Register tick handler for ActionBus, telemetry updates, and governor
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ConfigManager.getConfig().enabled)
                return;

            tickCounter++;

            // Update crash loop guard (stability marking)
            CrashLoopGuard.onClientTick();

            // Process ONE command per tick (Contract 2: NO CASCADE)
            actionBus.tick(actionProcessor);

            // Sample tick time (must be called once per tick)
            tickTimeSampler.onTick();

            // Update RuntimeState with telemetry data every second
            if (tickCounter % TELEMETRY_UPDATE_INTERVAL == 0) {
                updateTelemetryState();
            }

            // Run governor decision loop every 5 seconds
            if (tickCounter % GOVERNOR_POLL_INTERVAL == 0) {
                governorRunner.onTick();
            }

            if (tickCounter % SESSION_SAVE_INTERVAL == 0) {
                sessionLearning.saveIfDue();
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (sessionLearning != null) {
                sessionLearning.save();
            }
        });

        // === COMMANDS & SHUTDOWN ===

        // Register commands
        NozhCommands.register();

        // Register shutdown hook to close debug logger
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            dev.nozh.core.util.DebugLogger.log("SHUTDOWN", "NOZH shutting down...");
            dev.nozh.core.util.DebugLogger.close();
            if (sessionLearning != null) {
                sessionLearning.save();
            }
        }, "NOZH-Shutdown"));

        dev.nozh.core.util.DebugLogger.log("INIT", "NOZH Client initialized successfully");
        NozhConstants.LOGGER.info("NOZH Client initialized (FULL INTEGRATION MODE)");
    }

    /**
     * Update RuntimeState with current telemetry data.
     */
    private void updateTelemetryState() {
        try {
            var frameSnapshot = perfManager.getSnapshot();
            var tickSnapshot = tickTimeSampler.getSnapshot();
            if (frameSnapshot.sufficientData() || tickSnapshot.sufficientData()) {
                stateStore.update(state -> state.withTelemetry(
                        frameSnapshot.sufficientData() ? frameSnapshot.avgFrametimeMs() : state.avgFrametimeMs(),
                        frameSnapshot.sufficientData() ? frameSnapshot.p95FrametimeMs() : state.p95FrametimeMs(),
                        frameSnapshot.sufficientData() ? frameSnapshot.spikeCount() : state.spikeCount(),
                        tickSnapshot.sufficientData() ? tickSnapshot.avgFrametimeMs() : state.tickTimeAvg(),
                        tickSnapshot.sufficientData() ? tickSnapshot.p95FrametimeMs() : state.tickTimeP95()));
            }
        } catch (Exception e) {
            // Never crash telemetry update
            NozhConstants.LOGGER.debug("Telemetry update failed: " + e.getMessage());
        }
    }

    public static dev.nozh.core.profiler.PerfManager getPerfManager() {
        return perfManager;
    }

    public static GovernorRunner getGovernorRunner() {
        return governorRunner;
    }

    public static ActionBus getActionBus() {
        return actionBus;
    }

    public static StateStore getStateStore() {
        return stateStore;
    }
}
