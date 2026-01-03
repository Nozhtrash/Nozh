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
                RuntimeState base = RuntimeState.defaults();

                RuntimeState state = new RuntimeState(
                                base.enabled(),
                                true,
                                true,
                                base.debugLogs(),
                                base.governorDisabled(),
                                base.governorCooldownActive(),
                                base.governorLastActionTimestamp(),
                                base.benchmarkRunning(),
                                base.benchmarkStartTimestamp(),

                                Optional.<PendingAction>empty(),

                                base.executionHistorySize(),
                                base.lastSnapshotHistorySize(),
                                base.sessionChangesCount(),
                                base.spikeCount(),

                                base.avgFrametimeMs(),
                                base.p95FrametimeMs(),
                                base.spikeCount(),
                                base.sessionStartTime(),
                                base.stateVersion(),
                                base.currentScenario());

                ValidationResult result = StateInvariantValidator.validate(state);
                assertTrue(result.isInvalid());
                assertTrue(((ValidationResult.Invalid) result)
                                .violations().stream().anyMatch(v -> v.contains("Invariant 1")));
        }

        @Test
        void testInvariant1_SafeModeWithoutAutoTuning_IsValid() {
                RuntimeState base = RuntimeState.defaults();

                RuntimeState state = new RuntimeState(
                                base.enabled(),
                                true,
                                false,
                                base.debugLogs(),
                                base.governorDisabled(),
                                base.governorCooldownActive(),
                                base.governorLastActionTimestamp(),
                                base.benchmarkRunning(),
                                base.benchmarkStartTimestamp(),

                                Optional.<PendingAction>empty(),

                                base.executionHistorySize(),
                                base.lastSnapshotHistorySize(),
                                base.sessionChangesCount(),
                                base.spikeCount(),

                                base.avgFrametimeMs(),
                                base.p95FrametimeMs(),
                                base.spikeCount(),
                                base.sessionStartTime(),
                                base.stateVersion(),
                                base.currentScenario());

                assertTrue(StateInvariantValidator.validate(state).isValid());
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

                                Optional.<PendingAction>empty(),

                                base.executionHistorySize(),
                                base.lastSnapshotHistorySize(),
                                base.sessionChangesCount(),
                                base.spikeCount(),

                                base.avgFrametimeMs(),
                                base.p95FrametimeMs(),
                                base.spikeCount(),
                                base.sessionStartTime(),
                                base.stateVersion(),
                                base.currentScenario());

                assertTrue(StateInvariantValidator.validate(state).isInvalid());
        }

        @Test
        void testInvariant2_BenchmarkWithGovernorDisabled_IsValid() {
                RuntimeState base = RuntimeState.defaults();

                RuntimeState state = new RuntimeState(
                                base.enabled(),
                                base.safeMode(),
                                base.autoTuning(),
                                base.debugLogs(),
                                true,
                                base.governorCooldownActive(),
                                base.governorLastActionTimestamp(),
                                true,
                                System.currentTimeMillis(),

                                Optional.<PendingAction>empty(),

                                base.executionHistorySize(),
                                base.lastSnapshotHistorySize(),
                                base.sessionChangesCount(),
                                base.spikeCount(),

                                base.avgFrametimeMs(),
                                base.p95FrametimeMs(),
                                base.spikeCount(),
                                base.sessionStartTime(),
                                base.stateVersion(),
                                base.currentScenario());

                assertTrue(StateInvariantValidator.validate(state).isValid());
        }

        // === Invariant 3: PendingAction → Cooldown Active ===

        @Test
        void testInvariant3_PendingWithoutCooldown_IsInvalid() {
                RuntimeState base = RuntimeState.defaults();

                RuntimeState state = new RuntimeState(
                                base.enabled(),
                                base.safeMode(),
                                base.autoTuning(),
                                base.debugLogs(),
                                base.governorDisabled(),
                                false,
                                base.governorLastActionTimestamp(),
                                base.benchmarkRunning(),
                                base.benchmarkStartTimestamp(),

                                Optional.of(samplePending()),

                                base.executionHistorySize(),
                                base.lastSnapshotHistorySize(),
                                base.sessionChangesCount(),
                                base.spikeCount(),

                                base.avgFrametimeMs(),
                                base.p95FrametimeMs(),
                                base.spikeCount(),
                                base.sessionStartTime(),
                                base.stateVersion(),
                                base.currentScenario());

                assertTrue(StateInvariantValidator.validate(state).isInvalid());
        }

        @Test
        void testInvariant3_PendingWithCooldown_IsValid() {
                RuntimeState base = RuntimeState.defaults();

                RuntimeState state = new RuntimeState(
                                base.enabled(),
                                base.safeMode(),
                                base.autoTuning(),
                                base.debugLogs(),
                                base.governorDisabled(),
                                true,
                                base.governorLastActionTimestamp(),
                                base.benchmarkRunning(),
                                base.benchmarkStartTimestamp(),

                                Optional.of(samplePending()),

                                base.executionHistorySize(),
                                base.lastSnapshotHistorySize(),
                                base.sessionChangesCount(),
                                base.spikeCount(),

                                base.avgFrametimeMs(),
                                base.p95FrametimeMs(),
                                base.spikeCount(),
                                base.sessionStartTime(),
                                base.stateVersion(),
                                base.currentScenario());

                assertTrue(StateInvariantValidator.validate(state).isValid());
        }

        // === Invariant 4: History monotonic ===

        @Test
        void testInvariant4_HistoryDecreased_IsInvalid() {
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

                                5,
                                10,
                                base.sessionChangesCount(),
                                base.spikeCount(),

                                base.avgFrametimeMs(),
                                base.p95FrametimeMs(),
                                base.spikeCount(),
                                base.sessionStartTime(),
                                base.stateVersion(),
                                base.currentScenario());

                assertTrue(StateInvariantValidator.validate(state).isInvalid());
        }

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

                                15,
                                10,
                                base.sessionChangesCount(),
                                base.spikeCount(),

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
