package dev.nozh.core.bus;

import dev.nozh.api.PerfSnapshot;
import dev.nozh.core.NozhLogger;
import dev.nozh.core.intelligence.SessionLearning;
import dev.nozh.core.matrix.ActionSuccessTracker;
import dev.nozh.core.safety.CrashFailureContext;
import dev.nozh.core.safety.CrashLoopGuard;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Standard implementation of ActionProcessor (Contract 2, Paso 2.4).
 * 
 * Bridge between ActionBus and CapabilityExecutor.
 * 
 * PROHIBITIONS (NON-NEGOTIABLE):
 * ❌ Does NOT validate commands (already done)
 * ❌ Does NOT decide policy (SafeMode, GovernorMode)
 * ❌ Does NOT write to executionHistory
 * ❌ Does NOT throw exceptions upward
 * ❌ Does NOT know ActionBus exists
 * 
 * OBLIGATIONS:
 * ✅ Executes one intention
 * ✅ Captures ALL exceptions
 * ✅ Produces complete CommandExecutionReport
 * ✅ Signals rollback (if applicable)
 * ✅ Never leaves partial state undeclared
 * 
 * Logger injection ensures Contract 2 purity (no Minecraft deps in core).
 */
public final class StandardActionProcessor implements ActionProcessor {

    private final CapabilityExecutor executor;
    private final NozhLogger logger;
    private final SessionLearning sessionLearning;
    private final Supplier<PerfSnapshot> perfSnapshotSupplier;
    private final ActionSuccessTracker successTracker;

    public StandardActionProcessor(CapabilityExecutor executor, NozhLogger logger) {
        this(executor, logger, null, PerfSnapshot::empty, null);
    }

    public StandardActionProcessor(
            CapabilityExecutor executor,
            NozhLogger logger,
            SessionLearning sessionLearning,
            Supplier<PerfSnapshot> perfSnapshotSupplier,
            ActionSuccessTracker successTracker) {
        this.executor = executor;
        this.logger = logger;
        this.sessionLearning = sessionLearning;
        this.perfSnapshotSupplier = perfSnapshotSupplier != null ? perfSnapshotSupplier : PerfSnapshot::empty;
        this.successTracker = successTracker;
    }

    public StandardActionProcessor(
            CapabilityExecutor executor,
            NozhLogger logger,
            SessionLearning sessionLearning,
            Supplier<PerfSnapshot> perfSnapshotSupplier) {
        this(executor, logger, sessionLearning, perfSnapshotSupplier, null);
    }

    @Override
    public void process(Command command, Consumer<CommandExecutionReport> callback) {
        long startedAt = System.currentTimeMillis();

        // Process command based on type
        if (command instanceof Command.ApplyCapability) {
            processApply((Command.ApplyCapability) command, startedAt, callback);
        } else if (command instanceof Command.ResetCapability) {
            processReset((Command.ResetCapability) command, startedAt, callback);
        } else if (command instanceof Command.PreviewCapability) {
            processPreview((Command.PreviewCapability) command, startedAt, callback);
        } else if (command instanceof Command.RunBenchmark) {
            processBenchmark((Command.RunBenchmark) command, startedAt, callback);
        } else {
            // Unknown command type (should never happen due to sealed interface)
            long finishedAt = System.currentTimeMillis();
            CommandExecutionReport report = new CommandExecutionReport(
                    command.id(),
                    command.type(),
                    null,
                    CommandLifecycle.FAILED,
                    0, // queuedAt filled by Bus
                    startedAt,
                    finishedAt,
                    Optional.of("Unknown command type: " + command.getClass().getName()),
                    Optional.empty());
            callback.accept(report);
        }
    }

    private void processApply(Command.ApplyCapability cmd, long startedAt, Consumer<CommandExecutionReport> callback) {
        CapabilityId capability = cmd.capability();
        CapabilityValue value = cmd.value();
        PerfSnapshot beforeSnapshot = perfSnapshotSupplier.get();
        if (successTracker != null) {
            successTracker.recordPreActionSnapshot(capability, beforeSnapshot);
        }

        if (CrashLoopGuard.isCapabilityQuarantined(capability)) {
            long finishedAt = System.currentTimeMillis();
            CommandExecutionReport report = new CommandExecutionReport(
                    cmd.id(),
                    cmd.type(),
                    capability,
                    CommandLifecycle.ABORTED,
                    0,
                    startedAt,
                    finishedAt,
                    Optional.of("Capability quarantined after crash loop recovery"),
                    Optional.empty());
            callback.accept(report);
            return;
        }

        // Store old value for potential rollback
        Optional<CapabilityValue> oldValue = Optional.empty();
        boolean supportsRollback = executor.supportsRollback(capability);

        if (supportsRollback) {
            oldValue = executor.getCurrentValue(capability);
        }

        // Execute
        CommandLifecycle finalState;
        Optional<String> error = Optional.empty();
        Optional<String> rollbackReason = Optional.empty();

        try {
            CapabilityExecutor.ExecutionResult result = executor.execute(capability, value);

            if (result.succeeded()) {
                finalState = CommandLifecycle.SUCCESS;
                if (logger.isDebugEnabled()) {
                    logger.debug("Command executed successfully: " + cmd.id());
                }
            } else {
                // Execution reported failure
                CapabilityExecutor.ExecutionResult.Failure failure = (CapabilityExecutor.ExecutionResult.Failure) result;
                error = Optional.of(failure.error());
                CrashLoopGuard.recordFailureContext(CrashFailureContext.forCommandFailure(
                        "ACTION_EXECUTION",
                        capability,
                        cmd.type(),
                        value,
                        failure.error(),
                        null));

                // Attempt rollback if supported
                if (supportsRollback && oldValue.isPresent()) {
                    boolean rolledBack = tryRollback(capability, oldValue.get());
                    if (rolledBack) {
                        finalState = CommandLifecycle.ROLLED_BACK;
                        rollbackReason = Optional.of("Execution failed, rolled back to previous value");
                    } else {
                        finalState = CommandLifecycle.FAILED;
                        rollbackReason = Optional.of("Execution failed, rollback also failed");
                    }
                } else {
                    finalState = CommandLifecycle.FAILED;
                }

                logger.warn("Command execution failed: " + cmd.id() + " - " + failure.error());
            }

        } catch (Exception e) {
            // Execution threw exception (NEVER propagate)
            error = Optional.of(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            CrashLoopGuard.recordFailureContext(CrashFailureContext.forCommandFailure(
                    "ACTION_EXECUTION",
                    capability,
                    cmd.type(),
                    value,
                    error.orElse("unknown"),
                    e));

            // Attempt rollback if supported
            if (supportsRollback && oldValue.isPresent()) {
                boolean rolledBack = tryRollback(capability, oldValue.get());
                if (rolledBack) {
                    finalState = CommandLifecycle.ROLLED_BACK;
                    rollbackReason = Optional.of("Exception during execution, rolled back to previous value");
                } else {
                    finalState = CommandLifecycle.FAILED;
                    rollbackReason = Optional.of("Exception during execution, rollback also failed");
                }
            } else {
                finalState = CommandLifecycle.FAILED;
            }

            logger.error("Command execution threw exception: " + cmd.id(), e);
        }

        // Close report
        long finishedAt = System.currentTimeMillis();
        CommandExecutionReport report = new CommandExecutionReport(
                cmd.id(),
                cmd.type(),
                capability,
                finalState,
                0, // queuedAt filled by Bus
                startedAt,
                finishedAt,
                error,
                rollbackReason);

        recordLearningOutcome(capability, finalState, beforeSnapshot, perfSnapshotSupplier.get());
        callback.accept(report);
    }

    private void processReset(Command.ResetCapability cmd, long startedAt, Consumer<CommandExecutionReport> callback) {
        // Reset not implemented yet (future)
        long finishedAt = System.currentTimeMillis();
        CommandExecutionReport report = new CommandExecutionReport(
                cmd.id(),
                cmd.type(),
                cmd.capability(),
                CommandLifecycle.FAILED,
                0,
                startedAt,
                finishedAt,
                Optional.of("Reset capability not implemented yet"),
                Optional.empty());
        callback.accept(report);
    }

    private void processPreview(Command.PreviewCapability cmd, long startedAt,
            Consumer<CommandExecutionReport> callback) {
        // Preview just reports success without applying (future: show diff in HUD)
        long finishedAt = System.currentTimeMillis();
        CommandExecutionReport report = new CommandExecutionReport(
                cmd.id(),
                cmd.type(),
                cmd.capability(),
                CommandLifecycle.SUCCESS,
                0,
                startedAt,
                finishedAt,
                Optional.empty(),
                Optional.empty());
        callback.accept(report);
    }

    private void processBenchmark(Command.RunBenchmark cmd, long startedAt, Consumer<CommandExecutionReport> callback) {
        // Benchmark not implemented yet (Phase 7)
        long finishedAt = System.currentTimeMillis();
        CommandExecutionReport report = new CommandExecutionReport(
                cmd.id(),
                cmd.type(),
                null,
                CommandLifecycle.FAILED,
                0,
                startedAt,
                finishedAt,
                Optional.of("Benchmark not implemented yet (Phase 7)"),
                Optional.empty());
        callback.accept(report);
    }

    private void recordLearningOutcome(
            CapabilityId capability,
            CommandLifecycle finalState,
            PerfSnapshot beforeSnapshot,
            PerfSnapshot afterSnapshot) {
        if (sessionLearning == null || capability == null) {
            return;
        }

        // ONLY record failures here (technical faults).
        // Success/Performance evaluation is done by GovernorRunner after a delay.
        if (finalState != CommandLifecycle.SUCCESS) {
            sessionLearning.recordFailure(capability);
        }
    }

    /**
     * Attempt rollback to previous value.
     * 
     * @return true if rollback succeeded, false otherwise
     */
    private boolean tryRollback(CapabilityId capability, CapabilityValue oldValue) {
        try {
            CapabilityExecutor.ExecutionResult result = executor.rollback(capability, oldValue);
            boolean success = result.succeeded();

            if (!success) {
                logger.error("Rollback failed for " + capability + ": " +
                        (result instanceof CapabilityExecutor.ExecutionResult.Failure
                                ? ((CapabilityExecutor.ExecutionResult.Failure) result).error()
                                : "unknown"));
            }

            return success;

        } catch (Exception e) {
            logger.error("Rollback threw exception for " + capability, e);
            return false;
        }
    }
}
