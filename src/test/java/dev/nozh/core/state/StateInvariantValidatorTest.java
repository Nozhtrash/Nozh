package dev.nozh.core.state;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StateInvariantValidator (Contract 1).
 * 
 * Tests all 4 mandatory invariants:
 * 1. safeMode == true → autoTuning == false
 * 2. benchmarkRunning == true → governorDisabled == true
 * 3*. pendingActions.size() > 0 → governorCooldownActive == true
 * 4. executionHistory.size() >= lastSnapshotHistorySize
 */
class StateInvariantValidatorTest {

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

                assertTrue(result.isInvalid(), "SafeMode + AutoTuning should be invalid");
                ValidationResult.Invalid invalid = (ValidationResult.Invalid) result;
                assertTrue(invalid.violations().stream().anyMatch(v -> v.contains("Invariant 1")),
                                "Should report Invariant 1 violation");
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
                RuntimeState state = RuntimeState.defaults();
                state = new RuntimeState(
                                state.enabled(),
                                state.safeMode(),
                                state.autoTuning(),
                                state.debugLogs(),
                                false, // governorDisabled = false (VIOLATION)
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

                ValidationResult result = StateInvariantValidator.validate(state);

                assertTrue(result.isInvalid(), "Benchmark without Governor disabled should be invalid");
                ValidationResult.Invalid invalid = (ValidationResult.Invalid) result;
                assertTrue(invalid.violations().stream().anyMatch(v -> v.contains("Invariant 2")),
                                "Should report Invariant 2 violation");
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

                ValidationResult result = StateInvariantValidator.validate(state);
                assertTrue(result.isValid(), "Benchmark with Governor disabled should be valid");
        }

        // === Invariant 3: Pending Actions → Cooldown Active ===

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

                ValidationResult result = StateInvariantValidator.validate(state);

                assertTrue(result.isInvalid(), "Pending actions without cooldown should be invalid");
                ValidationResult.Invalid invalid = (ValidationResult.Invalid) result;
                assertTrue(invalid.violations().stream().anyMatch(v -> v.contains("Invariant 3")),
                                "Should report Invariant 3 violation");
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

                ValidationResult result = StateInvariantValidator.validate(state);
                assertTrue(result.isValid(), "Pending actions with cooldown should be valid");
        }

        // === Invariant 4: History Monotonic Growth ===

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

        // === Multiple Violations ===

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

                assertTrue(result.isInvalid(), "Multiple violations should be invalid");
                ValidationResult.Invalid invalid = (ValidationResult.Invalid) result;
                assertEquals(3, invalid.violations().size(), "Should report all 3 violations");
        }

        // === Default State Always Valid ===

        @Test
        void testDefaultState_IsAlwaysValid() {
                RuntimeState defaults = RuntimeState.defaults();
                ValidationResult result = StateInvariantValidator.validate(defaults);
                assertTrue(result.isValid(), "Default state must always be valid");
        }
}
