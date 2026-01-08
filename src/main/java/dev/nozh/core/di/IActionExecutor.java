package dev.nozh.core.di;

import dev.nozh.core.context.Scenario;
import dev.nozh.core.intelligence.DecisionReasoning;
import dev.nozh.core.learning.PerformanceLearningEngine;

/**
 * Interface for action execution - enables DI.
 */
public interface IActionExecutor {
    void executeAsync(
            String actionId,
            DecisionReasoning reasoning,
            Scenario scenario,
            PerformanceLearningEngine.GameState state,
            double fpsBefore
    );
    int getPendingCount();
    void shutdown();
}
