package dev.nozh.core.issues;

import dev.nozh.core.state.RuntimeState;
import dev.nozh.core.telemetry.TelemetrySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Telemetry starvation detector (Contract 9).
 * 
 * Detects when telemetry buffer is dropping too many samples.
 */
public final class TelemetryStarvationDetector implements IssueDetector {

    private static final int DROP_THRESHOLD_WARNING = 100;
    private static final int DROP_THRESHOLD_CRITICAL = 500;

    @Override
    public List<Issue> evaluate(RuntimeState state, TelemetrySnapshot telemetry) {
        List<Issue> issues = new ArrayList<>();
        long now = System.currentTimeMillis();

        int dropped = telemetry.droppedSamples();

        if (dropped >= DROP_THRESHOLD_CRITICAL) {
            issues.add(Issue.create(
                    IssueType.TELEMETRY_STARVATION,
                    IssueSeverity.CRITICAL,
                    "nozh.issue.telemetry.starvation.critical",
                    now));
        } else if (dropped >= DROP_THRESHOLD_WARNING) {
            issues.add(Issue.create(
                    IssueType.TELEMETRY_STARVATION,
                    IssueSeverity.WARNING,
                    "nozh.issue.telemetry.starvation.warning",
                    now));
        }

        return issues;
    }
}
