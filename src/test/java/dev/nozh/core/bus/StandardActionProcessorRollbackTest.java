package dev.nozh.core.bus;

import dev.nozh.core.NoOpLogger;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandardActionProcessorRollbackTest {

    @Test
    void reportsFailureWhenRollbackFailsAfterExecutionFailure() {
        RollbackFailingExecutor executor = new RollbackFailingExecutor(false, false);
        StandardActionProcessor processor = new StandardActionProcessor(executor, new NoOpLogger());
        Command cmd = new Command.ApplyCapability(
                CapabilityId.PARTICLES,
                new CapabilityValue.EnumValue("DECREASED"));

        AtomicReference<CommandExecutionReport> reportRef = new AtomicReference<>();
        processor.process(cmd, reportRef::set);

        CommandExecutionReport report = reportRef.get();
        assertEquals(CommandLifecycle.FAILED, report.finalState());
        assertTrue(report.error().isPresent());
        assertTrue(report.rollbackReason().isPresent());
        assertEquals("Execution failed, rollback also failed", report.rollbackReason().get());
    }

    @Test
    void reportsFailureWhenRollbackFailsAfterException() {
        RollbackFailingExecutor executor = new RollbackFailingExecutor(true, true);
        StandardActionProcessor processor = new StandardActionProcessor(executor, new NoOpLogger());
        Command cmd = new Command.ApplyCapability(
                CapabilityId.PARTICLES,
                new CapabilityValue.EnumValue("DECREASED"));

        AtomicReference<CommandExecutionReport> reportRef = new AtomicReference<>();
        processor.process(cmd, reportRef::set);

        CommandExecutionReport report = reportRef.get();
        assertEquals(CommandLifecycle.FAILED, report.finalState());
        assertTrue(report.error().isPresent());
        assertTrue(report.rollbackReason().isPresent());
        assertEquals("Exception during execution, rollback also failed", report.rollbackReason().get());
    }

    private static final class RollbackFailingExecutor implements CapabilityExecutor {
        private final boolean throwOnExecute;
        private final boolean throwOnRollback;

        private RollbackFailingExecutor(boolean throwOnExecute, boolean throwOnRollback) {
            this.throwOnExecute = throwOnExecute;
            this.throwOnRollback = throwOnRollback;
        }

        @Override
        public ExecutionResult execute(CapabilityId id, CapabilityValue value) throws Exception {
            if (throwOnExecute) {
                throw new RuntimeException("Simulated execution exception");
            }
            return new ExecutionResult.Failure("Simulated execution failure");
        }

        @Override
        public ExecutionResult rollback(CapabilityId id, CapabilityValue oldValue) throws Exception {
            if (throwOnRollback) {
                throw new RuntimeException("Simulated rollback exception");
            }
            return new ExecutionResult.Failure("Simulated rollback failure");
        }

        @Override
        public boolean supportsRollback(CapabilityId id) {
            return true;
        }

        @Override
        public Optional<CapabilityValue> getCurrentValue(CapabilityId id) {
            return Optional.of(new CapabilityValue.EnumValue("ALL"));
        }
    }
}
