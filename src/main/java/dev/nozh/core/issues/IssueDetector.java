package dev.nozh.core.issues;

import dev.nozh.core.state.RuntimeState;
import dev.nozh.core.telemetry.TelemetrySnapshot;

import java.util.List;

/**
 * Issue detector interface (Contract 9).
 * 
 * PURE - evaluates state + telemetry, returns issues.
 * NO side effects, NO logging, NO MC dependencies.
 */
public interface IssueDetector {

    /**
     * Evaluate current state and detect issues.
     * 
     * @param state     Current runtime state
     * @param telemetry Current telemetry snapshot
     * @return List of detected issues (may be empty, never null)
     */
    List<Issue> evaluate(RuntimeState state, TelemetrySnapshot telemetry);
}
