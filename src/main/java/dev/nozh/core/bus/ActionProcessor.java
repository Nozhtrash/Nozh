package dev.nozh.core.bus;

import java.util.function.Consumer;

/**
 * Bridge between ActionBus and ActionExecutor (Contract 2, Rule 2.2).
 * 
 * This is the ONLY component that knows both Bus and Executor exist.
 * 
 * Responsibilities:
 * - Take ValidatedCommand from Bus
 * - Invoke ActionExecutor
 * - Capture result
 * - Generate CommandExecutionReport
 * - Notify callbacks
 * - Trigger rollback if needed
 * 
 * The Bus does NOT know this exists.
 * The Processor does NOT decide, only executes.
 */
public interface ActionProcessor {

    /**
     * Process a validated command.
     * 
     * @param command  Validated command (lifecycle = VALIDATED or EXECUTING)
     * @param callback Callback to invoke with execution report
     */
    void process(Command command, Consumer<CommandExecutionReport> callback);
}
