package dev.nozh.core.profiler;

import java.util.concurrent.TimeUnit;

public final class DecisionLatencyEvaluator {

    private long lastDecisionLatencyMs = -1L;
    private long maxDecisionLatencyMs = 0L;
    private long totalDecisionLatencyMs = 0L;
    private long decisionCount = 0L;

    public long startTimer() {
        return System.nanoTime();
    }

    public long recordLatency(long startNanos) {
        long elapsedNanos = System.nanoTime() - startNanos;
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
        lastDecisionLatencyMs = elapsedMs;
        maxDecisionLatencyMs = Math.max(maxDecisionLatencyMs, elapsedMs);
        totalDecisionLatencyMs += elapsedMs;
        decisionCount++;
        return elapsedMs;
    }

    public boolean isWithinBudget(long startNanos, int budgetMs) {
        long elapsedMs = recordLatency(startNanos);
        return elapsedMs <= budgetMs;
    }

    public long getLastDecisionLatencyMs() {
        return lastDecisionLatencyMs;
    }

    public DecisionLatencyStats snapshot() {
        double avgMs = decisionCount > 0 ? (double) totalDecisionLatencyMs / decisionCount : 0.0;
        return new DecisionLatencyStats(decisionCount, avgMs, maxDecisionLatencyMs, lastDecisionLatencyMs);
    }
}
