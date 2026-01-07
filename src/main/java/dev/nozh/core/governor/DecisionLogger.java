package dev.nozh.core.governor;

import dev.nozh.NozhConstants;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Structured logger for governor decisions with searchable history.
 * 
 * Logs every decision with:
 * - Timestamp
 * - Reasoning
 * - Outcome
 * - Telemetry context
 * 
 * Keeps ring buffer of last 100 decisions for `/nozh explain`.
 * 
 * TASK 6: Explainable decisions - structured logging
 */
public final class DecisionLogger {

    private static final int HISTORY_SIZE = 100;
    private static final Deque<DecisionEntry> history = new ArrayDeque<>(HISTORY_SIZE);

    /**
     * Log a decision.
     */
    public static void logDecision(DecisionReasoning reasoning) {
        DecisionEntry entry = new DecisionEntry(
                System.currentTimeMillis(),
                reasoning
        );

        synchronized (history) {
            if (history.size() >= HISTORY_SIZE) {
                history.pollFirst();
            }
            history.offer(entry);
        }

        // Also log to Minecraft logger
        NozhConstants.LOGGER.info("[GOVERNOR] " + reasoning.toExplanation());
    }

    /**
     * Get recent decision history (last N entries).
     */
    public static List<DecisionEntry> getRecentHistory(int count) {
        List<DecisionEntry> result = new ArrayList<>();
        synchronized (history) {
            int toTake = Math.min(count, history.size());
            int skip = Math.max(0, history.size() - toTake);
            int i = 0;
            for (DecisionEntry entry : history) {
                if (i >= skip) {
                    result.add(entry);
                }
                i++;
            }
        }
        return result;
    }

    /**
     * Get most recent decision.
     */
    public static DecisionEntry getLatest() {
        synchronized (history) {
            return history.isEmpty() ? null : history.peekLast();
        }
    }

    /**
     * Get all history.
     */
    public static List<DecisionEntry> getAllHistory() {
        synchronized (history) {
            return new ArrayList<>(history);
        }
    }

    /**
     * Clear history.
     */
    public static void clearHistory() {
        synchronized (history) {
            history.clear();
        }
        NozhConstants.LOGGER.info("Decision history cleared");
    }

    /**
     * Decision entry record.
     */
    public record DecisionEntry(
            long timestamp,
            DecisionReasoning reasoning
    ) {
        public long getAgeMs() {
            return System.currentTimeMillis() - timestamp;
        }
    }
}
