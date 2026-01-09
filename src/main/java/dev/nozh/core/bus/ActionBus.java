/**
 * NOZH - Adaptive Performance Optimization
 * Copyright (c) 2025 NOZH Project
 * 
 * Licensed under the MIT License.
 * 
 * This file defines a CORE ARCHITECTURAL CONTRACT.
 * Changes here affect system-wide invariants.
 * 
 * Read docs/v0.2-alpha.md before modifying.
 */
package dev.nozh.core.bus;

import dev.nozh.core.capability.CapabilityId;
import dev.nozh.core.NozhLogger;
import dev.nozh.core.state.RuntimeState;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * ActionBus: Central command orchestrator (Contract 2).
 * 
 * WHY THIS EXISTS:
 * ActionBus enforces DETERMINISTIC command execution: exactly 1 command per
 * tick,
 * in FIFO order. This prevents "cascades" where multiple commands fire in one
 * tick,
 * making debugging impossible and causing flapping (rapid on/off changes).
 * 
 * WHY 1 COMMAND PER TICK:
 * Each command changes game state (particles, clouds, FPS cap). Processing
 * multiple
 * commands in one tick means you can't observe the effect of each change
 * individually.
 * This makes it impossible to measure if a change helped or hurt, breaking the
 * governor's
 * confidence scoring system.
 * 
 * RULES (NON-NEGOTIABLE):
 * - Rule 2.1: Single-thread ownership, processes exactly 1 command per tick
 * - Rule 2.2: Bus does NOT know ActionExecutor exists (bridge pattern)
 * - Rule C2.X: Bus does NOT know "why" commands are dispatched
 * - Rule C2.Y: Bus does NOT modify StateStore
 * - Rule C2.Z: Fully testable without Minecraft
 * 
 * Architecture:
 * ActionBus → ValidatedCommand → ActionProcessor (bridge) → ActionExecutor
 * 
 * WHY PASSIVE DISPATCH:
 * dispatch() only enqueues and validates. It does NOT execute. This allows
 * governor logic to remain pure (decision-making) while execution happens
 * separately on the MC main thread (side effects). Keeps /core pure.
 */
public final class ActionBus {

    private static final int MAX_QUEUE_SIZE = 100; // Prevent memory leak

    private final Queue<QueuedCommand> commandQueue = new LinkedList<>();
    private Optional<QueuedCommand> currentlyExecuting = Optional.empty();
    private final NozhLogger logger;
    private final Supplier<RuntimeState> stateProvider;

    /**
     * Constructor with dependency injection.
     * 
     * @param logger        Logger abstraction (NoOpLogger for tests, FabricLogger
     *                      for production)
     * @param stateProvider State provider (fake for tests, StateStore snapshot for
     *                      production)
     */
    public ActionBus(NozhLogger logger, Supplier<RuntimeState> stateProvider) {
        this.logger = logger;
        this.stateProvider = stateProvider;
    }

    /**
     * Dispatch a command (thread-safe enqueue).
     * 
     * Contract 2, Rule 2.1: dispatch() may be called from any thread.
     * Surgical Feedback: dispatch() is PASSIVE.
     * 
     * dispatch() ONLY:
     * - Validates command
     * - Enqueues if valid
     * - Generates rejection report if invalid
     * - Records queuedAtMillis
     * 
     * dispatch() NEVER:
     * - Validates effects
     * - Touches execution times
     * - Mutates global state
     * - Inspects history
     * 
     * @param command  Command to execute
     * @param callback Result callback (may be called from tick thread)
     */
    public void dispatch(Command command, Consumer<CommandExecutionReport> callback) {
        dispatch(command, callback, false);
    }

    /**
     * Dispatch a user-confirmed command (bypasses auto-tuning gating).
     */
    public void dispatchUserCommand(Command command, Consumer<CommandExecutionReport> callback) {
        dispatch(command, callback, true);
    }

    private void dispatch(Command command, Consumer<CommandExecutionReport> callback, boolean userInitiated) {
        long queuedAt = System.currentTimeMillis();

        synchronized (commandQueue) {
            // Validate against current state (injected provider, not StateStore singleton)
            RuntimeState state = stateProvider.get();
            if (!userInitiated && !state.autoTuning() && isAutoCapabilityCommand(command)) {
                CommandExecutionReport report = createRejectedReport(
                        command,
                        queuedAt,
                        "Auto-tuning disabled: automatic actions blocked");
                callback.accept(report);
                if (logger.isDebugEnabled()) {
                    logger.debug("Command rejected (auto-tuning disabled): " + command.id());
                }
                return;
            }
            ValidationResult validation = CommandValidator.validate(command, state);

            if (validation.isInvalid()) {
                // Reject invalid command immediately
                ValidationResult.Invalid invalid = (ValidationResult.Invalid) validation;
                CommandExecutionReport report = createRejectedReport(command, queuedAt, invalid.reason());
                callback.accept(report);

                if (logger.isDebugEnabled()) {
                    logger.debug("Command rejected: " + command.id() + " - " + invalid.reason());
                }
                return;
            }

            // Check queue capacity
            if (commandQueue.size() >= MAX_QUEUE_SIZE) {
                CommandExecutionReport report = createRejectedReport(command, queuedAt, "Command queue full");
                callback.accept(report);
                logger.warn("Command queue full, rejecting: " + command.id());
                return;
            }

            // Enqueue
            QueuedCommand queued = new QueuedCommand(
                    command,
                    callback,
                    CommandLifecycle.QUEUED,
                    queuedAt // CRITICAL: Record queue timestamp
            );
            commandQueue.offer(queued);

            if (logger.isDebugEnabled()) {
                logger.debug("Command queued: " + command.id() + " (queue size: " + commandQueue.size() + ")");
            }
        }
    }

    /**
     * Process queue (MUST be called from main tick loop only).
     * 
     * Contract 2, Rule 2.1: Processes EXACTLY 1 command per tick.
     * Surgical Feedback: NO while, NO for, NO "if there's time".
     * 
     * ONE action per tick. Always.
     * 
     * @param processor Bridge to ActionExecutor
     */
    public void tick(ActionProcessor processor) {
        // If currently executing, check if finished
        if (currentlyExecuting.isPresent()) {
            // Processor is responsible for async tracking
            // For now, assume synchronous execution (completed in previous tick)
            currentlyExecuting = Optional.empty();
        }

        // Take next command from queue (EXACTLY 1)
        QueuedCommand next;
        synchronized (commandQueue) {
            next = commandQueue.poll();
        }

        if (next == null) {
            return; // Queue empty, no-op
        }

        // Mark as VALIDATED (separate from EXECUTING)
        // Surgical Feedback: VALIDATED ≠ EXECUTING
        next = next.withLifecycle(CommandLifecycle.VALIDATED);

        if (logger.isDebugEnabled()) {
            logger.debug("Processing command: " + next.command.id());
        }

        // Mark as EXECUTING (distinct state)
        next = next.withLifecycle(CommandLifecycle.EXECUTING);
        currentlyExecuting = Optional.of(next);

        // Hand off to processor (bridge)
        // Processor will call callback with result
        final QueuedCommand finalNext = next; // For lambda capture
        processor.process(next.command, report -> {
            // Enrich report with queue timestamp if missing
            CommandExecutionReport enriched = new CommandExecutionReport(
                    report.commandId(),
                    report.type(),
                    report.capability(),
                    report.finalState(),
                    finalNext.queuedAtMillis, // From queue record
                    report.startedAtMillis(),
                    report.finishedAtMillis(),
                    report.error(),
                    report.rollbackReason());
            finalNext.callback.accept(enriched);
        });
    }

    /**
     * Get current queue size (for debugging/monitoring).
     */
    public int getQueueSize() {
        synchronized (commandQueue) {
            return commandQueue.size();
        }
    }

    /**
     * Check if a command is currently executing.
     */
    public boolean isExecuting() {
        return currentlyExecuting.isPresent();
    }

    // Helper: Create rejection report
    private CommandExecutionReport createRejectedReport(Command command, long queuedAt, String reason) {
        long now = System.currentTimeMillis();

        CapabilityId cap = extractCapabilityId(command);

        return new CommandExecutionReport(
                command.id(),
                command.type(),
                cap,
                CommandLifecycle.ABORTED,
                queuedAt, // Queued timestamp
                now, // Started = finished (immediate rejection)
                now,
                Optional.of(reason),
                Optional.empty());
    }

    private CapabilityId extractCapabilityId(Command command) {
        if (command instanceof Command.ApplyCapability) {
            return ((Command.ApplyCapability) command).capability();
        } else if (command instanceof Command.ResetCapability) {
            return ((Command.ResetCapability) command).capability();
        } else if (command instanceof Command.PreviewCapability) {
            return ((Command.PreviewCapability) command).capability();
        }
        return null; // Benchmark has no capability
    }

    private boolean isAutoCapabilityCommand(Command command) {
        return command instanceof Command.ApplyCapability
                || command instanceof Command.ResetCapability
                || command instanceof Command.PreviewCapability;
    }

    /**
     * Internal queued command wrapper.
     */
    private record QueuedCommand(
            Command command,
            Consumer<CommandExecutionReport> callback,
            CommandLifecycle lifecycle,
            long queuedAtMillis // CRITICAL: Track queue timestamp
    ) {
        QueuedCommand withLifecycle(CommandLifecycle newLifecycle) {
            return new QueuedCommand(command, callback, newLifecycle, queuedAtMillis);
        }
    }
}
