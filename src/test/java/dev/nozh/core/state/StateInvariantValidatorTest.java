package dev.nozh.core.state;

import dev.nozh.core.context.Scenario;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StateInvariantValidator (Contract 1).
 *
 * Tests all mandatory invariants.
 */
class StateInvariantValidatorTest {

        private PendingAction samplePending() {
                return new PendingAction(
                                System.currentTimeMillis(),
                                null,
                                Optional.empty(),
                                null,
                                16.0,
                                18.0);
        }

        // === Invariant 1: SafeMode → NoAutoTuning ===

        @Test
        void testInvariant1_SafeModeWithAutoTuning_IsInvalid() {
                RuntimeState state = RuntimeState.defaults();
                state = new RuntimeState(
                                state.enabled(),
                                true, // safeMode = true
                                true, // autoTuning = true (VIOLATION)
                                state.debugLogs(),
                                state.governorDisabled(),
                                state.governorCooldownActive(),
                                state.governorLastActionTimestamp(),
                                state.benchmarkRunning(),
                                state.benchmarkStartTimestamp(),
                                state.pendingActionsCount(),
                                state.executionHistorySize(),
                                state.lastSnapshotHistorySize(),
                                state.sessionChangesCount(),
                                state.avgFrametimeMs(),
                                state.p95FrametimeMs(),
                                state.tickTimeAvg(),
                                state.tickTimeP95(),
                                state.spikeCount(),
                                state.sessionStartTime(),
                                state.stateVersion(),
                                state.currentScenario());

                ValidationResult result = StateInvariantValidator.validate(state);
                assertTrue(result.isInvalid());
                assertTrue(((ValidationResult.Invalid) result)
                                .violations().stream().anyMatch(v -> v.contains("Invariant 1")));
        }

        @Test
        void testInvariant1_SafeModeWithoutAutoTuning_IsValid() {
                RuntimeState state = RuntimeState.defaults();
                state = new RuntimeState(
                                state.enabled(),
                                true, // safeMode = true
                                false, // autoTuning = false (OK)
                                state.debugLogs(),
                                state.governorDisabled(),
                                state.governorCooldownActive(),
                                state.governorLastActionTimestamp(),
                                state.benchmarkRunning(),
                                state.benchmarkStartTimestamp(),
                                state.pendingActionsCount(),
                                state.executionHistorySize(),
                                state.lastSnapshotHistorySize(),
                                state.sessionChangesCount(),
                                state.avgFrametimeMs(),
                                state.p95FrametimeMs(),
                                state.tickTimeAvg(),
                                state.tickTimeP95(),
                                state.spikeCount(),
                                state.sessionStartTime(),
                                state.stateVersion(),
                                state.currentScenario());

                ValidationResult result = StateInvariantValidator.validate(state);
                assertTrue(result.isValid(), "SafeMode without AutoTuning should be valid");
        }

        // === Invariant 2: Benchmark → Governor Disabled ===

        @Test
        void testInvariant2_BenchmarkWithoutGovernorDisabled_IsInvalid() {
                RuntimeState base = RuntimeState.defaults();

                RuntimeState state = new RuntimeState(
                                base.enabled(),
                                base.safeMode(),
                                base.autoTuning(),
                                base.debugLogs(),
                                false,
                                base.governorCooldownActive(),
                                base.governorLastActionTimestamp(),
                                true,
                                System.currentTimeMillis(),
                                state.pendingActionsCount(),
                                state.executionHistorySize(),
                                state.lastSnapshotHistorySize(),
                                state.sessionChangesCount(),
                                state.avgFrametimeMs(),
                                state.p95FrametimeMs(),
                                state.tickTimeAvg(),
                                state.tickTimeP95(),
                                state.spikeCount(),
                                state.sessionStartTime(),
                                state.stateVersion(),
                                state.currentScenario());

                                Optional.<PendingAction>empty(),
                                0,

                                base.executionHistorySize(),
                                base.lastSnapshotHistorySize(),
                                base.sessionChangesCount(),

        @Test
        void testInvariant2_BenchmarkWithGovernorDisabled_IsValid() {
                RuntimeState state = RuntimeState.defaults();
                state = new RuntimeState(
                                state.enabled(),
                                state.safeMode(),
                                state.autoTuning(),
                                state.debugLogs(),
                                true, // governorDisabled = true (OK)
                                state.governorCooldownActive(),
                                state.governorLastActionTimestamp(),
                                true, // benchmarkRunning = true
                                System.currentTimeMillis(),
                                state.pendingActionsCount(),
                                state.executionHistorySize(),
                                state.lastSnapshotHistorySize(),
                                state.sessionChangesCount(),
                                state.avgFrametimeMs(),
                                state.p95FrametimeMs(),
                                state.tickTimeAvg(),
                                state.tickTimeP95(),
                                state.spikeCount(),
                                state.sessionStartTime(),
                                state.stateVersion(),
                                state.currentScenario());

                assertTrue(StateInvariantValidator.validate(state).isInvalid());
        }

        @Test
        void testInvariant3_PendingActionsWithoutCooldown_IsInvalid() {
                RuntimeState state = RuntimeState.defaults();
                state = new RuntimeState(
                                state.enabled(),
                                state.safeMode(),
                                state.autoTuning(),
                                state.debugLogs(),
                                state.governorDisabled(),
                                false, // governorCooldownActive = false (VIOLATION)
                                state.governorLastActionTimestamp(),
                                state.benchmarkRunning(),
                                state.benchmarkStartTimestamp(),
                                3, // pendingActionsCount > 0
                                state.executionHistorySize(),
                                state.lastSnapshotHistorySize(),
                                state.sessionChangesCount(),
                                state.avgFrametimeMs(),
                                state.p95FrametimeMs(),
                                state.tickTimeAvg(),
                                state.tickTimeP95(),
                                state.spikeCount(),
                                state.sessionStartTime(),
                                state.stateVersion(),
                                state.currentScenario());

                                Optional.<PendingAction>empty(),
                                0,

                                base.executionHistorySize(),
                                base.lastSnapshotHistorySize(),
                                base.sessionChangesCount(),

        @Test
        void testInvariant3_PendingActionsWithCooldown_IsValid() {
                RuntimeState state = RuntimeState.defaults();
                state = new RuntimeState(
                                state.enabled(),
                                state.safeMode(),
                                state.autoTuning(),
                                state.debugLogs(),
                                state.governorDisabled(),
                                true, // governorCooldownActive = true (OK)
                                state.governorLastActionTimestamp(),
                                state.benchmarkRunning(),
                                state.benchmarkStartTimestamp(),
                                3, // pendingActionsCount > 0
                                state.executionHistorySize(),
                                state.lastSnapshotHistorySize(),
                                state.sessionChangesCount(),
                                state.avgFrametimeMs(),
                                state.p95FrametimeMs(),
                                state.tickTimeAvg(),
                                state.tickTimeP95(),
                                state.spikeCount(),
                                state.sessionStartTime(),
                                state.stateVersion(),
                                state.currentScenario());

                assertTrue(StateInvariantValidator.validate(state).isValid());
        }

        // === Invariant 3: PendingAction → Cooldown Active ===

        @Test
        void testInvariant4_HistorySizeDecreased_IsInvalid() {
                RuntimeState state = RuntimeState.defaults();
                state = new RuntimeState(
                                state.enabled(),
                                state.safeMode(),
                                state.autoTuning(),
                                state.debugLogs(),
                                state.governorDisabled(),
                                state.governorCooldownActive(),
                                state.governorLastActionTimestamp(),
                                state.benchmarkRunning(),
                                state.benchmarkStartTimestamp(),
                                state.pendingActionsCount(),
                                5, // executionHistorySize = 5
                                10, // lastSnapshotHistorySize = 10 (VIOLATION: decreased)
                                state.sessionChangesCount(),
                                state.avgFrametimeMs(),
                                state.p95FrametimeMs(),
                                state.tickTimeAvg(),
                                state.tickTimeP95(),
                                state.spikeCount(),
                                state.sessionStartTime(),
                                state.stateVersion(),
                                state.currentScenario());

                ValidationResult result = StateInvariantValidator.validate(state);

                assertTrue(result.isInvalid(), "History size decrease should be invalid");
                ValidationResult.Invalid invalid = (ValidationResult.Invalid) result;
                assertTrue(invalid.violations().stream().anyMatch(v -> v.contains("Invariant 4")),
                                "Should report Invariant 4 violation");
        }

        @Test
        void testInvariant4_HistorySizeIncreased_IsValid() {
                RuntimeState state = RuntimeState.defaults();
                state = new RuntimeState(
                                state.enabled(),
                                state.safeMode(),
                                state.autoTuning(),
                                state.debugLogs(),
                                state.governorDisabled(),
                                state.governorCooldownActive(),
                                state.governorLastActionTimestamp(),
                                state.benchmarkRunning(),
                                state.benchmarkStartTimestamp(),
                                state.pendingActionsCount(),
                                15, // executionHistorySize = 15
                                10, // lastSnapshotHistorySize = 10 (OK: increased)
                                state.sessionChangesCount(),
                                state.avgFrametimeMs(),
                                state.p95FrametimeMs(),
                                state.tickTimeAvg(),
                                state.tickTimeP95(),
                                state.spikeCount(),
                                state.sessionStartTime(),
                                state.stateVersion(),
                                state.currentScenario());

                ValidationResult result = StateInvariantValidator.validate(state);
                assertTrue(result.isValid(), "History size increase should be valid");
        }

        // === Invariant 4: History monotonic ===

        @Test
        void testMultipleViolations_ReportsAll() {
                RuntimeState state = RuntimeState.defaults();
                state = new RuntimeState(
                                state.enabled(),
                                true, // safeMode = true
                                true, // autoTuning = true (VIOLATION 1)
                                state.debugLogs(),
                                false, // governorDisabled = false
                                false, // governorCooldownActive = false
                                state.governorLastActionTimestamp(),
                                true, // benchmarkRunning = true (VIOLATION 2)
                                System.currentTimeMillis(),
                                5, // pendingActionsCount > 0 (VIOLATION 3)
                                state.executionHistorySize(),
                                state.lastSnapshotHistorySize(),
                                state.sessionChangesCount(),
                                state.avgFrametimeMs(),
                                state.p95FrametimeMs(),
                                state.tickTimeAvg(),
                                state.tickTimeP95(),
                                state.spikeCount(),
                                state.sessionStartTime(),
                                state.stateVersion(),
                                state.currentScenario());

                ValidationResult result = StateInvariantValidator.validate(state);

        @Test
        void testInvariant4_HistoryIncreased_IsValid() {
                RuntimeState base = RuntimeState.defaults();

                RuntimeState state = new RuntimeState(
                                base.enabled(),
                                base.safeMode(),
                                base.autoTuning(),
                                base.debugLogs(),
                                base.governorDisabled(),
                                base.governorCooldownActive(),
                                base.governorLastActionTimestamp(),
                                base.benchmarkRunning(),
                                base.benchmarkStartTimestamp(),

                                Optional.<PendingAction>empty(),
                                0,

                                15,
                                10,
                                base.sessionChangesCount(),

                                base.avgFrametimeMs(),
                                base.p95FrametimeMs(),
                                base.spikeCount(),
                                base.sessionStartTime(),
                                base.stateVersion(),
                                base.currentScenario());

                assertTrue(StateInvariantValidator.validate(state).isValid());
        }

        // === Default always valid ===

        @Test
        void testDefaultState_IsAlwaysValid() {
                assertTrue(StateInvariantValidator.validate(RuntimeState.defaults()).isValid());
        }
}
