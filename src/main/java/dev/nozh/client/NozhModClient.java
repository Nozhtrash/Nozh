package dev.nozh.client;

import dev.nozh.NozhConstants;
import dev.nozh.core.NozhLogger;
import dev.nozh.core.bus.ActionBus;
import dev.nozh.core.bus.StandardActionProcessor;
import dev.nozh.core.config.ConfigManager;
import dev.nozh.core.config.ConfigSyncService;
import dev.nozh.core.capability.ProviderCoverage;
import dev.nozh.core.capability.ProviderRegistry;
import dev.nozh.core.governor.GovernorRunner;
import dev.nozh.core.matrix.ActionSuccessTracker;
import dev.nozh.core.safety.CrashLoopGuard;

import dev.nozh.core.state.StateStore;
import dev.nozh.core.benchmark.InitialBenchmarkRunner;
import dev.nozh.core.benchmark.BenchmarkEnvironment;
import dev.nozh.core.benchmark.BenchmarkScenarioRecorder;
import dev.nozh.core.telemetry.TelemetryManager;
import dev.nozh.core.context.Scenario;
import dev.nozh.fabric.FabricNozhLogger;
import dev.nozh.fabric.capability.CompatAwareMinecraftOptionsAdapter;
import dev.nozh.fabric.capability.MinecraftOptionsAdapter;
import dev.nozh.fabric.capability.ProductionMinecraftOptionsAdapter;
import dev.nozh.fabric.capability.ProviderBootstrap;
import dev.nozh.fabric.capability.StandardCapabilityExecutor;
import dev.nozh.fabric.compat.CompatRegistry;
import dev.nozh.fabric.input.ManualConfirmationHandler;
import dev.nozh.client.hud.NozhHudRenderer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

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
    private static dev.nozh.core.potato.StartupOptimizer startupOptimizer;
    private static dev.nozh.core.potato.MemoryOptimizer memoryOptimizer;
    private static dev.nozh.core.potato.PotatoModeEngine potatoModeEngine;
    private static dev.nozh.core.optimization.ResourceBudgetAllocator resourceAllocator;
    private static ProviderRegistry providerRegistry;
    private static dev.nozh.fabric.context.FabricScenarioDetector scenarioDetector;
    private static InitialBenchmarkRunner initialBenchmarkRunner;
    private static BenchmarkScenarioRecorder benchmarkScenarioRecorder;
    private static KeyBinding toggleHudKey;
    private static KeyBinding applySuggestionKey;
    private static KeyBinding exportReportKey;
    private static ManualConfirmationHandler manualConfirmationHandler;
    private static TelemetryManager telemetryManager;
    private static boolean safeModeNotified = false;
    private static String lastSessionKey = null;
    private static boolean tutorialPrompted = false;
    private static boolean lastApplyKeyPressed = false;

    private static int tickCounter = 0;
    private static final int TELEMETRY_UPDATE_INTERVAL = 20; // Every second (20 ticks)
    private static final int GOVERNOR_POLL_INTERVAL = 100; // Every 5 seconds (100 ticks)
    private static final int SESSION_SAVE_INTERVAL = 1200; // Every 60 seconds (1200 ticks)
    private static final int TUTORIAL_STEP_TOGGLE = 0;
    private static final int TUTORIAL_STEP_APPLY = 1;
    private static final int TUTORIAL_STEP_EXPORT = 2;
    private static final int TUTORIAL_STEP_COMPLETE = 3;

    private static boolean initialized = false;

    @Override
    public void onInitializeClient() {
        if (initialized) {
            NozhConstants.LOGGER.warn("NOZH Client already initialized! Skipping duplicate initialization.");
            return;
        }
        initialized = true;

        NozhConstants.LOGGER.info("NOZH Client initializing...");

        // Initialize Config (ensures it's loaded before debug logger)
        ConfigManager.load();

        // Initialize Debug Logger (creates log file if debugLogs=true)
        dev.nozh.core.util.DebugLogger.init();
        dev.nozh.core.util.DebugLogger.log("INIT", "NOZH Client starting...");

        // === CORE INFRASTRUCTURE SETUP ===

        // 1. Create logger
        NozhLogger logger = new FabricNozhLogger();

        telemetryManager = new TelemetryManager();
        CrashLoopGuard.setTelemetryManager(telemetryManager);

        // 2. Get StateStore singleton
        stateStore = StateStore.getInstance();
        configSyncService = ConfigSyncService.start(stateStore);

        // 3. Create MinecraftOptionsAdapter
        MinecraftOptionsAdapter optionsAdapter = new CompatAwareMinecraftOptionsAdapter(
                new ProductionMinecraftOptionsAdapter(),
                new CompatRegistry());

        // 4. Create ProviderHealthTracker and ProviderRegistry
        dev.nozh.core.capability.ProviderHealthTracker healthTracker = new dev.nozh.core.capability.ProviderHealthTracker();
        providerRegistry = new ProviderRegistry(healthTracker);
        ProviderBootstrap.registerAll(providerRegistry, optionsAdapter);
        logger.info("Registered " + providerRegistry.getAllProviders().size() + " capability providers");
        ProviderCoverage coverage = providerRegistry.coverage();
        logger.info(String.format("Capability coverage: %.1f%% (%d/%d)",
                coverage.coveragePercent(),
                coverage.controlledCapabilities(),
                coverage.totalCapabilities()));

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

        // Initialize PerfManager (collects FPS data)
        perfManager = new dev.nozh.core.profiler.PerfManager();
        telemetryManager.setCriticalEventSink(perfManager);
        tickTimeSampler = new dev.nozh.core.monitoring.TickTimeSampler();
        perfManager.setTickSnapshotSupplier(() -> tickTimeSampler.getSnapshot());
        if (ConfigManager.getConfig().benchmarkModeEnabled) {
            benchmarkScenarioRecorder = new BenchmarkScenarioRecorder(
                    perfManager,
                    () -> perfManager != null ? perfManager.getSnapshot() : dev.nozh.api.PerfSnapshot.empty(),
                    NozhConstants.CONFIG_DIR.resolve("benchmark_artifacts"));
        }
        initialBenchmarkRunner = new InitialBenchmarkRunner(stateStore,
                () -> perfManager != null ? perfManager.getSnapshot() : dev.nozh.api.PerfSnapshot.empty(),
                snapshot -> {
                    if (benchmarkScenarioRecorder != null) {
                        benchmarkScenarioRecorder.recordInitialBenchmarkSnapshot(snapshot);
                    }
                });

        // Initialize Module 3 Components
        dev.nozh.core.potato.StartupOptimizer startupOptimizer = new dev.nozh.core.potato.StartupOptimizer();
        startupOptimizer.beginStartup();
        startupOptimizer.beginPhase(dev.nozh.core.potato.StartupOptimizer.StartupPhase.INIT);

        resourceAllocator = new dev.nozh.core.optimization.ResourceBudgetAllocator();
        memoryOptimizer = new dev.nozh.core.potato.MemoryOptimizer();
        dev.nozh.core.potato.PotatoConfigApplicator applicator = new dev.nozh.fabric.potato.FabricPotatoConfigApplicator(
                optionsAdapter);
        potatoModeEngine = new dev.nozh.core.potato.PotatoModeEngine(applicator);

        // Initial Potato Check
        potatoModeEngine.autoConfigure(ConfigManager.getConfig());
        ConfigManager.addListener(config -> {
            if (potatoModeEngine != null) {
                potatoModeEngine.autoConfigure(config);
            }
        });

        // 8. Create ActionProcessor bridge
        StandardActionProcessor actionProcessor = new StandardActionProcessor(
                capabilityExecutor,
                logger,
                sessionLearning,
                () -> perfManager != null ? perfManager.getSnapshot() : dev.nozh.api.PerfSnapshot.empty(),
                successTracker);

        // Create ScenarioDetector (Fabric implementation) - FIXED: Added
        // MinecraftClient parameter
        MinecraftClient client = MinecraftClient.getInstance();
        scenarioDetector = new dev.nozh.fabric.context.FabricScenarioDetector(client);

        // 9. Create GovernorRunner with all dependencies
        governorRunner = new GovernorRunner(
                providerRegistry,
                successTracker,
                actionBus,
                stateStore,
                logger,
                sessionLearning,
                perfManager,
                scenarioDetector,
                () -> perfManager != null ? perfManager.getSnapshot() : dev.nozh.api.PerfSnapshot.empty());

        logger.info("Governor system initialized");

        // === TELEMETRY SETUP ===

        toggleHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.nozh.toggle_hud",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "category.nozh"));

        applySuggestionKey = dev.nozh.core.manual.ManualConfirmKeybind.KEY;

        exportReportKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.nozh.export_report",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "category.nozh"));

        manualConfirmationHandler = new ManualConfirmationHandler(
                stateStore,
                actionBus,
                null,
                applySuggestionKey,
                null);

        // Register Frametime Sampler (called every frame)
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.START.register(context -> {
            if (perfManager != null) {
                perfManager.onRenderFrameStart();
                perfManager.onRenderPhaseStart(dev.nozh.core.profiler.RenderPhase.WORLD);
            }
        });

        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.END.register(context -> {
            if (perfManager != null) {
                perfManager.onRenderPhaseEnd(dev.nozh.core.profiler.RenderPhase.WORLD);
                perfManager.onFrame();
                perfManager.onRenderFrameEnd();
            }
        });

        HudRenderCallback.EVENT.register(new NozhHudRenderer(
                stateStore,
                providerRegistry,
                () -> {
                    var gov = dev.nozh.NozhMod.getGovernor();
                    return (gov != null && gov.getTelemetryService() != null)
                            ? gov.getTelemetryService().getSnapshot()
                            : dev.nozh.core.telemetry.TelemetrySnapshot.EMPTY;
                },
                perfManager,
                NozhModClient::resolveExportKeyLabel));

        // === TICK HANDLER SETUP ===

        // Register tick handler for ActionBus, telemetry updates, and governor
        ClientTickEvents.START_CLIENT_TICK.register(clientInstance -> {
            if (perfManager != null) {
                perfManager.onRenderPhaseStart(dev.nozh.core.profiler.RenderPhase.CLIENT_TICK);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(clientInstance -> {
            if (!ConfigManager.getConfig().enabled) {
                if (perfManager != null) {
                    perfManager.onRenderPhaseEnd(dev.nozh.core.profiler.RenderPhase.CLIENT_TICK);
                }
                return;
            }

            tickCounter++;

            // Complete startup tracking on first tick
            if (tickCounter == 1 && startupOptimizer != null) {
                startupOptimizer.completeStartup();
            }

            // Update crash loop guard (stability marking)
            CrashLoopGuard.onClientTick();

            // Process ONE command per tick (Contract 2: NO CASCADE)
            actionBus.tick(actionProcessor);

            // Sample tick time (must be called once per tick)
            tickTimeSampler.onTick();
            if (perfManager != null) {
                perfManager.onTickSample(tickTimeSampler.getLastTickMs());
            }

            if (scenarioDetector != null) {
                scenarioDetector.tick();
            }

            if (toggleHudKey != null) {
                while (toggleHudKey.wasPressed()) {
                    var config = ConfigManager.getConfig();
                    config.showHud = !config.showHud;
                    ConfigManager.saveAndNotify();
                    advanceTutorialIfExpected(clientInstance, TUTORIAL_STEP_TOGGLE);
                }
            }

            if (exportReportKey != null) {
                while (exportReportKey.wasPressed()) {
                    exportHudReport(clientInstance);
                    advanceTutorialIfExpected(clientInstance, TUTORIAL_STEP_EXPORT);
                }
            }

            if (!safeModeNotified && CrashLoopGuard.isInSafeMode()) {
                safeModeNotified = true;
                notifyClient(clientInstance,
                        Text.translatable("nozh.safemode.entered.title"),
                        Text.translatable("nozh.safemode.entered.message"));
            }

            // Update RuntimeState with telemetry data every second
            if (tickCounter % TELEMETRY_UPDATE_INTERVAL == 0) {
                updateTelemetryState();
                if (scenarioDetector != null) {
                    scenarioDetector.logTelemetry();
                }

                // GOD MODE: Reactive Sodium Control
                // Checks FPS every second and downscales if needed
                if (perfManager != null) {
                    dev.nozh.api.PerfSnapshot snapshot = perfManager.getSnapshot();
                    if (snapshot.sufficientData()) {
                        dev.nozh.fabric.compat.SodiumAdapterExpanded.ReactiveController.captureState();
                        dev.nozh.fabric.compat.SodiumAdapterExpanded.ReactiveController.optimize(
                                snapshot.avgFrametimeMs() > 0 ? 1000.0 / snapshot.avgFrametimeMs() : 60.0,
                                ConfigManager.getConfig().targetFps);
                    }
                }
            }

            // Run governor decision loop every 5 seconds
            if (tickCounter % GOVERNOR_POLL_INTERVAL == 0) {
                governorRunner.onTick();
                // Also update Potato Mode checks occasionally
                if (potatoModeEngine != null) {
                    potatoModeEngine.update();
                }
            }

            // High frequency updates
            if (memoryOptimizer != null) {
                memoryOptimizer.tick();
                // Feed memory pressure to resource allocator
                if (resourceAllocator != null) {
                    resourceAllocator.setMemoryPressure(memoryOptimizer.getMemoryPressure());
                }
            }

            if (tickCounter % SESSION_SAVE_INTERVAL == 0) {
                sessionLearning.saveIfDue();
            }

            if (initialBenchmarkRunner != null) {
                initialBenchmarkRunner.tick();
            }
            if (benchmarkScenarioRecorder != null) {
                Scenario scenario = stateStore.snapshotSafe().currentScenario();
                benchmarkScenarioRecorder.tick(scenario);
            }

            handleTutorial(clientInstance);

            if (perfManager != null) {
                perfManager.onRenderPhaseEnd(dev.nozh.core.profiler.RenderPhase.CLIENT_TICK);
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, clientInstance) -> {
            if (sessionLearning != null) {
                sessionLearning.save();
            }
            if (initialBenchmarkRunner != null) {
                initialBenchmarkRunner.onSessionEnd();
            }
            if (benchmarkScenarioRecorder != null) {
                benchmarkScenarioRecorder.onSessionEnd();
            }
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, clientInstance) -> {
            if (governorRunner != null) {
                governorRunner.captureBaselineSettings();
            }
            if (sessionLearning != null) {
                String sessionKey = resolveSessionKey(clientInstance, handler);
                if (lastSessionKey == null || !lastSessionKey.equals(sessionKey)) {
                    sessionLearning.resetForSession(sessionKey);
                    sessionLearning.save();
                    lastSessionKey = sessionKey;
                }
            }
            if (initialBenchmarkRunner != null) {
                initialBenchmarkRunner.onSessionStart();
            }
            if (benchmarkScenarioRecorder != null) {
                benchmarkScenarioRecorder.onSessionStart(buildBenchmarkEnvironment(clientInstance));
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
                        frameSnapshot.sufficientData() ? frameSnapshot.p99FrametimeMs() : state.p99FrametimeMs(),
                        frameSnapshot.sufficientData() ? frameSnapshot.frametimeStddevMs()
                                : state.frametimeStddevMs(),
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

    public static dev.nozh.fabric.context.FabricScenarioDetector getScenarioDetector() {
        return scenarioDetector;
    }

    private BenchmarkEnvironment buildBenchmarkEnvironment(MinecraftClient clientInstance) {
        String hardwareProfile = ConfigManager.getConfig().hardwareProfile;
        String resolution = "unknown";
        if (clientInstance != null && clientInstance.getWindow() != null) {
            resolution = clientInstance.getWindow().getFramebufferWidth()
                    + "x" + clientInstance.getWindow().getFramebufferHeight();
        }
        int modCount = FabricLoader.getInstance().getAllMods().size();
        String modsSummary = "mods=" + modCount;
        String workloadProfile = ConfigManager.getConfig().optimizationProfile;
        return new BenchmarkEnvironment(hardwareProfile, resolution, modsSummary, workloadProfile);
    }

    public static void requestSuggestedAction() {
        if (manualConfirmationHandler == null) {
            return;
        }
        manualConfirmationHandler.requestApply();
    }

    private static void exportHudReport(MinecraftClient client) {
        if (perfManager == null) {
            notifyClient(client,
                    Text.translatable("nozh.telemetry.export.failed"),
                    Text.translatable("nozh.telemetry.export.unavailable"));
            return;
        }
        try {
            var output = perfManager.exportTelemetry(
                    NozhConstants.CONFIG_DIR.resolve("telemetry_exports"),
                    dev.nozh.core.telemetry.TelemetryExportFormat.JSON);
            notifyClient(client,
                    Text.translatable("nozh.telemetry.export.success", output.toString()),
                    Text.translatable("nozh.telemetry.export.complete"));
        } catch (Exception e) {
            notifyClient(client,
                    Text.translatable("nozh.telemetry.export.failed"),
                    Text.literal(e.getMessage() != null ? e.getMessage() : "Error"));
        }
    }

    private static String resolveExportKeyLabel() {
        if (exportReportKey == null) {
            return "";
        }
        return exportReportKey.getBoundKeyLocalizedText().getString();
    }

    private static void notifyClient(MinecraftClient client, Text title, Text message) {
        if (client == null) {
            return;
        }
        if (client.getToastManager() != null) {
            SystemToast.add(client.getToastManager(), SystemToast.Type.TUTORIAL_HINT, title, message);
        }
        if (client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(title.copy().append(Text.literal(" ")).append(message));
        }
    }

    private static void handleTutorial(MinecraftClient client) {
        var config = ConfigManager.getConfig();
        if (config == null || config.tutorialStep >= TUTORIAL_STEP_COMPLETE) {
            return;
        }
        if (!tutorialPrompted) {
            showTutorialStep(client, config.tutorialStep);
            tutorialPrompted = true;
        }

        boolean applyPressed = applySuggestionKey != null && applySuggestionKey.isPressed();
        if (config.tutorialStep == TUTORIAL_STEP_APPLY && applyPressed && !lastApplyKeyPressed) {
            advanceTutorial(client);
        }
        lastApplyKeyPressed = applyPressed;
    }

    private static void advanceTutorialIfExpected(MinecraftClient client, int expectedStep) {
        var config = ConfigManager.getConfig();
        if (config == null || config.tutorialStep != expectedStep) {
            return;
        }
        advanceTutorial(client);
    }

    private static void advanceTutorial(MinecraftClient client) {
        var config = ConfigManager.getConfig();
        if (config == null) {
            return;
        }
        config.tutorialStep++;
        ConfigManager.saveAndNotify();
        if (config.tutorialStep >= TUTORIAL_STEP_COMPLETE) {
            notifyClient(client,
                    Text.translatable("nozh.tutorial.complete.title"),
                    Text.translatable("nozh.tutorial.complete.message"));
            tutorialPrompted = true;
            return;
        }
        showTutorialStep(client, config.tutorialStep);
        tutorialPrompted = true;
    }

    private static void showTutorialStep(MinecraftClient client, int step) {
        if (client == null) {
            return;
        }
        if (step == TUTORIAL_STEP_TOGGLE) {
            notifyClient(client,
                    Text.translatable("nozh.tutorial.welcome.title"),
                    Text.translatable("nozh.tutorial.welcome.message", resolveKeyLabel(toggleHudKey)));
        } else if (step == TUTORIAL_STEP_APPLY) {
            notifyClient(client,
                    Text.translatable("nozh.tutorial.step.apply.title"),
                    Text.translatable("nozh.tutorial.step.apply.message", resolveKeyLabel(applySuggestionKey)));
        } else if (step == TUTORIAL_STEP_EXPORT) {
            notifyClient(client,
                    Text.translatable("nozh.tutorial.step.export.title"),
                    Text.translatable("nozh.tutorial.step.export.message", resolveKeyLabel(exportReportKey)));
        }
    }

    private static String resolveKeyLabel(KeyBinding keyBinding) {
        if (keyBinding == null) {
            return Text.translatable("nozh.hud.export.unbound").getString();
        }
        String label = keyBinding.getBoundKeyLocalizedText().getString();
        if (label == null || label.isBlank()) {
            return Text.translatable("nozh.hud.export.unbound").getString();
        }
        return label;
    }

    private static String resolveSessionKey(MinecraftClient client,
            net.minecraft.client.network.ClientPlayNetworkHandler handler) {
        if (client != null && client.isIntegratedServerRunning() && client.getServer() != null) {
            try {
                String levelName = client.getServer().getSaveProperties().getLevelName();
                if (levelName != null && !levelName.isBlank()) {
                    return "local:" + levelName;
                }
            } catch (Exception ignored) {
            }
        }
        if (handler != null && handler.getConnection() != null && handler.getConnection().getAddress() != null) {
            return "remote:" + handler.getConnection().getAddress().toString();
        }
        return "unknown";
    }

}
