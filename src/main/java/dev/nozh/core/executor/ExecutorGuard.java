package dev.nozh.core.executor;

import dev.nozh.api.governor.Decision;
import dev.nozh.api.governor.ActionType;
import dev.nozh.api.governor.DecisionSeverity;
import dev.nozh.api.governor.DecisionConfidence;
import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.config.NozhConfig;
import dev.nozh.core.safety.NozhState;

/**
 * THE BOUNCER.
 * Validates strictly if an action can be executed.
 */
public class ExecutorGuard {

    private final ExecutorCooldown cooldown;

    public ExecutorGuard(ExecutorCooldown cooldown) {
        this.cooldown = cooldown;
    }

    public ExecutionResult check(Decision decision, NozhConfig config, NozhState state) {
        long now = System.currentTimeMillis();

        // 1. Basic Validity
        if (decision.type() == ActionType.NONE) {
            return new ExecutionResult(ExecutionStatus.SKIPPED, "No action proposed", now);
        }

        // 2. Config Gates
        if (!config.enabled) {
            return new ExecutionResult(ExecutionStatus.BLOCKED, "Mod disabled", now);
        }
        if (!config.allowAutoTuning) {
            return new ExecutionResult(ExecutionStatus.BLOCKED, "Auto-tuning disabled", now);
        }

        // 3. Safe Mode
        if (state.isSafeModeActive()) {
            return new ExecutionResult(ExecutionStatus.BLOCKED,
                    "Safe Mode Active (" + state.getSafeModeReason() + ")", now);
        }

        // 3.1 Crash recovery quarantine (targeted capability disable)
        CapabilityId capabilityId = resolveCapabilityId(decision.type());
        if (capabilityId != null && state.isCapabilityQuarantined(capabilityId, now)) {
            long retryAt = state.getCapabilityRetryAt(capabilityId).orElse(0L);
            return new ExecutionResult(ExecutionStatus.BLOCKED,
                    "Capability quarantined until " + retryAt, now);
        }

        // 4. Confidence & Severity
        if (decision.confidence() == DecisionConfidence.LOW) {
            return new ExecutionResult(ExecutionStatus.BLOCKED, "Low confidence", now);
        }
        if (decision.severity() != DecisionSeverity.CRITICAL && decision.severity() != DecisionSeverity.WARN) {
            // Only act on WARN or CRITICAL (Phase 6 rule)
            return new ExecutionResult(ExecutionStatus.SKIPPED, "Severity too low (" + decision.severity() + ")", now);
        }

        // 5. Cooldowns
        if (cooldown.isCooldownActive(config)) {
            return new ExecutionResult(ExecutionStatus.BLOCKED,
                    "Cooldown active (" + cooldown.getRemainingCooldown(config) + "s)", now);
        }
        if (cooldown.maxChangesReached(config, state)) {
            return new ExecutionResult(ExecutionStatus.BLOCKED, "Max session changes reached", now);
        }

        // 6. Action-Specific Blocks (Optional for Phase 6)
        // e.g. check if target is already reached (Handler's job? Or Guard?)
        // Guard checks policy. Handler checks feasibility.

        return new ExecutionResult(ExecutionStatus.EXECUTED, "Allowed", now); // "EXECUTED" used here means "Authorized"
                                                                              // in context of guard
    }

    private CapabilityId resolveCapabilityId(ActionType actionType) {
        if (actionType == null) {
            return null;
        }
        return switch (actionType) {
            case DECREASE_PARTICLES -> CapabilityId.PARTICLES;
            case DECREASE_RENDER_DISTANCE -> CapabilityId.RENDER_DISTANCE;
            case DECREASE_SIMULATION_DISTANCE -> CapabilityId.SIMULATION_DISTANCE;
            case DECREASE_ENTITY_DISTANCE -> CapabilityId.ENTITY_DISTANCE;
            case DISABLE_CLOUDS -> CapabilityId.CLOUDS;
            case DISABLE_ENTITY_SHADOWS -> CapabilityId.ENTITY_SHADOWS;
            case DECREASE_BIOME_BLEND -> CapabilityId.BIOME_BLEND;
            default -> null;
        };
    }
}
