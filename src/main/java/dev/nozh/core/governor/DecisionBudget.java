package dev.nozh.core.governor;

import java.util.concurrent.TimeUnit;

public final class DecisionBudget {

    private final long startNanos;
    private final long budgetNanos;

    public DecisionBudget(int budgetMs) {
        this.startNanos = System.nanoTime();
        this.budgetNanos = TimeUnit.MILLISECONDS.toNanos(Math.max(0, budgetMs));
    }

    public boolean isOverBudget() {
        if (budgetNanos <= 0) {
            return true;
        }
        return System.nanoTime() - startNanos > budgetNanos;
    }

    public long elapsedMs() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }
}
