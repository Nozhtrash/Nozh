package dev.nozh.core.profiler;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerfManagerLatencyBudgetTest {

    @Test
    void decisionLatencyBudgetStopsOverruns() {
        PerfManager manager = new PerfManager();

        long start = manager.startDecisionTimer();
        long forcedStart = start - TimeUnit.MILLISECONDS.toNanos(12);
        assertFalse(manager.isDecisionWithinBudget(forcedStart, 5));

        long freshStart = manager.startDecisionTimer();
        assertTrue(manager.isDecisionWithinBudget(freshStart, 50));
    }
}
