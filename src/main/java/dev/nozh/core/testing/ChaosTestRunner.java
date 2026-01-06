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

        return new ChaosTestReport(results, results.size(), passed, failed, totalDuration);
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
