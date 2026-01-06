package dev.nozh.core.profiler;

import java.util.concurrent.TimeUnit;

public final class DecisionLatencyEvaluator {

    private long lastDecisionLatencyMs = -1L;

    public long startTimer() {
        return System.nanoTime();
    }

    public boolean isWithinBudget(long startNanos, int budgetMs) {
        long elapsedNanos = System.nanoTime() - startNanos;
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
        lastDecisionLatencyMs = elapsedMs;
        return elapsedMs <= budgetMs;
    }

    public long getLastDecisionLatencyMs() {
        return lastDecisionLatencyMs;
    }
}
