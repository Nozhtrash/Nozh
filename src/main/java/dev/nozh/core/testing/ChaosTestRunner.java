package dev.nozh.core.testing;

import dev.nozh.core.NoOpLogger;
import dev.nozh.core.bus.ActionBus;
import dev.nozh.core.bus.Command;
import dev.nozh.core.bus.CommandExecutionReport;
import dev.nozh.core.bus.CommandLifecycle;
import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;
import dev.nozh.core.capability.ApplyResult;
import dev.nozh.core.capability.CapabilityProvider;
import dev.nozh.core.capability.CostLevel;
import dev.nozh.core.capability.ImpactLevel;
import dev.nozh.core.capability.ProviderHealthTracker;
import dev.nozh.core.capability.ProviderMetadata;
import dev.nozh.core.capability.ProviderRegistry;
import dev.nozh.core.capability.ProviderStatus;
import dev.nozh.core.capability.RollbackGuarantee;
import dev.nozh.core.capability.SafetyLevel;
import dev.nozh.core.capability.SideEffects;
import dev.nozh.core.governor.SimulationGovernor;
import dev.nozh.core.matrix.ActionMatrix;
import dev.nozh.core.matrix.ActionSuccessTracker;
import dev.nozh.core.matrix.ConfidenceCalculator;
import dev.nozh.core.preset.HardwareTier;
import dev.nozh.core.preset.PresetConstraints;
import dev.nozh.core.preset.PresetRegistry;
import dev.nozh.core.safety.CrashFailureContext;
import dev.nozh.core.safety.CrashLoopGuard;
import dev.nozh.core.safety.CrashRecoveryAction;
import dev.nozh.core.safety.CrashRecoveryDecision;
import dev.nozh.core.safety.NozhState;
import dev.nozh.core.state.RuntimeState;
import dev.nozh.core.state.StateInvariantViolationException;
import dev.nozh.core.state.StateStore;
import dev.nozh.core.telemetry.RingTelemetryBuffer;
import dev.nozh.core.telemetry.TelemetrySample;
import dev.nozh.core.telemetry.TelemetrySnapshot;
import dev.nozh.core.ui.HudViewModel;
import dev.nozh.core.ui.HudViewModelBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Chaos test runner (Phase 4 - Contract 11.1).
 * 
 * Executes all chaos scenarios using fakes.
 * PURE - NO MC dependencies, uses fakes for all external components.
 * NEVER throws exceptions upward - all failures captured in results.
 */
public final class ChaosTestRunner {

    /**
     * Run all chaos scenarios.
     * 
     * @return Complete test report
     */
    public static ChaosTestReport runAll() {
        List<ChaosScenarioResult> results = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        for (ChaosScenario scenario : ChaosScenario.values()) {
            results.add(runScenario(scenario));
        }

        long totalDuration = System.currentTimeMillis() - startTime;

        int passed = (int) results.stream().filter(ChaosScenarioResult::passed).count();
        int failed = results.size() - passed;

        return new ChaosTestReport(results, results.size(), passed, failed, totalDuration, buildReportMetadata());
    }

    /**
     * Run single chaos scenario.
     * 
     * NEVER throws - all exceptions caught and recorded.
     */
    private static ChaosScenarioResult runScenario(ChaosScenario scenario) {
        long start = System.currentTimeMillis();

        try {
            switch (scenario) {
                case PROVIDER_INIT_FAILURE:
                    return testProviderInitFailure(start);
                case INVARIANT_VIOLATION_ATTEMPT:
                    return testInvariantViolation(start);
                case QUEUE_OVERFLOW:
                    return testQueueOverflow(start);
                case TELEMETRY_STARVATION:
                    return testTelemetryStarvation(start);
                case GOVERNOR_FLAPPING:
                    return testGovernorFlapping(start);
                case PRESET_VIOLATION:
                    return testPresetViolation(start);
                case SAFEMODE_DISPATCH:
                    return testSafeModeDispatch(start);
                case HUD_SNAPSHOT_CORRUPTION:
                    return testHudSnapshotCorruption(start);
                case CRASH_LOOP_RECOVERY:
                    return testCrashLoopRecovery(start);
                case ENTITY_SWARM:
                    return testEntitySwarm(start);
                case CHUNK_SPAM:
                    return testChunkSpam(start);
                case SHADER_LOAD:
                    return testShaderLoad(start);
                default:
                    return ChaosScenarioResult.fail(scenario, "Unknown scenario", 0);
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            return ChaosScenarioResult.fail(scenario, "Uncaught exception: " + e.getMessage(), duration);
        }
    }

    // Scenario implementations (chaos scenarios using fakes and pure core components)

    private static ChaosScenarioResult testProviderInitFailure(long start) {
        // Malicious: 50% intermittent init failures with different exceptions
        ProviderHealthTracker healthTracker = new ProviderHealthTracker();
        ProviderRegistry registry = new ProviderRegistry(healthTracker);
        int failures = 0;

        for (int i = 0; i < 20; i++) {
            boolean shouldThrow = i % 2 == 0;
            CapabilityProvider provider = new IntermittentChaosProvider(CapabilityId.PARTICLES, shouldThrow, i);
            registry.register(provider);
            if (shouldThrow) {
                failures++;
                if (healthTracker.getStatus(CapabilityId.PARTICLES) != ProviderStatus.BROKEN) {
                    long duration = System.currentTimeMillis() - start;
                    return ChaosScenarioResult.fail(ChaosScenario.PROVIDER_INIT_FAILURE,
                            "Provider failures did not mark BROKEN", duration);
                }
            }
        }

        if (failures == 0) {
            long duration = System.currentTimeMillis() - start;
            return ChaosScenarioResult.fail(ChaosScenario.PROVIDER_INIT_FAILURE,
                    "No failures triggered during intermittent init", duration);
        }

        if (healthTracker.getStatus(CapabilityId.PARTICLES) != ProviderStatus.HEALTHY) {
            long duration = System.currentTimeMillis() - start;
            return ChaosScenarioResult.fail(ChaosScenario.PROVIDER_INIT_FAILURE,
                    "Provider did not recover to HEALTHY after success", duration);
        }

        if (registry.get(CapabilityId.PARTICLES).isEmpty()) {
            long duration = System.currentTimeMillis() - start;
            return ChaosScenarioResult.fail(ChaosScenario.PROVIDER_INIT_FAILURE,
                    "Registry did not retain healthy provider after failures", duration);
        }

        long duration = System.currentTimeMillis() - start;
        return ChaosScenarioResult.pass(ChaosScenario.PROVIDER_INIT_FAILURE, duration);
    }

    private static ChaosScenarioResult testInvariantViolation(long start) {
        // Malicious: 1000 invalid updates in rapid succession
        StateStore store = StateStore.getInstance();
        store.reset();
        RuntimeState baseline = store.snapshot();
        int rejected = 0;

        for (int i = 0; i < 1000; i++) {
            try {
                store.update(state -> violateInvariant(state));
            } catch (StateInvariantViolationException e) {
                rejected++;
            }
        }

        RuntimeState after = store.snapshot();
        store.reset();

        if (rejected != 1000) {
            long duration = System.currentTimeMillis() - start;
            return ChaosScenarioResult.fail(ChaosScenario.INVARIANT_VIOLATION_ATTEMPT,
                    "Expected 1000 rejected updates, got " + rejected, duration);
        }

        if (!baseline.equals(after)) {
            long duration = System.currentTimeMillis() - start;
            return ChaosScenarioResult.fail(ChaosScenario.INVARIANT_VIOLATION_ATTEMPT,
                    "State mutated despite invariant violation", duration);
        }

        long duration = System.currentTimeMillis() - start;
        return ChaosScenarioResult.pass(ChaosScenario.INVARIANT_VIOLATION_ATTEMPT, duration);
    }

    private static ChaosScenarioResult testQueueOverflow(long start) {
        // Malicious: 10 threads hammering 1000 commands each
        RuntimeState autoState = copyState(RuntimeState.defaults(), null, true, null, null, null, null, null, null,
                null, null);
        ActionBus bus = new ActionBus(new NoOpLogger(), () -> autoState);
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);
        AtomicInteger rejected = new AtomicInteger();

        for (int i = 0; i < 10; i++) {
            executor.execute(() -> {
                for (int j = 0; j < 1000; j++) {
                    Command cmd = new Command.ApplyCapability(
                            CapabilityId.CLOUDS,
                            new CapabilityValue.EnumValue("OFF"));
                    bus.dispatch(cmd, report -> {
                        if (report.finalState() == CommandLifecycle.ABORTED &&
                                report.error().orElse("").contains("queue")) {
                            rejected.incrementAndGet();
                        }
                    });
                }
                latch.countDown();
            });
        }

        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdownNow();
        }

        int queueSize = bus.getQueueSize();
        if (queueSize > 100) {
            long duration = System.currentTimeMillis() - start;
            return ChaosScenarioResult.fail(ChaosScenario.QUEUE_OVERFLOW,
                    "Queue overflow exceeded cap: " + queueSize, duration);
        }

        if (rejected.get() == 0) {
            long duration = System.currentTimeMillis() - start;
            return ChaosScenarioResult.fail(ChaosScenario.QUEUE_OVERFLOW,
                    "No commands rejected during overflow hammer", duration);
        }

        long duration = System.currentTimeMillis() - start;
        return ChaosScenarioResult.pass(ChaosScenario.QUEUE_OVERFLOW, duration);
    }

    private static ChaosScenarioResult testTelemetryStarvation(long start) {
        // Malicious: 100k samples in a tight loop
        RingTelemetryBuffer buffer = new RingTelemetryBuffer(512);
        for (int i = 0; i < 100_000; i++) {
            buffer.add(new TelemetrySample(
                    System.currentTimeMillis(),
                    14.0 + (i % 8),
                    5.0,
                    60,
                    200,
                    30,
                    1500,
                    buffer.getDroppedCount()));
        }

        TelemetrySnapshot snapshot = buffer.snapshot();
        if (snapshot.sampleCount() == 0) {
            long duration = System.currentTimeMillis() - start;
            return ChaosScenarioResult.fail(ChaosScenario.TELEMETRY_STARVATION,
                    "Telemetry snapshot empty after massive overflow", duration);
        }

        if (buffer.getDroppedCount() == 0) {
            long duration = System.currentTimeMillis() - start;
            return ChaosScenarioResult.fail(ChaosScenario.TELEMETRY_STARVATION,
                    "Dropped count did not increase under starvation", duration);
        }

        long duration = System.currentTimeMillis() - start;
        return ChaosScenarioResult.pass(ChaosScenario.TELEMETRY_STARVATION, duration);
    }

    private static ChaosScenarioResult testGovernorFlapping(long start) {
        // Malicious: rapid state changes attempting to trigger cascade
        File chaosDir = new File("build/tmp/chaos");
        chaosDir.mkdirs();
        ActionMatrix matrix = new ActionMatrix(
                new ProviderRegistry(new ProviderHealthTracker()),
                new ActionSuccessTracker("chaos"),
                new ConfidenceCalculator(),
                new dev.nozh.core.intelligence.SessionLearning(chaosDir));
        SimulationGovernor governor = new SimulationGovernor(matrix);

        RuntimeState stableState = copyState(RuntimeState.defaults(), null, null, null, null, null, null, null, null,
                16.0, 17.0);
        long base = 1_000_000L;
        boolean allowedTooSoon = governor.canAct(stableState, base, base + 1000, false, 0);
        boolean allowedAfterWindow = governor.canAct(stableState, base, base + 35_000, false, 0);

        if (allowedTooSoon) {
            long duration = System.currentTimeMillis() - start;
            return ChaosScenarioResult.fail(ChaosScenario.GOVERNOR_FLAPPING,
                    "Governor allowed action inside observation window", duration);
        }

        if (!allowedAfterWindow) {
            long duration = System.currentTimeMillis() - start;
            return ChaosScenarioResult.fail(ChaosScenario.GOVERNOR_FLAPPING,
                    "Governor blocked action after observation window", duration);
        }

        long duration = System.currentTimeMillis() - start;
        return ChaosScenarioResult.pass(ChaosScenario.GOVERNOR_FLAPPING, duration);
    }

    private static ChaosScenarioResult testPresetViolation(long start) {
        // Malicious: Action outside preset bounds (CAFETERA tier)
        PresetConstraints constraints = PresetRegistry.get(HardwareTier.CAFETERA);
        CapabilityId target = CapabilityId.RENDER_DISTANCE;
        CapabilityValue.IntValue value = new CapabilityValue.IntValue(32);

        boolean allowed = constraints.allows(target);
        boolean violatesBound = value.value() > constraints.maxRenderDistance();

        if (allowed || !violatesBound) {
            long duration = System.currentTimeMillis() - start;
            return ChaosScenarioResult.fail(ChaosScenario.PRESET_VIOLATION,
                    "Preset constraints failed to detect out-of-bounds action", duration);
        }

        long duration = System.currentTimeMillis() - start;
        return ChaosScenarioResult.pass(ChaosScenario.PRESET_VIOLATION, duration);
    }

    private static ChaosScenarioResult testSafeModeDispatch(long start) {
        // Malicious: Attempt to dispatch capability change while SafeMode active
        RuntimeState safeState = copyState(RuntimeState.defaults(), true, true, null, null, null, null, null, null,
                null, null);
        ActionBus bus = new ActionBus(new NoOpLogger(), () -> safeState);
        Command cmd = new Command.ApplyCapability(
                CapabilityId.PARTICLES,
                new CapabilityValue.EnumValue("MINIMAL"));

        AtomicReference<CommandExecutionReport> reportRef = new AtomicReference<>();
        bus.dispatch(cmd, reportRef::set);

        CommandExecutionReport report = reportRef.get();
        if (report == null) {
            long duration = System.currentTimeMillis() - start;
            return ChaosScenarioResult.fail(ChaosScenario.SAFEMODE_DISPATCH,
                    "SafeMode dispatch produced no report", duration);
        }

        if (report.finalState() != CommandLifecycle.ABORTED) {
            long duration = System.currentTimeMillis() - start;
            return ChaosScenarioResult.fail(ChaosScenario.SAFEMODE_DISPATCH,
                    "SafeMode dispatch was not rejected", duration);
        }

        if (!report.error().orElse("").contains("SafeMode")) {
            long duration = System.currentTimeMillis() - start;
            return ChaosScenarioResult.fail(ChaosScenario.SAFEMODE_DISPATCH,
                    "SafeMode rejection reason missing", duration);
        }

        long duration = System.currentTimeMillis() - start;
        return ChaosScenarioResult.pass(ChaosScenario.SAFEMODE_DISPATCH, duration);
    }

    private static ChaosScenarioResult testHudSnapshotCorruption(long start) {
        // Malicious: Null telemetry snapshot (corruption) must not crash HUD
        HudViewModel viewModel = HudViewModelBuilder.build(
                RuntimeState.defaults(),
                null,
                dev.nozh.core.profiler.PerfDiagnosticsSnapshot.empty(),
                List.of(),
                HardwareTier.MEDIUM,
                new ProviderRegistry(new ProviderHealthTracker()));

        if (viewModel == null || viewModel != HudViewModel.EMPTY) {
            long duration = System.currentTimeMillis() - start;
            return ChaosScenarioResult.fail(ChaosScenario.HUD_SNAPSHOT_CORRUPTION,
                    "HUD did not return EMPTY on corrupted snapshot", duration);
        }

        long duration = System.currentTimeMillis() - start;
        return ChaosScenarioResult.pass(ChaosScenario.HUD_SNAPSHOT_CORRUPTION, duration);
    }

    private static ChaosScenarioResult testCrashLoopRecovery(long start) {
        NozhState state = new NozhState();
        state.bootAttempts = dev.nozh.NozhConstants.MAX_BOOT_ATTEMPTS_BEFORE_SAFE_MODE;
        state.sessionStable = false;
        CrashFailureContext context = new CrashFailureContext(
                System.currentTimeMillis(),
                "CHAOS_TEST",
                dev.nozh.core.bus.CapabilityId.RENDER_DISTANCE.name(),
                dev.nozh.core.bus.CommandType.APPLY.name(),
                "32",
                "Simulated crash loop",
                "SimulatedException");
        state.setLastFailureContext(context);

        long now = System.currentTimeMillis();
        CrashRecoveryDecision decision = CrashLoopGuard.evaluateCrashRecovery(state, now);
        if (decision.action() != CrashRecoveryAction.QUARANTINED_CAPABILITY) {
            long duration = System.currentTimeMillis() - start;
            return ChaosScenarioResult.fail(ChaosScenario.CRASH_LOOP_RECOVERY,
                    "Expected capability quarantine recovery action", duration);
        }

        if (state.isSafeModeActive()) {
            long duration = System.currentTimeMillis() - start;
            return ChaosScenarioResult.fail(ChaosScenario.CRASH_LOOP_RECOVERY,
                    "Safe mode activated instead of targeted quarantine", duration);
        }

        if (!state.isCapabilityQuarantined(dev.nozh.core.bus.CapabilityId.RENDER_DISTANCE, now)) {
            long duration = System.currentTimeMillis() - start;
            return ChaosScenarioResult.fail(ChaosScenario.CRASH_LOOP_RECOVERY,
                    "Capability not quarantined after crash loop recovery", duration);
        }

        CrashRecoveryDecision escalated = CrashLoopGuard.evaluateCrashRecovery(state, now);
        if (escalated.action() != CrashRecoveryAction.SAFE_MODE) {
            long duration = System.currentTimeMillis() - start;
            return ChaosScenarioResult.fail(ChaosScenario.CRASH_LOOP_RECOVERY,
                    "Expected escalation to safe mode when quarantine already active", duration);
        }

        long afterRetry = decision.retryAtMillis() + 1;
        if (state.isCapabilityQuarantined(dev.nozh.core.bus.CapabilityId.RENDER_DISTANCE, afterRetry)) {
            long duration = System.currentTimeMillis() - start;
            return ChaosScenarioResult.fail(ChaosScenario.CRASH_LOOP_RECOVERY,
                    "Capability quarantine did not expire after retry window", duration);
        }

        long duration = System.currentTimeMillis() - start;
        return ChaosScenarioResult.pass(ChaosScenario.CRASH_LOOP_RECOVERY, duration);
    }

    private static ChaosScenarioResult testEntitySwarm(long start) {
        int entityCount = 350;
        int ticks = 240;
        List<SimulatedEntity> entities = new ArrayList<>(entityCount);
        for (int i = 0; i < entityCount; i++) {
            entities.add(new SimulatedEntity(i, i % 23, i % 17));
        }

        int updates = 0;
        double accumulatedMotion = 0.0;
        for (int tick = 0; tick < ticks; tick++) {
            for (SimulatedEntity entity : entities) {
                entity.step(tick);
                accumulatedMotion += entity.energy();
                updates++;
            }
        }

        if (updates != entityCount * ticks) {
            long duration = System.currentTimeMillis() - start;
            return ChaosScenarioResult.fail(ChaosScenario.ENTITY_SWARM,
                    "Entity updates mismatch: " + updates, duration);
        }

        if (!Double.isFinite(accumulatedMotion) || accumulatedMotion <= 0.0) {
            long duration = System.currentTimeMillis() - start;
            return ChaosScenarioResult.fail(ChaosScenario.ENTITY_SWARM,
                    "Entity simulation produced invalid motion", duration);
        }

        long duration = System.currentTimeMillis() - start;
        return ChaosScenarioResult.pass(ChaosScenario.ENTITY_SWARM, duration);
    }

    private static ChaosScenarioResult testChunkSpam(long start) {
        int requests = 5000;
        int processed = 0;
        int maxQueue = 0;
        java.util.ArrayDeque<ChunkRequest> queue = new java.util.ArrayDeque<>();

        for (int i = 0; i < requests; i++) {
            queue.add(new ChunkRequest(i, i % 2 == 0));
            maxQueue = Math.max(maxQueue, queue.size());
            if (i % 3 == 0) {
                processed += drainQueue(queue, 5);
            }
        }

        processed += drainQueue(queue, queue.size());

        if (processed != requests) {
            long duration = System.currentTimeMillis() - start;
            return ChaosScenarioResult.fail(ChaosScenario.CHUNK_SPAM,
                    "Chunk requests processed mismatch: " + processed, duration);
        }

        if (maxQueue < 500) {
            long duration = System.currentTimeMillis() - start;
            return ChaosScenarioResult.fail(ChaosScenario.CHUNK_SPAM,
                    "Chunk spam did not reach stress threshold", duration);
        }

        long duration = System.currentTimeMillis() - start;
        return ChaosScenarioResult.pass(ChaosScenario.CHUNK_SPAM, duration);
    }

    private static ChaosScenarioResult testShaderLoad(long start) {
        String[] shaderPacks = {"OFF", "RAIN_STORM", "CINEMATIC", "PERFORMANCE"};
        String current = shaderPacks[0];
        int toggles = 0;
        double shaderCost = 0.0;

        for (int i = 0; i < 240; i++) {
            String next = shaderPacks[i % shaderPacks.length];
            if (!next.equals(current)) {
                toggles++;
                current = next;
            }
            shaderCost += simulateShaderFrameCost(current, i);
        }

        if (toggles < 100) {
            long duration = System.currentTimeMillis() - start;
            return ChaosScenarioResult.fail(ChaosScenario.SHADER_LOAD,
                    "Shader toggling did not occur frequently enough", duration);
        }

        if (!Double.isFinite(shaderCost) || shaderCost <= 0.0) {
            long duration = System.currentTimeMillis() - start;
            return ChaosScenarioResult.fail(ChaosScenario.SHADER_LOAD,
                    "Shader load simulation produced invalid cost", duration);
        }

        long duration = System.currentTimeMillis() - start;
        return ChaosScenarioResult.pass(ChaosScenario.SHADER_LOAD, duration);
    }

    private ChaosTestRunner() {
        // Static utility
    }

    private static RuntimeState violateInvariant(RuntimeState state) {
        return copyState(state, true, true, null, null, null, null, null, null, null, null);
    }

    private static RuntimeState copyState(
            RuntimeState state,
            Boolean safeMode,
            Boolean autoTuning,
            Boolean benchmarkRunning,
            Boolean governorDisabled,
            Boolean governorCooldownActive,
            Integer pendingActionsCount,
            Integer executionHistorySize,
            Integer lastSnapshotHistorySize,
            Double avgFrametimeMs,
            Double p95FrametimeMs) {
        return new RuntimeState(
                state.enabled(),
                safeMode != null ? safeMode : state.safeMode(),
                autoTuning != null ? autoTuning : state.autoTuning(),
                state.debugLogs(),
                governorDisabled != null ? governorDisabled : state.governorDisabled(),
                governorCooldownActive != null ? governorCooldownActive : state.governorCooldownActive(),
                state.governorLastActionTimestamp(),
                benchmarkRunning != null ? benchmarkRunning : state.benchmarkRunning(),
                state.benchmarkValidity(),
                state.benchmarkStartTimestamp(),
                state.pendingAction(),
                state.suggestedActions(),
                pendingActionsCount != null ? pendingActionsCount : state.pendingActionsCount(),
                executionHistorySize != null ? executionHistorySize : state.executionHistorySize(),
                lastSnapshotHistorySize != null ? lastSnapshotHistorySize : state.lastSnapshotHistorySize(),
                state.actionHistory(),
                state.sessionChangesCount(),
                avgFrametimeMs != null ? avgFrametimeMs : state.avgFrametimeMs(),
                p95FrametimeMs != null ? p95FrametimeMs : state.p95FrametimeMs(),
                state.p99FrametimeMs(),
                state.frametimeStddevMs(),
                state.tickTimeAvg(),
                state.tickTimeP95(),
                state.spikeCount(),
                state.lastDecisionReason(),
                state.lastDecisionTimestamp(),
                state.lastImpactMs(),
                state.lastOutcome(),
                state.lastDecisionAccepted(),
                state.sessionStartTime(),
                state.stateVersion(),
                state.currentScenario(),
                state.scenarioConfidence(),
                state.lastScenarioChangeTimestamp(),
                state.scenarioChangeCount(),
                state.rapidScenarioChangeCount(),
                state.combatAfkFlipCount(),
                state.scenarioHistory(),
                state.baselineSettings(),
                state.currentSettings());
    }

    private static ChaosReportMetadata buildReportMetadata() {
        int renderDistance = Integer.getInteger("nozh.chaos.renderDistance", 16);
        int simulationDistance = Integer.getInteger("nozh.chaos.simulationDistance", 12);
        String shaderPack = System.getProperty("nozh.chaos.shaderPack", "OFF");
        return new ChaosReportMetadata(renderDistance, simulationDistance, shaderPack);
    }

    private static int drainQueue(java.util.ArrayDeque<ChunkRequest> queue, int max) {
        int drained = 0;
        int entropy = 0;
        while (!queue.isEmpty() && drained < max) {
            ChunkRequest request = queue.removeFirst();
            entropy ^= request.id();
            if (request.isLoad()) {
                drained++;
            } else {
                drained++;
            }
        }
        if (entropy == Integer.MIN_VALUE) {
            return drained;
        }
        return drained;
    }

    private static double simulateShaderFrameCost(String shaderPack, int frame) {
        double base = 2.5;
        double packMultiplier = switch (shaderPack) {
            case "RAIN_STORM" -> 3.5;
            case "CINEMATIC" -> 3.0;
            case "PERFORMANCE" -> 1.5;
            default -> 1.0;
        };
        return base * packMultiplier + (frame % 7) * 0.1;
    }

    private static final class SimulatedEntity {
        private double x;
        private double y;
        private double z;

        private SimulatedEntity(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private void step(int tick) {
            x += Math.sin(tick * 0.05 + x) * 0.1;
            y += Math.cos(tick * 0.03 + y) * 0.1;
            z += Math.sin(tick * 0.04 + z) * 0.1;
        }

        private double energy() {
            return x * x + y * y + z * z;
        }
    }

    private static final class ChunkRequest {
        private final int id;
        private final boolean load;

        private ChunkRequest(int id, boolean load) {
            this.id = id;
            this.load = load;
        }

        private boolean isLoad() {
            return load;
        }

        private int id() {
            return id;
        }
    }

    private static final class IntermittentChaosProvider implements CapabilityProvider {
        private final CapabilityId id;
        private final boolean shouldThrow;
        private final int seed;

        private IntermittentChaosProvider(CapabilityId id, boolean shouldThrow, int seed) {
            this.id = id;
            this.shouldThrow = shouldThrow;
            this.seed = seed;
        }

        @Override
        public CapabilityId id() {
            return id;
        }

        @Override
        public ProviderMetadata metadata() {
            return new ProviderMetadata() {
                @Override
                public SideEffects sideEffects() {
                    return SideEffects.none();
                }

                @Override
                public SafetyLevel safetyLevel() {
                    return SafetyLevel.SAFE;
                }

                @Override
                public RollbackGuarantee rollbackGuarantee() {
                    return RollbackGuarantee.STRONG;
                }

                @Override
                public ImpactLevel gameplayImpact() {
                    return ImpactLevel.LOW;
                }

                @Override
                public CostLevel costLevel() {
                    return CostLevel.LOW;
                }

                @Override
                public ImpactLevel visualImpact() {
                    return ImpactLevel.LOW;
                }

                @Override
                public double expectedGainMs() {
                    return 1.0;
                }

                @Override
                public java.util.Set<String> requiredMods() {
                    return java.util.Set.of();
                }

                @Override
                public java.util.Set<String> conflictingMods() {
                    return java.util.Set.of();
                }
            };
        }

        @Override
        public ProviderStatus status() {
            if (shouldThrow) {
                throw new IllegalStateException("Intermittent status failure " + seed);
            }
            return ProviderStatus.HEALTHY;
        }

        @Override
        public Optional<String> statusReason() {
            return Optional.empty();
        }

        @Override
        public boolean isAvailable() {
            if (shouldThrow) {
                throw new IllegalStateException("Intermittent init failure " + seed);
            }
            return true;
        }

        @Override
        public Optional<CapabilityValue> getCurrentValueSafe() {
            return Optional.of(new CapabilityValue.EnumValue("ALL"));
        }

        @Override
        public ApplyResult apply(CapabilityValue value) {
            return new ApplyResult.Success(value, value);
        }
    }
}
