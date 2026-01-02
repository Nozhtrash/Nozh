package dev.nozh.core.state;

import java.util.ArrayList;
import java.util.List;

/**
 * Enforces StateStore invariants (Contract 1).
 * 
 * ALL state mutations MUST pass validation before being applied.
 * NO partial updates allowed.
 * 
 * Mandatory Invariants:
 * 1. safeMode == true → autoTuning == false
 * 2. benchmarkRunning == true → governorDisabled == true
 * 3. pendingActions.size() > 0 → governorCooldownActive == true
 * 4. executionHistory.size() >= lastSnapshotHistorySize (monotonic growth)
 */
public final class StateInvariantValidator {

    private StateInvariantValidator() {
        throw new AssertionError("No instances");
    }

    /**
     * Validate a RuntimeState snapshot.
     * 
     * @param state State to validate
     * @return ValidationResult (Valid or Invalid with violations)
     */
    public static ValidationResult validate(RuntimeState state) {
        List<String> violations = new ArrayList<>();

        // Invariant 1: SafeMode → AutoTuning OFF
        if (state.safeMode() && state.autoTuning()) {
            violations.add("Invariant 1: SafeMode is active, but AutoTuning is enabled (forbidden)");
        }

        // Invariant 2: Benchmark Running → Governor Disabled
        if (state.benchmarkRunning() && !state.governorDisabled()) {
            violations.add("Invariant 2: Benchmark is running, but Governor is not disabled (forbidden)");
        }

        // Invariant 3: Pending Actions → Cooldown Active
        if (state.pendingActionsCount() > 0 && !state.governorCooldownActive()) {
            violations.add("Invariant 3: Pending actions exist, but Governor cooldown is not active (forbidden)");
        }

        // Invariant 4: Execution History Monotonic Growth
        if (state.executionHistorySize() < state.lastSnapshotHistorySize()) {
            violations.add("Invariant 4: Execution history size decreased (monotonic growth violated)");
        }

        // Return result
        if (violations.isEmpty()) {
            return new ValidationResult.Valid();
        } else {
            return new ValidationResult.Invalid(violations);
        }
    }

    /**
     * Validate and throw if invalid (convenience method for critical paths).
     * 
     * @param state State to validate
     * @throws StateInvariantViolationException if validation fails
     */
    public static void validateOrThrow(RuntimeState state) {
        ValidationResult result = validate(state);
        if (result.isInvalid()) {
            ValidationResult.Invalid invalid = (ValidationResult.Invalid) result;
            throw new StateInvariantViolationException(invalid.formatViolations());
        }
    }
}
