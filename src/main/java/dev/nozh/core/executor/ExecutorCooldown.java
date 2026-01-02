package dev.nozh.core.executor;

import dev.nozh.core.config.NozhConfig;
import dev.nozh.core.safety.NozhState;

/**
 * Enforces rate limiting for actions.
 * Rules:
 * - Minimum interval between any actions.
 * - Global cooldown.
 * - Max changes per session.
 */
public class ExecutorCooldown {

    private long lastExecutionTime = 0;
    private long lastGlobalActionTime = 0; // Added field for global cooldown

    public boolean isCooldownActive(NozhConfig config) {
        long elapsed = System.currentTimeMillis() - lastExecutionTime;
        return elapsed < config.cooldownActionMillis;
    }

    public long getRemainingCooldown(NozhConfig config) {
        long elapsed = System.currentTimeMillis() - lastExecutionTime;
        return Math.max(0, config.cooldownActionMillis - elapsed);
    }

    public boolean isGlobalCooldownActive(NozhConfig config) { // Added config parameter
        long elapsed = System.currentTimeMillis() - lastGlobalActionTime;
        return elapsed < config.cooldownGlobalMinIntervalMillis;
    }

    public long getGlobalCooldownRemaining(NozhConfig config) { // Added config parameter
        long elapsed = System.currentTimeMillis() - lastGlobalActionTime;
        return Math.max(0, config.cooldownGlobalMinIntervalMillis - elapsed);
    }

    public void markExecution() {
        this.lastExecutionTime = System.currentTimeMillis();
        this.lastGlobalActionTime = System.currentTimeMillis(); // Mark global action time as well
    }

    public boolean maxChangesReached(NozhConfig config, NozhState state) {
        if (state.executionHistory.isEmpty())
            return false;

        long sessionStart = state.sessionStartTime;
        long changesInSession = state.executionHistory.stream()
                .filter(a -> a.timestamp() >= sessionStart)
                .count();

        return changesInSession >= config.maxChangesPerSession;
    }
}
