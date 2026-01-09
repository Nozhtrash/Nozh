package dev.nozh.core.bus;
import dev.nozh.core.capability.CapabilityId;

import java.util.Optional;
import java.util.UUID;

/**
 * Immutable execution report for a command (Contract 2, Rule 2.5).
 * 
 * Requirements:
 * - Serializable to JSON
 * - Stored in executionHistory
 * - Exportable in /nozh selfcheck
 * - Displayable in HUD → History
 * 
 * Contract 2, Rule 2.5: Report is GOLD. Never mutable.
 * 
 * CRITICAL FIELD (Surgical Feedback): queuedAtMillis
 * Without this, cannot measure latency, audit starvation, or explain "why it
 * took so long".
 */
public record CommandExecutionReport(
        UUID commandId,
        CommandType type,
        CapabilityId capability,
        CommandLifecycle finalState,
        long queuedAtMillis, // CRITICAL: When command entered queue
        long startedAtMillis, // When execution began
        long finishedAtMillis, // When execution completed
        Optional<String> error,
        Optional<String> rollbackReason // Simplified for now, can evolve to RollbackReport
) {

    /**
     * Calculate queue latency (time from queued to started).
     */
    public long queueLatencyMillis() {
        return startedAtMillis - queuedAtMillis;
    }

    /**
     * Calculate execution duration (time from started to finished).
     */
    public long executionDurationMillis() {
        return finishedAtMillis - startedAtMillis;
    }

    /**
     * Calculate total latency (time from queued to finished).
     */
    public long totalLatencyMillis() {
        return finishedAtMillis - queuedAtMillis;
    }

    /**
     * Check if command succeeded.
     */
    public boolean succeeded() {
        return finalState == CommandLifecycle.SUCCESS;
    }

    /**
     * Check if command failed.
     */
    public boolean failed() {
        return finalState == CommandLifecycle.FAILED;
    }

    /**
     * Check if command was rolled back.
     */
    public boolean wasRolledBack() {
        return finalState == CommandLifecycle.ROLLED_BACK;
    }
}
