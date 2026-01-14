package dev.nozh.core.governor;

import dev.nozh.NozhConstants;
import dev.nozh.core.capability.ProviderExecutor;
import dev.nozh.core.capability.ProviderRegistry;
import dev.nozh.core.capability.ProviderHealthTracker;
import dev.nozh.core.cloud.CloudManager;
import dev.nozh.core.compatibility.ModConflictDetector;
import dev.nozh.core.config.AdaptiveConfigManager;
import dev.nozh.core.config.ConfigManager;
import dev.nozh.core.config.NozhConfig;
import dev.nozh.core.context.CameraActivityTracker;
import dev.nozh.core.context.Scenario;
import dev.nozh.core.context.ScenarioSnapshot;
import dev.nozh.core.governor.TelemetryService;
import dev.nozh.core.governor.DecisionEngine;
import dev.nozh.core.intelligence.AnomalyDetector;
import dev.nozh.core.telemetry.TelemetrySample;
import dev.nozh.core.telemetry.TelemetrySnapshot;
import dev.nozh.core.safety.ProviderBlacklist;
import dev.nozh.fabric.context.EnhancedFabricScenarioDetector;
import dev.nozh.fabric.telemetry.FabricFrameTickSampler;
import dev.nozh.core.monitoring.NetworkLatencyTracker;
import dev.nozh.core.telemetry.VitalsRecorder;
import net.minecraft.client.MinecraftClient;

import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Integrated Governor - The main orchestration brain.
 * <p>
 * REFACTORED (Audit 2026): Now acts as a coordinator rather than a God Object.
 * Delegates logic to {@link TelemetryService} and {@link DecisionEngine}.
 */
public final class IntegratedGovernor {

    // Core systems
    private final MinecraftClient client;

    // Services (Refactored)
    private final TelemetryService telemetryService;
    private final DecisionEngine decisionEngine;

    // Inputs / Detectors
    private final EnhancedFabricScenarioDetector scenarioDetector;
    private final FabricFrameTickSampler frameTickSampler;
    private final ModConflictDetector modConflictDetector;
    private final CameraActivityTracker cameraTracker;
    private final NetworkLatencyTracker latencyTracker;
    private final VitalsRecorder vitalsRecorder;

    // Execution
    private final ProviderRegistry providerRegistry;
    private final ProviderExecutor providerExecutor;
    private final ScheduledExecutorService asyncExecutor;
    private final ConcurrentHashMap<String, CompletableFuture<ProviderExecutor.ExecutionResult>> pendingActions = new ConcurrentHashMap<>();

    // Config & Safety
    private final AdaptiveConfigManager configManager;
    private final ProviderBlacklist blacklist;

    // State
    private volatile Scenario currentScenario = Scenario.STANDARD;
    private volatile DecisionReasoning lastDecisionReasoning = null;
    private final AtomicLong lastDecisionTimeRaw = new AtomicLong(Double.doubleToRawLongBits(0.0));
    private final AtomicInteger tickCounter = new AtomicInteger(0);
    private volatile boolean initialized = false;
    private volatile boolean safeModeActive = false;

    // --- Constructor ---

    public IntegratedGovernor(MinecraftClient client, Path logPath) {
        this(client, logPath, false);
    }

    public IntegratedGovernor(MinecraftClient client, Path logPath, boolean forceSafeMode) {
        if (client == null)
            throw new NullPointerException("MinecraftClient cannot be null");
        if (logPath == null)
            throw new NullPointerException("Log path cannot be null");

        this.client = client;

        // 1. Initialize Director Mode wiring (Hardware/Input)
        this.frameTickSampler = new FabricFrameTickSampler(client);
        this.modConflictDetector = new ModConflictDetector();
        double cpuBias = modConflictDetector.getCpuBiasAdjustment();
        double gpuBias = modConflictDetector.getGpuBiasAdjustment();

        // 2. Initialize Detectors
        this.scenarioDetector = new EnhancedFabricScenarioDetector(client);
        this.cameraTracker = new CameraActivityTracker(client);
        this.latencyTracker = new NetworkLatencyTracker();
        this.vitalsRecorder = new VitalsRecorder();

        // 3. Initialize Config
        this.configManager = new AdaptiveConfigManager();
        double targetFps = safeGetTargetFps();

        // 4. Initialize Services
        this.telemetryService = new TelemetryService(logPath);

        AnomalyDetector anomalyDetector = new AnomalyDetector(this.latencyTracker);
        this.decisionEngine = new DecisionEngine(configManager, anomalyDetector, targetFps, cpuBias, gpuBias);

        // 5. Initialize Safety & Execution
        this.blacklist = new ProviderBlacklist();
        this.blacklist.initializeDefaults();

        this.asyncExecutor = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "Governor-Async");
            t.setDaemon(true);
            return t;
        });

        ProviderHealthTracker healthTracker = new ProviderHealthTracker();
        this.providerRegistry = new ProviderRegistry(healthTracker);
        ProviderRegistry.discoverProviders(this.providerRegistry);
        this.providerExecutor = new ProviderExecutor(this.providerRegistry, this.asyncExecutor);

        // 6. Safe Mode Handling
        if (forceSafeMode) {
            activateSafeMode();
        }

        this.initialized = true;
        NozhConstants.LOGGER.info("IntegratedGovernor (Refactored) initialized - Coordinators active");
    }

    // --- Main Loop ---

    public void tick() {
        if (!initialized || client == null || client.world == null)
            return;

        try {
            int ticks = tickCounter.incrementAndGet();

            // 1. Update Context Trackers
            if (cameraTracker != null)
                cameraTracker.tick();

            // 2. Collect Data
            TelemetrySample sample = collectTelemetry();
            if (sample == null)
                return;

            // 3. Process Telemetry (Buffer, Log, Monitor) - PROCESS FIRST for fresh data
            telemetryService.processSample(sample, ticks);

            // 4. Enrich State for AI (Feeding FRESH data to DecisionEngine)
            int entityCount = client.world.getRegularEntityCount();
            int particleCount = frameTickSampler.getParticleCount();
            int chunkUpdates = frameTickSampler.getChunkUpdateCount();
            double speed = (client.player != null) ? client.player.getVelocity().length() : 0.0;

            // Intelligence update with LATEST snapshot
            decisionEngine.feedPredictors(
                    telemetryService.getSnapshot(),
                    ticks, entityCount, particleCount, chunkUpdates, speed);

            // 5. Vitals
            if (vitalsRecorder != null) {
                vitalsRecorder.recordFrame((float) sample.frametimeMs());
            }

            // 6. Configured Decision Loop
            double decisionInterval = configManager.getValue("decision_interval_ms", 2000.0);
            double now = System.currentTimeMillis();
            if (now - getLastDecisionTime() >= decisionInterval) {
                makeDecision();
                setLastDecisionTime(now);
            }

        } catch (Exception e) {
            NozhConstants.LOGGER.error("Governor tick error", e);
            telemetryService.recordError("tick_error: " + e.getMessage());
        }
    }

    // --- Decision Making ---

    private void makeDecision() {
        // Safety First
        if (safeModeActive) {
            if (tickCounter.get() % 200 == 0)
                NozhConstants.LOGGER.debug("Safe Mode active: Optimization suspended.");
            return;
        }

        TelemetrySnapshot snapshot = telemetryService.getSnapshot();
        if (snapshot == null)
            return;

        // 1. Detect Scenario
        Scenario detected = Scenario.STANDARD;
        try {
            ScenarioSnapshot scenarioSnap = scenarioDetector.detect();
            if (scenarioSnap != null)
                detected = scenarioSnap.scenario();
        } catch (Exception e) {
            /* ignore */ }

        currentScenario = detected;

        // 2. Calculate FPS
        double currentFps = 1000.0 / snapshot.avgFrametimeMs();
        if (!Double.isFinite(currentFps) || currentFps <= 0)
            return;

        // 3. Consult Decision Engine
        double targetFps = safeGetTargetFps();
        boolean shouldOptimize = decisionEngine.shouldOptimize(snapshot, currentFps, targetFps);

        if (!shouldOptimize) {
            if (tickCounter.get() % 200 == 0) {
                NozhConstants.LOGGER.debug("Performance stable: {:.1f} FPS", currentFps);
            }
            return;
        }

        // 4. Select Action
        String[] availableActions = getAvailableActions(); // From where? We need to implement this helper or delegate

        String selectedAction = decisionEngine.selectAction(currentScenario, currentFps, availableActions);

        if (selectedAction == null) {
            NozhConstants.LOGGER.debug("No suitable action selected.");
            return;
        }

        // 5. Execute
        DecisionReasoning reasoning = DecisionReasoning.create(
                currentScenario, currentFps, targetFps, 0.0, 0.0, false, snapshot.spikeCount());
        this.lastDecisionReasoning = reasoning;

        executeAction(selectedAction, reasoning, currentFps);
    }

    private void executeAction(String actionId, DecisionReasoning reasoning, double fpsBefore) {
        if (pendingActions.containsKey(actionId))
            return;

        long startTime = System.currentTimeMillis();
        double expectedFpsDelta = configManager.getValue("expected_fps_delta", 15.0);

        decisionEngine.getEffectivenessTracker().recordActionStart(actionId, expectedFpsDelta, reasoning);

        NozhConstants.LOGGER.info("Governor requesting action: {}", actionId);

        // Safe execution via ProviderExecutor (which now handles thread safety)
        CompletableFuture<ProviderExecutor.ExecutionResult> future = providerExecutor.executeAction(actionId);

        // Track completion & Cleanup using whenComplete for robustness
        future.whenComplete((result, ex) -> {
            boolean success = (result != null && result.isSuccess());
            long duration = System.currentTimeMillis() - startTime;

            if (ex != null) {
                NozhConstants.LOGGER.error("Action '{}' failed exceptionally", actionId, ex);
            }

            // Post-execution measurement & learning (async)
            asyncExecutor.schedule(() -> {
                try {
                    TelemetrySnapshot after = telemetryService.getSnapshot();
                    double fpsAfter = (after != null) ? 1000.0 / after.avgFrametimeMs() : fpsBefore;
                    double actualDelta = fpsAfter - fpsBefore;

                    // Feed back to learning engine
                    decisionEngine.getEffectivenessTracker().recordActionResult(actionId, actualDelta, success);

                    NozhConstants.LOGGER.info("Action '{}' completed. Result: {}, Delta: {:.1f} FPS",
                            actionId, success ? "Success" : "Failed", actualDelta);
                } finally {
                    // Robust cleanup: Always remove from pending map
                    pendingActions.remove(actionId);
                }
            }, 1500, TimeUnit.MILLISECONDS);
        });

        pendingActions.put(actionId, future);
    }

    // --- Helpers ---

    private TelemetrySample collectTelemetry() {
        // Simple delegate to sampling logic
        if (frameTickSampler == null)
            return null;
        if (client.world == null)
            return null;

        double frametimeMs = frameTickSampler.getLastRenderMs();
        double tickMs = frameTickSampler.getLastTickMs();
        int fps = (frametimeMs > 0) ? (int) (1000.0 / frametimeMs) : client.getCurrentFps();
        int entities = client.world.getRegularEntityCount();
        int chunks = client.world.getChunkManager().getLoadedChunkCount();

        return new TelemetrySample(
                System.currentTimeMillis(),
                frametimeMs,
                tickMs,
                fps,
                entities,
                chunks,
                -1, // drawCalls (unavailable)
                0, // droppedSamples (fresh)
                frameTickSampler.getConsecutiveSlowFrames(), // Layer 1
                frameTickSampler.getMaxChunkEntityCount(), // Layer 2
                frameTickSampler.getDenseChunkCount() // Layer 2
        );
    }

    private double safeGetTargetFps() {
        try {
            return configManager.getValue("target_fps", 60.0);
        } catch (Exception e) {
            return 60.0;
        }
    }

    private void activateSafeMode() {
        this.safeModeActive = true;
        NozhConstants.LOGGER.warn("[NOZH] Safe Mode ACTIVATED");
        try {
            NozhConfig config = ConfigManager.getConfig();
            config.allowAutoTuning = false;
            ConfigManager.saveNow();
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to force safe config", e);
        }
    }

    private String[] getAvailableActions() {
        // Naive implementation for refactor compatibility - ideally move to a Registry
        // Helper
        return providerRegistry.getRegisteredProviderIds().toArray(new String[0]);
    }

    private double getLastDecisionTime() {
        return Double.longBitsToDouble(lastDecisionTimeRaw.get());
    }

    private void setLastDecisionTime(double time) {
        lastDecisionTimeRaw.set(Double.doubleToRawLongBits(time));
    }

    // --- Public API for Commands ---

    public boolean isInitialized() {
        return initialized;
    }

    public DecisionReasoning getLastDecisionReasoning() {
        return lastDecisionReasoning;
    }

    public String getHealthStatus() {
        return telemetryService.getHealthMonitor().getHealthStatus();
    }

    public String getHealthReport() {
        return telemetryService.getHealthMonitor().generateHealthReport();
    }

    public java.util.Map<String, Object> getLearningStats() {
        return decisionEngine.getLearningEngine().getStatistics();
    }

    public java.util.Map<String, Object> getMetricsSummary() {
        return telemetryService.getMetricsCollector().getSummary();
    }

    public double getActionEffectiveness(String actionId) {
        return decisionEngine.getEffectivenessTracker().getEffectivenessScore(actionId);
    }

    public void resetLearning() {
        decisionEngine.getLearningEngine().reset();
        // Also reset internal tracker
        decisionEngine.getEffectivenessTracker().clear();
    }

    public VitalsRecorder getVitalsRecorder() {
        return vitalsRecorder;
    }

    public TelemetryService getTelemetryService() {
        return telemetryService;
    }

    public void shutdown() {
        asyncExecutor.shutdown();
        CloudManager.getInstance().shutdown();
    }
}
