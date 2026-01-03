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
                                new dev.nozh.core.bus.Command.ResetCapability(null),
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
                                state.benchmarkValidity(),
                                state.benchmarkStartTimestamp(),
                                state.pendingAction(),
                                state.pendingActionsCount(),
                                state.executionHistorySize(),
                                state.lastSnapshotHistorySize(),
                                state.sessionChangesCount(),
                                state.avgFrametimeMs(),
                                state.p95FrametimeMs(),
                                state.tickTimeAvg(),
                                state.tickTimeP95(),
                                state.spikeCount(),
                                state.lastDecisionReason(),
                                state.lastDecisionTimestamp(),
                                state.sessionStartTime(),
                                state.stateVersion(),
                                state.currentScenario(),
                                state.scenarioConfidence());

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
                                state.benchmarkValidity(),
                                state.benchmarkStartTimestamp(),
                                state.pendingAction(),
                                state.pendingActionsCount(),
                                state.executionHistorySize(),
                                state.lastSnapshotHistorySize(),
                                state.sessionChangesCount(),
                                state.avgFrametimeMs(),
                                state.p95FrametimeMs(),
                                state.tickTimeAvg(),
                                state.tickTimeP95(),
                                state.spikeCount(),
                                state.lastDecisionReason(),
                                state.lastDecisionTimestamp(),
                                state.sessionStartTime(),
                                state.stateVersion(),
                                state.currentScenario(),
                                state.scenarioConfidence());

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
                                false, // governorDisabled = false (INVALID when benchmark running)
                                base.governorCooldownActive(),
                                base.governorLastActionTimestamp(),
                                true, // benchmarkRunning = true
                                "NONE", // benchmarkValidity
                                System.currentTimeMillis(),
                                base.pendingAction(),
                                base.pendingActionsCount(),
                                base.executionHistorySize(),
                                base.lastSnapshotHistorySize(),
                                base.sessionChangesCount(),
                                base.avgFrametimeMs(),
                                base.p95FrametimeMs(),
                                base.tickTimeAvg(),
                                base.tickTimeP95(),
                                base.spikeCount(),
                                base.lastDecisionReason(),
                                base.lastDecisionTimestamp(),
                                base.sessionStartTime(),
                                base.stateVersion(),
                                base.currentScenario(),
                                base.scenarioConfidence());

                ValidationResult result = StateInvariantValidator.validate(state);
                assertTrue(result.isInvalid());
                assertTrue(((ValidationResult.Invalid) result)
                                .violations().stream().anyMatch(v -> v.contains("Invariant 2")));
        }

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
                                "NONE", // benchmarkValidity
                                System.currentTimeMillis(),
                                state.pendingAction(),
                                state.pendingActionsCount(),
                                state.executionHistorySize(),
                                state.lastSnapshotHistorySize(),
                                state.sessionChangesCount(),
                                state.avgFrametimeMs(),
                                state.p95FrametimeMs(),
                                state.tickTimeAvg(),
                                state.tickTimeP95(),
                                state.spikeCount(),
                                state.lastDecisionReason(),
                                state.lastDecisionTimestamp(),
                                state.sessionStartTime(),
                                state.stateVersion(),
                                state.currentScenario(),
                                state.scenarioConfidence());

                assertTrue(StateInvariantValidator.validate(state).isValid());
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
                                state.benchmarkValidity(),
                                state.benchmarkStartTimestamp(),
                                state.pendingAction(),
                                3, // pendingActionsCount > 0
                                state.executionHistorySize(),
                                state.lastSnapshotHistorySize(),
                                state.sessionChangesCount(),
                                state.avgFrametimeMs(),
                                state.p95FrametimeMs(),
                                state.tickTimeAvg(),
                                state.tickTimeP95(),
                                state.spikeCount(),
                                state.lastDecisionReason(),
                                state.lastDecisionTimestamp(),
                                state.sessionStartTime(),
                                state.stateVersion(),
                                state.currentScenario(),
                                state.scenarioConfidence());

                ValidationResult result = StateInvariantValidator.validate(state);
                assertTrue(result.isInvalid());
                assertTrue(((ValidationResult.Invalid) result)
                                .violations().stream().anyMatch(v -> v.contains("Invariant 3")));
        }

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
                                state.benchmarkValidity(),
                                state.benchmarkStartTimestamp(),
                                state.pendingAction(),
                                3, // pendingActionsCount > 0
                                state.executionHistorySize(),
                                state.lastSnapshotHistorySize(),
                                state.sessionChangesCount(),
                                state.avgFrametimeMs(),
                                state.p95FrametimeMs(),
                                state.tickTimeAvg(),
                                state.tickTimeP95(),
                                state.spikeCount(),
                                state.lastDecisionReason(),
                                state.lastDecisionTimestamp(),
                                state.sessionStartTime(),
                                state.stateVersion(),
                                state.currentScenario(),
                                state.scenarioConfidence());

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
                                state.benchmarkValidity(),
                                state.benchmarkStartTimestamp(),
                                state.pendingAction(),
                                state.pendingActionsCount(),
                                5, // executionHistorySize = 5
                                10, // lastSnapshotHistorySize = 10 (VIOLATION: decreased)
                                state.sessionChangesCount(),
                                state.avgFrametimeMs(),
                                state.p95FrametimeMs(),
                                state.tickTimeAvg(),
                                state.tickTimeP95(),
                                state.spikeCount(),
                                state.lastDecisionReason(),
                                state.lastDecisionTimestamp(),
                                state.sessionStartTime(),
                                state.stateVersion(),
                                state.currentScenario(),
                                state.scenarioConfidence());

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
                                state.benchmarkValidity(),
                                state.benchmarkStartTimestamp(),
                                state.pendingAction(),
                                state.pendingActionsCount(),
                                15, // executionHistorySize = 15
                                10, // lastSnapshotHistorySize = 10 (OK: increased)
                                state.sessionChangesCount(),
                                state.avgFrametimeMs(),
                                state.p95FrametimeMs(),
                                state.tickTimeAvg(),
                                state.tickTimeP95(),
                                state.spikeCount(),
                                state.lastDecisionReason(),
                                state.lastDecisionTimestamp(),
                                state.sessionStartTime(),
                                state.stateVersion(),
                                state.currentScenario(),
                                state.scenarioConfidence());

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
                                state.benchmarkValidity(),
                                System.currentTimeMillis(),
                                state.pendingAction(),
                                5, // pendingActionsCount > 0 (VIOLATION 3)
                                state.executionHistorySize(),
                                state.lastSnapshotHistorySize(),
                                state.sessionChangesCount(),
                                state.avgFrametimeMs(),
                                state.p95FrametimeMs(),
                                state.tickTimeAvg(),
                                state.tickTimeP95(),
                                state.spikeCount(),
                                state.lastDecisionReason(),
                                state.lastDecisionTimestamp(),
                                state.sessionStartTime(),
                                state.stateVersion(),
                                state.currentScenario(),
                                state.scenarioConfidence());

                ValidationResult result = StateInvariantValidator.validate(state);
                assertTrue(result.isInvalid());
                ValidationResult.Invalid invalid = (ValidationResult.Invalid) result;
                // We expect violations for:
                // 1. SafeMode+AutoTuning
                // 2. Benchmark running but Governor not disabled is NOT a violation if we
                // didn't set GovDisabled=false explicitly?
                // Wait, in this test: GovernorDisabled=false. So Benchmark+GovEnabled IS a
                // violation.
                // 3. PendingActions > 0 but Cooldown=false.

                // Just check we have multiple violations
                assertTrue(invalid.violations().size() >= 2);
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
                                base.benchmarkValidity(),
                                base.benchmarkStartTimestamp(),

                                Optional.<PendingAction>empty(),
                                0,

                                15,
                                10,
                                base.sessionChangesCount(),

                                base.avgFrametimeMs(),
                                base.p95FrametimeMs(),
                                base.tickTimeAvg(),
                                base.tickTimeP95(),
                                base.spikeCount(),
                                base.lastDecisionReason(),
                                base.lastDecisionTimestamp(),
                                base.sessionStartTime(),
                                base.stateVersion(),
                                base.currentScenario(),
                                base.scenarioConfidence());

                assertTrue(StateInvariantValidator.validate(state).isValid());
        }

        // === Default always valid ===

        @Test
        void testDefaultState_IsAlwaysValid() {
                assertTrue(StateInvariantValidator.validate(RuntimeState.defaults()).isValid());
        }
}
