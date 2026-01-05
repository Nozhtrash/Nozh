package dev.nozh.client;

import dev.nozh.NozhConstants;
import dev.nozh.core.NozhLogger;
import dev.nozh.core.bus.ActionBus;
import dev.nozh.core.bus.Command;
import dev.nozh.core.bus.StandardActionProcessor;
import dev.nozh.core.config.ConfigManager;
import dev.nozh.core.config.ConfigSyncService;
import dev.nozh.core.capability.ProviderRegistry;
import dev.nozh.core.governor.GovernorRunner;
import dev.nozh.core.governor.ActionOutcome;
import dev.nozh.core.matrix.ActionSuccessTracker;
import dev.nozh.core.safety.CrashLoopGuard;
import dev.nozh.core.state.PendingAction;
import dev.nozh.core.state.RuntimeState;
import dev.nozh.core.state.ActionHistoryEntry;
import dev.nozh.core.state.StateStore;
import dev.nozh.fabric.FabricNozhLogger;
import dev.nozh.fabric.capability.CompatAwareMinecraftOptionsAdapter;
import dev.nozh.fabric.capability.MinecraftOptionsAdapter;
import dev.nozh.fabric.capability.ProductionMinecraftOptionsAdapter;
import dev.nozh.fabric.capability.ProviderBootstrap;
import dev.nozh.fabric.capability.StandardCapabilityExecutor;
import dev.nozh.fabric.compat.CompatRegistry;
import dev.nozh.client.hud.NozhHudRenderer;
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

import java.util.function.Consumer;

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
    private static ProviderRegistry providerRegistry;
    private static KeyBinding toggleHudKey;
    private static KeyBinding applySuggestionKey;
    private static boolean safeModeNotified = false;
    private static String lastSessionKey = null;

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
        MinecraftOptionsAdapter optionsAdapter = new CompatAwareMinecraftOptionsAdapter(
                new ProductionMinecraftOptionsAdapter(),
                new CompatRegistry());

        // 4. Create ProviderHealthTracker and ProviderRegistry
        dev.nozh.core.capability.ProviderHealthTracker healthTracker = new dev.nozh.core.capability.ProviderHealthTracker();
        providerRegistry = new ProviderRegistry(healthTracker);
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
                () -> perfManager != null ? perfManager.getSnapshot() : dev.nozh.api.PerfSnapshot.empty(),
                successTracker);

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
                scenarioDetector,
                () -> perfManager != null ? perfManager.getSnapshot() : dev.nozh.api.PerfSnapshot.empty());

        logger.info("Governor system initialized");

        // === TELEMETRY SETUP ===

        // Initialize PerfManager (collects FPS data)
        perfManager = new dev.nozh.core.profiler.PerfManager();
        tickTimeSampler = new dev.nozh.core.monitoring.TickTimeSampler();

        toggleHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.nozh.toggle_hud",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "category.nozh"));

        applySuggestionKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.nozh.apply_suggestion",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.nozh"));

        // Register Frametime Sampler (called every frame)
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.END.register(context -> {
            perfManager.onFrame();
        });

        HudRenderCallback.EVENT.register(new NozhHudRenderer(
                stateStore,
                providerRegistry,
                () -> perfManager != null ? perfManager.getSnapshot() : dev.nozh.api.PerfSnapshot.empty()));

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

            if (toggleHudKey != null) {
                while (toggleHudKey.wasPressed()) {
                    var config = ConfigManager.getConfig();
                    config.showHud = !config.showHud;
                    ConfigManager.saveAndNotify();
                }
            }

            if (applySuggestionKey != null) {
                while (applySuggestionKey.wasPressed()) {
                    applySuggestedAction(client);
                }
            }

            if (!safeModeNotified && CrashLoopGuard.isInSafeMode()) {
                safeModeNotified = true;
                notifyClient(client,
                        Text.translatable("nozh.safemode.entered.title"),
                        Text.translatable("nozh.safemode.entered.message"));
            }

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

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (governorRunner != null) {
                governorRunner.captureBaselineSettings();
            }
            if (sessionLearning != null) {
                String sessionKey = resolveSessionKey(client, handler);
                if (lastSessionKey == null || !lastSessionKey.equals(sessionKey)) {
                    sessionLearning.resetForSession(sessionKey);
                    sessionLearning.save();
                    lastSessionKey = sessionKey;
                }
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

    public static KeyBinding getApplySuggestionKey() {
        return applySuggestionKey;
    }

    public static void applySuggestedAction(MinecraftClient client) {
        if (client == null) {
            return;
        }
        var actionBus = getActionBus();
        if (actionBus == null) {
            notifyClient(client,
                    Text.translatable("nozh.suggestion.apply.failed"),
                    Text.translatable("nozh.suggestion.apply.unavailable"));
            return;
        }

        RuntimeState state = StateStore.getInstance().snapshotSafe();
        if (state.suggestedActions() == null || state.suggestedActions().isEmpty()) {
            notifyClient(client,
                    Text.translatable("nozh.suggestion.apply.none"),
                    Text.translatable("nozh.suggestion.apply.none_detail"));
            return;
        }

        PendingAction pending = state.suggestedActions().get(0);
        var command = pending.command();
        long now = System.currentTimeMillis();
        int maxHistoryEntries = ConfigManager.getConfig() != null ? ConfigManager.getConfig().historyMaxEntries : 50;
        ActionHistoryEntry actionEntry = new ActionHistoryEntry(
                now,
                pending.capability().name() + "=" + pending.newValue(),
                pending.scenario(),
                pending.scenarioConfidence(),
                pending.baselineSnapshot(),
                dev.nozh.api.PerfSnapshot.empty(),
                0.0,
                0,
                0,
                ActionOutcome.NEUTRAL,
                false);

        actionBus.dispatch(command, report -> {
            if (report.succeeded()) {
                notifyClient(client,
                        Text.translatable("nozh.suggestion.apply.success"),
                        Text.translatable("nozh.suggestion.apply.success_detail",
                                pending.capability().name(), pending.newValue().toString()));
            } else {
                String reason = report.error().orElse("unknown");
                notifyClient(client,
                        Text.translatable("nozh.suggestion.apply.failed"),
                        Text.translatable("nozh.suggestion.apply.failed_detail", reason));
                StateStore.getInstance().update(RuntimeState::withPendingActionCleared);
            }
        });

        StateStore.getInstance().update(currentState -> currentState
                .withAppliedSuggestion(now, pending, actionEntry, maxHistoryEntries));
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
