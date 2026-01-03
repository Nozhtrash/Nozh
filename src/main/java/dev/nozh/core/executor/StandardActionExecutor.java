package dev.nozh.core.executor;

import dev.nozh.NozhConstants;
import dev.nozh.api.governor.ActionType;
import dev.nozh.api.governor.Decision;
import dev.nozh.core.config.ConfigManager;
import dev.nozh.core.config.NozhConfig;
import dev.nozh.core.executor.handlers.DecreaseEntityDistanceHandler;
import dev.nozh.core.executor.handlers.DecreaseParticlesHandler;
import dev.nozh.core.executor.handlers.DecreaseRenderDistanceHandler;
import dev.nozh.core.executor.handlers.DecreaseSimulationDistanceHandler;
import dev.nozh.core.safety.NozhState;
import dev.nozh.core.safety.StateManager;
import net.minecraft.client.MinecraftClient;

import java.util.EnumMap;
import java.util.Map;

/**
 * Standard implementation of ActionExecutor.
 */
public class StandardActionExecutor implements ActionExecutor {

    private final ExecutorCooldown cooldown;
    private final ExecutorGuard guard;
    private final Map<ActionType, ActionHandler> handlers;

    public StandardActionExecutor() {
        this.cooldown = new ExecutorCooldown();
        this.guard = new ExecutorGuard(cooldown);
        this.handlers = new EnumMap<>(ActionType.class);

        // Register Phase 6 Handler
        handlers.put(ActionType.DECREASE_PARTICLES, new DecreaseParticlesHandler());
        handlers.put(ActionType.DECREASE_RENDER_DISTANCE, new DecreaseRenderDistanceHandler());
        handlers.put(ActionType.DECREASE_SIMULATION_DISTANCE, new DecreaseSimulationDistanceHandler());
        handlers.put(ActionType.DECREASE_ENTITY_DISTANCE, new DecreaseEntityDistanceHandler());
    }

    @Override
    public ExecutionResult execute(Decision decision) {
        NozhConfig config = ConfigManager.getConfig();
        NozhState state = StateManager.getState();
        long now = System.currentTimeMillis();

        // 1. Guard Check
        ExecutionResult guardResult = guard.check(decision, config, state);
        if (guardResult.status() != ExecutionStatus.EXECUTED) { // Valid "Authorized" status check
            // If skipped or blocked, just return guarding result
            if (guardResult.status() == ExecutionStatus.BLOCKED && config.debugLogs) {
                NozhConstants.LOGGER.info("[EXECUTOR] BLOCKED action={} reason={}", decision.type(),
                        guardResult.message());
            }
            return guardResult;
        }

        // 2. Locate Handler
        ActionHandler handler = handlers.get(decision.type());
        if (handler == null) {
            return new ExecutionResult(ExecutionStatus.SKIPPED, "No handler for " + decision.type(), now);
        }

        // 3. Execute
        MinecraftClient client = MinecraftClient.getInstance();
        boolean success = false;
        try {
            success = handler.execute(client);
        } catch (Exception e) {
            NozhConstants.LOGGER.error("[EXECUTOR] FAILED handling action {}: {}", decision.type(), e.getMessage());
            return new ExecutionResult(ExecutionStatus.FAILED, e.getMessage(), now);
        }

        if (success) {
            // 4. Success Logic
            cooldown.markExecution();

            // Persist History
            ExecutedAction actionRecord = new ExecutedAction(
                    now,
                    decision.type(),
                    handler.getOldValue(),
                    handler.getNewValue());
            state.executionHistory.add(actionRecord);
            StateManager.save(); // Atomic save

            // Log
            NozhConstants.LOGGER.info("[EXECUTOR] EXECUTED action={} reason={} confidence={} change={}",
                    decision.type(),
                    decision.reasonCode(),
                    decision.confidence(),
                    handler.getLastChangeDetails());

            return new ExecutionResult(ExecutionStatus.EXECUTED, "Success", now);
        } else {
            return new ExecutionResult(ExecutionStatus.SKIPPED, "Handler returned no change (already optimal?)", now);
        }
    }

    @Override
    public void revertLast(MinecraftClient client) {
        NozhState state = StateManager.getState();

        // CRITICAL: Safe Mode Block
        if (state.isSafeModeActive()) {
            NozhConstants.LOGGER.warn("[EXECUTOR] Revert BLOCKED: Safe Mode Active");
            return;
        }

        if (state.executionHistory.isEmpty())
            return;

        // Get last action
        dev.nozh.core.executor.ExecutedAction last = state.executionHistory.get(state.executionHistory.size() - 1);

        // Find handler
        ActionHandler handler = handlers.get(last.type());
        if (handler != null) {
            // Revert
            boolean success = handler.apply(client, last.oldValue());

            if (success) {
                NozhConstants.LOGGER.info("[EXECUTOR] ROLLBACK action={} reason=NO_IMPROVEMENT restored={}",
                        last.type(), last.oldValue());

                // Remove from history (it's gone)
                state.executionHistory.remove(state.executionHistory.size() - 1);
                StateManager.save();

                // Trigger cooldown to prevent ping-pong
                cooldown.markExecution();
            } else {
                NozhConstants.LOGGER.warn("[EXECUTOR] FAILED to rollback action {}", last.type());
            }
        }
    }
}
